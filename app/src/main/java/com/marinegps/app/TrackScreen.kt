package com.marinegps.app
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.text.DateFormat
import java.util.Date
import kotlin.math.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment

@Composable fun TrackScreen(request:()->Unit,unit:String,speedUnit:String){
 val context=LocalContext.current
 val dao=remember{TrackDb.get(context).tracks()}
 val tracks by remember{dao.observe()}.collectAsState(initial=emptyList())
 val health by TrackHealth.state.collectAsState()
 var clock by remember{mutableLongStateOf(android.os.SystemClock.elapsedRealtime())}
 LaunchedEffect(Unit){while(true){clock=android.os.SystemClock.elapsedRealtime();kotlinx.coroutines.delay(1000)}}
 val active=tracks.firstOrNull{it.ended==null}
 val scope=rememberCoroutineScope()
 var selection by remember{mutableStateOf(setOf<Long>())}
 var cancelRecording by remember{mutableStateOf(false)}
 var bulkDelete by remember{mutableStateOf(false)}
 var rename by remember{mutableStateOf<Track?>(null)}
 var renameValue by remember{mutableStateOf("")}
 var selected by remember{mutableStateOf<Long?>(null)}
 var error by remember{mutableStateOf("")}
 val detail=tracks.firstOrNull{it.id==(selected?:active?.id)}
 fun command(action:String){
 if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){request();return}
 if(action in listOf("start","resume","reconnect")&&!context.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)){error="Enable GPS first";return}
 try{context.startForegroundService(Intent(context,TrackService::class.java).setAction(action));error=""}catch(e:Exception){error="Unable to start recording: "+e.localizedMessage}
 }
 Column(Modifier.fillMaxSize()){
 Text(tr("Track recording"),style=MaterialTheme.typography.titleLarge);Text(tr(active?.state?:"READY • Offline"))
 Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
 if(active==null)Button(onClick={selected=null;command("start")}){Text(tr("Record"))}
 else{
 Button(onClick={command(if(active.state=="paused")"resume" else "pause")}){Text(tr(if(active.state=="paused")"Resume" else "Pause"))}
 OutlinedButton(onClick={command("stop")}){Text(tr("Stop & save"))}
 TextButton(onClick={cancelRecording=true}){Text(if(LocalArabic.current)"إلغاء التسجيل" else "Discard",color=MaterialTheme.colorScheme.error)}
 }
 };if(error.isNotEmpty())Text(tr(error),color=MaterialTheme.colorScheme.error)
 var diagnostics by remember{mutableStateOf(false)}
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){
 if(active?.state=="recording")item{Text(tr(if(!health.running)"Recording service inactive — reconnect" else if(health.lastFix==0L||clock-health.lastFix>10000L)"Waiting for usable GPS" else "GPS connected"));TextButton(onClick={diagnostics=!diagnostics}){Text(tr(if(diagnostics)"Hide GPS diagnostics" else "GPS diagnostics"))};if(diagnostics){Text(tr("Last fix: ")+if(health.lastFix==0L)"—" else ((clock-health.lastFix).coerceAtLeast(0)/1000).toString()+" s ago");Text(tr("GPS accuracy: ")+(health.accuracy?.let{"± "+fmtPoint(it.toDouble(),1)+" m"}?:"—"));Text(tr("Session gaps: ")+health.gaps+" • "+((health.gapMillis+if(health.waitingSince>0)(clock-health.waitingSince).coerceAtLeast(0) else 0)/1000)+" s");Text(tr("Background service: ")+tr(if(health.running)"Active" else "Inactive"));val power=context.getSystemService(android.os.PowerManager::class.java);Text(tr("Power saving: ")+tr(if(power.isPowerSaveMode)"On" else "Off"));Text(tr("Battery optimization: ")+tr(if(power.isIgnoringBatteryOptimizations(context.packageName))"Exempt" else "Enabled"));TextButton(onClick={runCatching{context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+context.packageName)))}}){Text(tr("App battery settings"))};var recovery by remember{mutableStateOf(false)};TextButton(onClick={recovery=!recovery}){Text(tr("Recording help"))};if(recovery||!health.running){Text(tr("If recording stops updating, reconnect GPS."));OutlinedButton(onClick={command("reconnect")}){Text(tr("Reconnect GPS"))}}}}
 if(detail!=null)item{TrackDetail(detail,dao,unit,speedUnit)}
 item{Text(tr("Saved tracks"),style=MaterialTheme.typography.titleMedium)}
 item{Row{TextButton(onClick={selection=tracks.filter{it.ended!=null}.map{it.id}.toSet()}){Text(tr("Select all"))};TextButton(onClick={selection=emptySet()}){Text(tr("Clear selection"))}}}
 if(selection.isNotEmpty())item{var data by remember(selection){mutableStateOf<TransferData?>(null)};LaunchedEffect(selection){data=TransferData(emptyList(),tracks.filter{it.id in selection&&it.ended!=null}.map{t->TransferTrack(t.name,dao.points(t.id).first().groupBy{it.segment}.values.map{s->s.map{TransferPoint("",it.lat,it.lon,time=it.time,accuracy=it.accuracy)}},t.started,t.ended,t.activeMillis,t.meters,t.maxSpeed)})};data?.let{SelectionActions(it,{bulkDelete=true})}}
 items(tracks.filter{it.ended!=null},key={it.id}){t->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(t.id in selection,{checked->selection=if(checked)selection+t.id else selection-t.id});Column(Modifier.weight(1f)){Text(t.name.ifBlank{tr("Track")},style=MaterialTheme.typography.titleMedium);Text(localDate(t.started),style=MaterialTheme.typography.bodySmall);Text(ltrValue(distanceLabel(t.meters,unit)+" • "+routeDuration(t.activeMillis)));Row{TextButton(onClick={selected=t.id}){Text(tr("Tracks"))};if(selection.isEmpty())TextButton(onClick={rename=t;renameValue=t.name}){Text(tr("Rename"))}}}}}}
 item{Text(tr("Track sketch only — not a nautical chart. Keep independent navigation equipment."))}
 }
 }
 if(cancelRecording)AlertDialog(onDismissRequest={cancelRecording=false},title={Text(if(LocalArabic.current)"إلغاء التسجيل دون حفظ؟" else "Discard recording?")},text={Text(if(LocalArabic.current)"سيحذف التسجيل الحالي فقط." else "Only the current recording will be deleted.")},confirmButton={TextButton(onClick={cancelRecording=false;selected=null;command("cancel")}){Text(tr("Delete"))}},dismissButton={TextButton(onClick={cancelRecording=false}){Text(tr("Cancel"))}})
 rename?.let{t->AlertDialog(onDismissRequest={rename=null},title={Text(tr("Rename track"))},text={OutlinedTextField(renameValue,{renameValue=it.take(200)})},confirmButton={TextButton(onClick={rename=null;scope.launch{dao.update(t.copy(name=renameValue.trim()))}}){Text(tr("Save"))}},dismissButton={TextButton(onClick={rename=null}){Text(tr("Cancel"))}})}
 if(bulkDelete)AlertDialog(onDismissRequest={bulkDelete=false},title={Text(tr("Confirm deletion"))},confirmButton={TextButton(onClick={bulkDelete=false;scope.launch{TrackDb.get(context).withTransaction{tracks.filter{it.id in selection&&it.ended!=null}.forEach{dao.delete(it)}};selection=emptySet()}}){Text(tr("Delete"))}},dismissButton={TextButton(onClick={bulkDelete=false}){Text(tr("Cancel"))}})
}
internal fun niceScale(max:Double):Double {if(!max.isFinite()||max<=0)return 0.0;val base=10.0.pow(floor(log10(max)));return base*(listOf(1.0,2.0,5.0).lastOrNull{it<=max/base}?:0.5)}
@Composable private fun TrackDetail(t:Track,dao:TrackDao,unit:String,speedUnit:String){
 val points by remember(t.id){dao.points(t.id)}.collectAsState(initial=emptyList())
 Text(tr("Distance: ")+distanceLabel(t.meters,unit),style=MaterialTheme.typography.headlineSmall)
 Text(tr("Recorded time: ")+ltrValue(routeDuration(t.activeMillis)))
 Text(tr("Average: ")+speedLabel(if(t.activeMillis>0)t.meters/t.activeMillis*1000 else 0.0,speedUnit)+" • "+tr("Max: ")+speedLabel(t.maxSpeed,speedUnit))
 Text(tr("Points: ")+points.size)
 val color=MaterialTheme.colorScheme.primary
 val ink=MaterialTheme.colorScheme.onSurface
 val grid=MaterialTheme.colorScheme.outline
 Text(tr("North-up • Green: start • Orange: end/current"),style=MaterialTheme.typography.bodySmall)
 Canvas(Modifier.fillMaxWidth().height(220.dp)){
 for(i in 1..4){val x=size.width*i/5;val y=size.height*i/5;drawLine(grid.copy(alpha=0.15f),Offset(x,0f),Offset(x,size.height));drawLine(grid.copy(alpha=0.15f),Offset(0f,y),Offset(size.width,y))}
 if(points.isNotEmpty()&&size.width>48.dp.toPx()&&size.height>48.dp.toPx()){
 val origin=points.first()
 val xy=points.map{p-> val delta=((p.lon-origin.lon+540)%360)-180;delta*cos(Math.toRadians(origin.lat)) to p.lat-origin.lat}
 val minX=xy.minOf{it.first};val minY=xy.minOf{it.second}
 val scale=minOf((size.width-48.dp.toPx()).coerceAtLeast(1f)/(xy.maxOf{it.first}-minX).coerceAtLeast(0.0002),(size.height-48.dp.toPx()).coerceAtLeast(1f)/(xy.maxOf{it.second}-minY).coerceAtLeast(0.0002))
 val pos=xy.map{Offset(center.x+((it.first-(minX+xy.maxOf{p->p.first})/2)*scale).toFloat(),center.y-((it.second-(minY+xy.maxOf{p->p.second})/2)*scale).toFloat())}
 for(i in 1 until pos.size)if(points[i].segment==points[i-1].segment)drawLine(color,pos[i-1],pos[i],3f)
 drawCircle(Color(0xFF22A65A),5.dp.toPx(),pos.first());drawCircle(Color(0xFFFF9800),4.dp.toPx(),pos.last())
 val metersPerPixel=111195.0/scale
 val maxMeters=size.width*0.25*metersPerPixel
 val bar=niceScale(maxMeters)
 val px=(bar/metersPerPixel).toFloat()
 val paint=android.graphics.Paint(3).apply{this.color=ink.toArgb();textSize=11.dp.toPx()}
 val y=size.height-8.dp.toPx();val x=8.dp.toPx()
 drawLine(ink,Offset(x,y),Offset(x+px,y),2.dp.toPx());drawContext.canvas.nativeCanvas.drawText(if(bar<1)fmtPoint(bar,2)+" m" else distanceLabel(bar,unit),x,y-5.dp.toPx(),paint)
 drawContext.canvas.nativeCanvas.drawText("N ↑",size.width-30.dp.toPx(),15.dp.toPx(),paint)
 }
 }
}
