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
import kotlin.math.cos

@Composable fun TrackScreen(request:()->Unit){
 val context=LocalContext.current
 val dao=remember{TrackDb.get(context).tracks()}
 val tracks by remember{dao.observe()}.collectAsState(initial=emptyList())
 val active=tracks.firstOrNull{it.ended==null}
 var selected by remember{mutableStateOf<Long?>(null)}
 var error by remember{mutableStateOf("")}
 val detail=tracks.firstOrNull{it.id==(selected?:active?.id)}
 fun command(action:String){
 if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){request();return}
 if(action=="start"&&!context.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)){error="Enable GPS first";return}
 try{context.startForegroundService(Intent(context,TrackService::class.java).setAction(action));error=""}catch(e:Exception){error="Unable to start recording: "+e.localizedMessage}
 }
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){
 item{Text("Track recording",style=MaterialTheme.typography.titleLarge);Text(active?.state?.uppercase()?:"READY • Offline")}
 item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
 if(active==null)Button(onClick={selected=null;command("start")}){Text("Record")}
 else{
 Button(onClick={command(if(active.state=="paused")"resume" else "pause")}){Text(if(active.state=="paused")"Resume" else "Pause")}
 OutlinedButton(onClick={command("stop")}){Text("Stop & save")}
 }
 };if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)}
 if(active!=null)item{Text("If recording was interrupted, tap Resume to reconnect.");OutlinedButton(onClick={command("resume")}){Text("Reconnect GPS")}}
 if(detail!=null)item{TrackDetail(detail,dao)}
 item{Text("Saved tracks",style=MaterialTheme.typography.titleMedium)}
 items(tracks.filter{it.ended!=null},key={it.id}){t->OutlinedButton(onClick={selected=t.id},modifier=Modifier.fillMaxWidth()){Text(DateFormat.getDateTimeInstance().format(Date(t.started))+" • "+trackNumber(t.meters/1852)+" NM")}}
 item{Text("Track sketch only — not a nautical chart. Keep independent navigation equipment.")}
 }
}
private fun trackNumber(v:Double)=String.format(Locale.US,"%.2f",v)
@Composable private fun TrackDetail(t:Track,dao:TrackDao){
 val points by remember(t.id){dao.points(t.id)}.collectAsState(initial=emptyList())
 Text("Distance: "+trackNumber(t.meters/1852)+" NM")
 Text("Recorded time: "+t.activeMillis/60000+" min "+t.activeMillis/1000%60+" s")
 Text("Average: "+trackNumber(if(t.activeMillis>0)t.meters/t.activeMillis*1000*3600/1852 else 0.0)+" kn • Max: "+trackNumber(t.maxSpeed*3600/1852)+" kn")
 Text("Points: "+points.size)
 val color=MaterialTheme.colorScheme.primary
 Canvas(Modifier.fillMaxWidth().height(220.dp)){
 if(points.isNotEmpty()){
 val origin=points.first()
 val xy=points.map{p-> val delta=((p.lon-origin.lon+540)%360)-180;delta*cos(Math.toRadians(origin.lat)) to p.lat-origin.lat}
 val minX=xy.minOf{it.first};val minY=xy.minOf{it.second}
 val scale=minOf((size.width-24).coerceAtLeast(1f)/(xy.maxOf{it.first}-minX).coerceAtLeast(0.000001),(size.height-24).coerceAtLeast(1f)/(xy.maxOf{it.second}-minY).coerceAtLeast(0.000001))
 val pos=xy.map{Offset(12+((it.first-minX)*scale).toFloat(),size.height-12-((it.second-minY)*scale).toFloat())}
 for(i in 1 until pos.size)if(points[i].segment==points[i-1].segment)drawLine(color,pos[i-1],pos[i],3f)
 drawCircle(color,5f,pos.first());drawCircle(color,7f,pos.last())
 }
 }
}
