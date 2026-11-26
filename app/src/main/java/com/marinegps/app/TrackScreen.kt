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
 var selected by remember{mutableStateOf<Long?>(null)}
 var error by remember{mutableStateOf("")}
 val detail=tracks.firstOrNull{it.id==(selected?:active?.id)}
 fun command(action:String){
 if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){request();return}
 if(action in listOf("start","resume","reconnect")&&!context.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)){error="Enable GPS first";return}
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
 if(active?.state=="recording")item{Text(if(!health.running)"Recording service inactive — reconnect" else if(health.lastFix==0L||clock-health.lastFix>10000L)"Waiting for usable GPS" else "GPS connected");Text("Last fix: "+if(health.lastFix==0L)"—" else ((clock-health.lastFix).coerceAtLeast(0)/1000).toString()+" s ago");Text("GPS accuracy: "+(health.accuracy?.let{"± "+fmtPoint(it.toDouble(),1)+" m"}?:"—"));Text("Session gaps: "+health.gaps+" • "+((health.gapMillis+if(health.waitingSince>0)(clock-health.waitingSince).coerceAtLeast(0) else 0)/1000)+" s");Text("Background service: "+if(health.running)"Active" else "Inactive");Text(health.message);val power=context.getSystemService(android.os.PowerManager::class.java);Text("Power saving: "+if(power.isPowerSaveMode)"On" else "Off");Text("Battery optimization: "+if(power.isIgnoringBatteryOptimizations(context.packageName))"Exempt" else "Enabled");TextButton(onClick={runCatching{context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+context.packageName)))}}){Text("App battery settings")};var recovery by remember{mutableStateOf(false)};TextButton(onClick={recovery=!recovery}){Text("Recording help")};if(recovery||!health.running){Text("If recording stops updating, reconnect GPS.");OutlinedButton(onClick={command("reconnect")}){Text("Reconnect GPS")}}}
 if(detail!=null)item{TrackDetail(detail,dao,unit,speedUnit)}
 item{Text("Saved tracks",style=MaterialTheme.typography.titleMedium)}
 items(tracks.filter{it.ended!=null},key={it.id}){t->OutlinedButton(onClick={selected=t.id},modifier=Modifier.fillMaxWidth()){Text(DateFormat.getDateTimeInstance().format(Date(t.started))+" • "+distanceLabel(t.meters,unit))}}
 item{Text("Track sketch only — not a nautical chart. Keep independent navigation equipment.")}
 }
}
internal fun niceScale(max:Double):Double {if(!max.isFinite()||max<=0)return 0.0;val base=10.0.pow(floor(log10(max)));return base*(listOf(1.0,2.0,5.0).lastOrNull{it<=max/base}?:0.5)}
@Composable private fun TrackDetail(t:Track,dao:TrackDao,unit:String,speedUnit:String){
 val points by remember(t.id){dao.points(t.id)}.collectAsState(initial=emptyList())
 Text("Distance: "+distanceLabel(t.meters,unit),style=MaterialTheme.typography.headlineSmall)
 Text("Recorded time: "+t.activeMillis/60000+" min "+t.activeMillis/1000%60+" s")
 Text("Average: "+speedLabel(if(t.activeMillis>0)t.meters/t.activeMillis*1000 else 0.0,speedUnit)+" • Max: "+speedLabel(t.maxSpeed,speedUnit))
 Text("Points: "+points.size)
 val color=MaterialTheme.colorScheme.primary
 val ink=MaterialTheme.colorScheme.onSurface
 val grid=MaterialTheme.colorScheme.outline
 Text("North-up • Green: start • Orange: end/current",style=MaterialTheme.typography.bodySmall)
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
