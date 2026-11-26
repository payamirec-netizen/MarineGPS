package com.marinegps.app
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

@Composable fun DataScreen(back:()->Unit){
 val c=LocalContext.current
 val scope=rememberCoroutineScope()
 val wd=remember{WaypointDb.get(c)}
 val td=remember{TrackDb.get(c)}
 var message by remember{mutableStateOf("")}
 var busy by remember{mutableStateOf(false)}
 var pending by remember{mutableStateOf<TransferData?>(null)}
 var output by remember{mutableStateOf("")}
 var qr by remember{mutableStateOf<TransferPoint?>(null)}
 val prefs=c.getSharedPreferences("marine_settings",0)
 val waypoints by remember(wd){wd.points().observe()}.collectAsState(initial=emptyList())
 val tracks by remember(td){td.tracks().observe()}.collectAsState(initial=emptyList())
 var edit by remember{mutableStateOf<Track?>(null)}
 var delete by remember{mutableStateOf<Track?>(null)}
 var name by remember{mutableStateOf("")}
 val scan=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{try{val p=withContext(Dispatchers.IO){readQr(c,uri)};pending=TransferData(listOf(p),emptyList());message="Review QR waypoint"}catch(e:Exception){message="QR rejected: "+e.message}}}
 val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->
 if(uri!=null)scope.launch{busy=true;try{withContext(Dispatchers.IO){c.contentResolver.openOutputStream(uri)?.use{it.write(output.toByteArray(Charsets.UTF_8))}?:error("Cannot open file")};message="Export saved"}catch(e:Exception){message="Export failed: "+e.message}finally{busy=false}}
 }
 val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
 if(uri!=null)scope.launch{busy=true;try{pending=withContext(Dispatchers.IO){val raw=c.contentResolver.openInputStream(uri)?.use{it.readBytesLimited()}?:error("Cannot read file");require(raw.size<=8_000_000);raw.toString(Charsets.UTF_8).let{if(it.trimStart().startsWith("{"))restore(it) else parseGpx(it)}};message="Review import before saving"}catch(e:Exception){message="Import rejected: "+e.message}finally{busy=false}}
 }
 fun export(ext:String,share:Boolean=false){scope.launch{busy=true;try{
 val data=withContext(Dispatchers.IO){TransferData(wd.points().observe().first().map{TransferPoint(it.name,it.latitude,it.longitude,it.notes,it.createdAt,it.category,it.accuracy)},td.tracks().observe().first().filter{it.ended!=null}.map{t->TransferTrack(t.name.ifBlank{"Track "+t.started},td.tracks().points(t.id).first().groupBy{it.segment}.values.map{s->s.map{TransferPoint("",it.lat,it.lon,time=it.time,accuracy=it.accuracy)}},t.started,t.ended,t.activeMillis,t.meters,t.maxSpeed)},prefs.all.filterValues{it is String}.mapValues{it.value.toString()})}
 output=withContext(Dispatchers.Default){when(ext){"gpx"->gpx(data);"kml"->kml(data);"json"->backup(data);else->csv(data)}}
 if(share)shareFile(c,ext,output) else save.launch("MarineGPS."+ext)
 }catch(e:Exception){message="Export failed: "+e.message}finally{busy=false}}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 TextButton(onClick=back){Text("Back to settings")}
 Text("Data management",style=MaterialTheme.typography.headlineSmall)
 Text("Offline • Existing data is retained")
 for(ext in listOf("gpx","kml","csv","json"))OutlinedButton(enabled=!busy,onClick={export(ext)},modifier=Modifier.fillMaxWidth()){Text("Export "+ext.uppercase())}
 Button(enabled=!busy,onClick={open.launch(arrayOf("*/*"))}){Text("Import GPX / restore JSON")}
 Text(message)
 OutlinedButton(enabled=!busy,onClick={export("gpx",true)}){Text("Share GPX")}
 OutlinedButton(enabled=!busy,onClick={export("json",true)}){Text("Share backup")}
 OutlinedButton(enabled=!busy,onClick={scan.launch("image/*")}){Text("Read QR from image")}
 Text("Share waypoints")
 waypoints.forEach{w->Row{Text(w.name,Modifier.weight(1f));TextButton(onClick={qr=TransferPoint(w.name,w.latitude,w.longitude,w.notes)}){Text("QR")};TextButton(onClick={val clip=c.getSystemService(android.content.ClipboardManager::class.java);clip.setPrimaryClip(android.content.ClipData.newPlainText("Coordinates","${w.latitude}, ${w.longitude}"))}){Text("Copy")}}}
 qr?.let{p->AlertDialog(onDismissRequest={qr=null},title={Text(p.name)},text={QrImage(p)},confirmButton={TextButton(onClick={qr=null}){Text("Close")}})}
 Text("Manage saved tracks")
 tracks.filter{it.ended!=null}.forEach{t->Row{Text(t.name.ifBlank{"Track "+t.started},Modifier.weight(1f));TextButton(onClick={edit=t;name=t.name}){Text("Rename")};TextButton(onClick={delete=t}){Text("Delete")}}}
 edit?.let{t->AlertDialog(onDismissRequest={edit=null},title={Text("Rename track")},text={OutlinedTextField(name,{name=it.take(200)})},confirmButton={TextButton(onClick={edit=null;scope.launch{td.tracks().update(t.copy(name=name.trim()))}}){Text("Save")}})}
 delete?.let{t->AlertDialog(onDismissRequest={delete=null},title={Text("Delete saved track?")},confirmButton={TextButton(onClick={delete=null;scope.launch{td.tracks().delete(t)}}){Text("Delete")}},dismissButton={TextButton(onClick={delete=null}){Text("Cancel")}})}
 pending?.let{d->AlertDialog(onDismissRequest={pending=null},title={Text("Import preview")},text={Text("${d.points.size} waypoints • ${d.tracks.size} tracks. Existing data will not be deleted.")},dismissButton={TextButton(onClick={pending=null}){Text("Cancel")}},confirmButton={TextButton(onClick={pending=null;scope.launch{busy=true;try{
 require(td.tracks().active()==null){"Stop recording before import"}
 wd.withTransaction{val existing=wd.points().observe().first().map{Triple(it.name,it.latitude,it.longitude)}.toMutableSet();d.points.forEach{p->if(existing.add(Triple(p.name.ifBlank{"Imported waypoint"},p.lat,p.lon)))wd.points().insert(Waypoint(name=p.name.ifBlank{"Imported waypoint"},latitude=p.lat,longitude=p.lon,category=p.category,notes=p.notes,createdAt=p.time?:System.currentTimeMillis(),accuracy=p.accuracy))}}
 td.withTransaction{val existing=td.tracks().observe().first().filter{it.ended!=null}.map{t->trackKey(td.tracks().points(t.id).first().groupBy{it.segment}.values.map{s->s.map{TransferPoint("",it.lat,it.lon)}})}.toMutableSet();d.tracks.forEach{t->if(t.segments.flatten().isEmpty()||!existing.add(trackKey(t.segments)))return@forEach;val now=System.currentTimeMillis();val meters=t.segments.sumOf{s->s.zipWithNext().sumOf{(a,b)->targetMetrics(a.lat,a.lon,b.lat,b.lon).meters}};val id=td.tracks().insert(Track(started=t.started?:t.segments.flatten().mapNotNull{it.time}.minOrNull()?:now,ended=t.ended?:t.segments.flatten().mapNotNull{it.time}.maxOrNull()?:now,meters=t.meters?:meters,state="saved",name=t.name,activeMillis=t.activeMillis,maxSpeed=t.maxSpeed));t.segments.forEachIndexed{i,s->s.forEach{p->td.tracks().point(TrackPoint(trackId=id,time=p.time?:now,lat=p.lat,lon=p.lon,accuracy=p.accuracy?:0f,segment=i.toLong()))}}}}
 prefs.edit().apply{d.settings.forEach{(k,v)->putString(k,v)}}.apply();message="Import completed; reopen app to apply restored settings"
 }catch(e:Exception){message="Import interrupted; some data may have been saved: "+e.message}finally{busy=false}}}){Text("Import")}})}
 }
}

private fun java.io.InputStream.readBytesLimited():ByteArray {val out=java.io.ByteArrayOutputStream();val b=ByteArray(8192);while(true){val n=read(b);if(n<0)break;require(out.size()+n<=8_000_000);out.write(b,0,n)};return out.toByteArray()}
