package com.marinegps.app
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

internal fun distanceLabel(m:Double,unit:String)=if(m<1000)fmtPoint(m,0)+" m" else fmtPoint(m/when(unit){"NM"->1852.0;"mi"->1609.344;else->1000.0},2)+" "+unit
internal fun usableBearing(m:TargetMetrics,accuracy:Float?,saved:Float?):Double? {
 if(accuracy==null||!accuracy.isFinite()||accuracy<0)return null
 val uncertainty=accuracy.toDouble()+(saved?.takeIf{it.isFinite()&&it>=0}?:0f)
 return m.bearing?.takeIf{m.meters>max(10.0,uncertainty)}
}
internal fun etaSeconds(m:Double,speed:Double?,course:Double?,bearing:Double?):Double? {
 if(speed==null||course==null||bearing==null||!speed.isFinite()||speed<0.5)return null
 val closing=speed*cos(Math.toRadians(bearing-course))
 return if(closing>0.3)m/closing else null
}
@Composable fun NavigationScreen(s:GnssState,target:Waypoint,unit:String,speedUnit:String,stop:()->Unit){
 val fix=currentFix(s)
 val m=fix?.let{targetMetrics(it.latitude,it.longitude,target.latitude,target.longitude)}
 val accuracy=fix?.takeIf{it.hasAccuracy()}?.accuracy
 val bearing=m?.let{usableBearing(it,accuracy,target.accuracy)}
 val speed=fix?.takeIf{it.hasSpeed()}?.speed?.toDouble()
 val course=fix?.takeIf{it.hasBearing()&&it.hasSpeed()&&it.speed>=0.5f&&(!it.hasBearingAccuracy()||it.bearingAccuracyDegrees<=30f)}?.bearing?.toDouble()
 val eta=m?.let{etaSeconds(it.meters,speed,course,bearing)}
 val ink=MaterialTheme.colorScheme.onSurface
 val accent=MaterialTheme.colorScheme.primary
 val outline=MaterialTheme.colorScheme.outline
 LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){
 item{Text("Navigating to",style=MaterialTheme.typography.labelLarge);Text(target.name,style=MaterialTheme.typography.headlineSmall);Text(target.category)}
 if(target.category=="Hazard")item{Text("Hazard waypoint — do not approach blindly",color=MaterialTheme.colorScheme.error)}
 item{Text(if(fix==null)"GPS unavailable — navigation paused" else if(bearing==null)"Direction uncertain — near target or insufficient accuracy" else "GPS FIX • North-up",color=if(bearing==null)MaterialTheme.colorScheme.error else accent)}
 item{Box(Modifier.fillMaxWidth().height(260.dp)){
 Canvas(Modifier.fillMaxSize().padding(24.dp)){
 val r=size.minDimension/2;val c=center
 drawCircle(outline,r,c,style=Stroke(1.dp.toPx()))
 for(i in 0 until 36){
 val a=i*PI/18
 val d=if(i%3==0)0.86f else 0.94f
 val inner=Offset(c.x+(sin(a)*r*d).toFloat(),c.y-(cos(a)*r*d).toFloat())
 val outer=Offset(c.x+(sin(a)*r).toFloat(),c.y-(cos(a)*r).toFloat())
 drawLine(outline,inner,outer,1.dp.toPx())
 }
 fun point(a:Double,k:Float)=Offset(c.x+(sin(Math.toRadians(a))*r*k).toFloat(),c.y-(cos(Math.toRadians(a))*r*k).toFloat())
 bearing?.let{b->val tip=point(b,0.8f);val left=point(b-155,0.27f);val right=point(b+155,0.27f);drawPath(Path().apply{moveTo(tip.x,tip.y);lineTo(left.x,left.y);lineTo(c.x,c.y);lineTo(right.x,right.y);close()},accent)}
 course?.let{drawLine(ink,c,point(it,0.95f),3.dp.toPx())}
 drawCircle(ink,3.dp.toPx(),c)
 }
 Text("N",Modifier.align(Alignment.TopCenter));Text("S",Modifier.align(Alignment.BottomCenter));Text("W",Modifier.align(Alignment.CenterStart));Text("E",Modifier.align(Alignment.CenterEnd))
 }}
 item{Text("Green: target bearing • White: GPS course. Not phone heading.")}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
 Text("Distance: "+(m?.let{distanceLabel(it.meters,unit)}?:"—"),style=MaterialTheme.typography.headlineSmall)
 Text("Bearing true: "+(bearing?.let{fmtPoint(it,1)+"°"}?:"—"))
 Text("SOG: "+(speed?.let{fmtPoint(it*when(speedUnit){"kn"->3600.0/1852;"km/h"->3.6;else->1.0},1)+" "+speedUnit}?:"—"))
 Text("COG true: "+(course?.let{fmtPoint(it,1)+"°"}?:"—"))
 Text("ETA (current closing speed): "+(eta?.let{fmtPoint(it/60,1)+" min"}?:"—"))
 Text("GPS accuracy: "+(accuracy?.let{"± "+fmtPoint(it.toDouble(),1)+" m"}?:"—"))
 if(m!=null&&m.meters<=max(20.0,(accuracy?:0f).toDouble()+(target.accuracy?:0f)))Text("Near target — verify position visually")
 }}}
 item{Text("Direct bearing only. No charts, depth or obstacle avoidance. Keep a separate navigation aid.")}
 item{OutlinedButton(onClick=stop,modifier=Modifier.fillMaxWidth()){Text("Stop navigation")}}
 }
}
