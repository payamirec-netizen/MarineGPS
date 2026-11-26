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
 val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->
 if(uri!=null)scope.launch{busy=true;try{withContext(Dispatchers.IO){c.contentResolver.openOutputStream(uri)?.use{it.write(output.toByteArray(Charsets.UTF_8))}?:error("Cannot open file")};message="Export saved"}catch(e:Exception){message="Export failed: "+e.message}finally{busy=false}}
 }
 val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
 if(uri!=null)scope.launch{busy=true;try{pending=withContext(Dispatchers.IO){val raw=c.contentResolver.openInputStream(uri)?.use{it.readBytesLimited()}?:error("Cannot read file");require(raw.size<=8_000_000);parseGpx(raw.toString(Charsets.UTF_8))};message="Review import before saving"}catch(e:Exception){message="Import rejected: "+e.message}finally{busy=false}}
 }
 fun export(ext:String){scope.launch{busy=true;try{
 val data=withContext(Dispatchers.IO){TransferData(wd.points().observe().first().map{TransferPoint(it.name,it.latitude,it.longitude,it.notes)},td.tracks().observe().first().filter{it.ended!=null}.map{t->TransferTrack("Track "+t.started,td.tracks().points(t.id).first().groupBy{it.segment}.values.map{s->s.map{TransferPoint("",it.lat,it.lon)}})})}
 output=withContext(Dispatchers.Default){when(ext){"gpx"->gpx(data);"kml"->kml(data);else->csv(data)}}
 save.launch("MarineGPS."+ext)
 }catch(e:Exception){message="Export failed: "+e.message}finally{busy=false}}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 TextButton(onClick=back){Text("Back to settings")}
 Text("Data management",style=MaterialTheme.typography.headlineSmall)
 Text("Offline • Existing data is retained")
 for(ext in listOf("gpx","kml","csv"))OutlinedButton(enabled=!busy,onClick={export(ext)},modifier=Modifier.fillMaxWidth()){Text("Export "+ext.uppercase())}
 Button(enabled=!busy,onClick={open.launch(arrayOf("*/*"))}){Text("Import GPX")}
 Text(message)
 pending?.let{d->AlertDialog(onDismissRequest={pending=null},title={Text("Import preview")},text={Text("${d.points.size} waypoints • ${d.tracks.size} tracks. Existing data will not be deleted.")},dismissButton={TextButton(onClick={pending=null}){Text("Cancel")}},confirmButton={TextButton(onClick={pending=null;scope.launch{busy=true;try{
 wd.withTransaction{val existing=wd.points().observe().first().map{Triple(it.name,it.latitude,it.longitude)}.toMutableSet();d.points.forEach{p->if(existing.add(Triple(p.name,p.lat,p.lon)))wd.points().insert(Waypoint(name=p.name.ifBlank{"Imported waypoint"},latitude=p.lat,longitude=p.lon,category="Other",notes=p.notes,createdAt=System.currentTimeMillis()))}}
 td.withTransaction{d.tracks.forEach{t->val now=System.currentTimeMillis();val meters=t.segments.sumOf{s->s.zipWithNext().sumOf{(a,b)->targetMetrics(a.lat,a.lon,b.lat,b.lon).meters}};val id=td.tracks().insert(Track(started=now,ended=now,meters=meters,state="saved"));t.segments.forEachIndexed{i,s->s.forEach{p->td.tracks().point(TrackPoint(trackId=id,time=now,lat=p.lat,lon=p.lon,accuracy=0f,segment=i.toLong()))}}}}
 message="Import completed"
 }catch(e:Exception){message="Import interrupted; some data may have been saved: "+e.message}finally{busy=false}}}){Text("Import")}})}
 }
}

private fun java.io.InputStream.readBytesLimited():ByteArray {val out=java.io.ByteArrayOutputStream();val b=ByteArray(8192);while(true){val n=read(b);if(n<0)break;require(out.size()+n<=8_000_000);out.write(b,0,n)};return out.toByteArray()}
