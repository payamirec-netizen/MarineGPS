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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlin.math.*

internal fun distanceLabel(m:Double,unit:String)=if(m<1000)fmtPoint(m,0)+" m" else fmtPoint(m/when(unit){"NM"->1852.0;"mi"->1609.344;else->1000.0},2)+" "+unit
internal fun usableBearing(m:TargetMetrics,accuracy:Float?,saved:Float?):Double? {
 if(accuracy==null||!accuracy.isFinite()||accuracy<0)return null
 val uncertainty=accuracy.toDouble()+(saved?.takeIf{it.isFinite()&&it>=0}?:0f)
 return m.bearing?.takeIf{m.meters>max(10.0,uncertainty)}
}
internal fun speedLabel(mps:Double,unit:String)=fmtPoint(mps*when(unit){"kn"->3600.0/1852;"km/h"->3.6;else->1.0},1)+" "+unit
internal fun courseReason(speed:Double?,bearing:Double?,accuracy:Double?):String? = when {speed==null||!speed.isFinite()->"Speed unavailable";speed<0.5->"Moving too slowly";bearing==null||!bearing.isFinite()->"GPS course unavailable";accuracy!=null&&(!accuracy.isFinite()||accuracy<0||accuracy>30)->"GPS course accuracy low";else->null}
internal fun etaSeconds(m:Double,speed:Double?,course:Double?,bearing:Double?):Double? {
 if(speed==null||course==null||bearing==null||!speed.isFinite()||speed<0.5)return null
 val closing=speed*cos(Math.toRadians(bearing-course))
 return if(closing>0.3)m/closing else null
}
@Composable fun NavigationScreen(s:GnssState,target:Waypoint,unit:String,speedUnit:String,session:NavigationSession,stop:()->Unit){
 val fix=currentFix(s)
 val m=fix?.let{targetMetrics(it.latitude,it.longitude,target.latitude,target.longitude)}
 val accuracy=fix?.takeIf{it.hasAccuracy()}?.accuracy
 val bearing=m?.let{usableBearing(it,accuracy,target.accuracy)}
 val speed=fix?.takeIf{it.hasSpeed()}?.speed?.toDouble()
 val rawCourse=fix?.takeIf{it.hasBearing()}?.bearing?.toDouble()
 val reason=courseReason(speed,rawCourse,fix?.takeIf{it.hasBearingAccuracy()}?.bearingAccuracyDegrees?.toDouble())
 val course=rawCourse.takeIf{reason==null}
 val near=m!=null&&m.meters<=max(20.0,(accuracy?:0f).toDouble()+(target.accuracy?:0f))
 val eta=m?.let{etaSeconds(it.meters,speed,course,bearing)}
 val context=LocalContext.current
 val prefs=remember(context){context.getSharedPreferences("marine_settings",0)}
 var headingUp by rememberSaveable{mutableStateOf(prefs.getBoolean("heading_up",true))}
 var keepOn by rememberSaveable{mutableStateOf(prefs.getBoolean("navigation_keep_on",true))}
 KeepNavigationScreenOn(keepOn)
 val headingInfo=rememberHeadingInfo(fix,headingUp)
 val heading=headingInfo.heading
 val reference=if(headingUp)heading else 0.0
 val relative=bearing?.let{b->reference?.let{relativeHeading(b,it)}}
 var plot by session::plot
 val ink=MaterialTheme.colorScheme.onSurface
 val accent=MaterialTheme.colorScheme.primary
 val outline=MaterialTheme.colorScheme.outline
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(4.dp)){
 Text((if(LocalArabic.current)"الملاحة إلى" else "Navigating to")+" • "+target.name,style=MaterialTheme.typography.titleMedium,maxLines=2)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=headingUp,onClick={headingUp=true;prefs.edit().putBoolean("heading_up",true).apply()},label={Text(tr("Heading-up"))});FilterChip(selected=!headingUp,onClick={headingUp=false;prefs.edit().putBoolean("heading_up",false).apply()},label={Text(tr("North-up"))})}
 Row{FilterChip(!plot,{plot=false},label={Text(if(LocalArabic.current)"البوصلة" else "Compass")});Spacer(Modifier.width(8.dp));FilterChip(plot,{plot=true},label={Text(if(LocalArabic.current)"الرسم الملاحي" else "Plotter")})}
 if(near)Text(tr("Near destination — verify visually"),color=accent,style=MaterialTheme.typography.bodyMedium)
 LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
 if(target.category=="Hazard")item{Text(tr("Hazard waypoint — do not approach blindly"),color=MaterialTheme.colorScheme.error)}
 item{Text(tr(if(fix==null)"GPS unavailable — navigation paused" else if(bearing==null&&near)"" else if(bearing==null)"Direction uncertain — near target or insufficient accuracy" else if(headingUp&&heading==null)"Compass unavailable — hold phone flat, calibrate away from metal or select North-up" else tr("GPS FIX")+" • "+tr(if(headingUp)"Heading-up" else "North-up")),color=if(bearing==null||reference==null)MaterialTheme.colorScheme.error else accent)}
 if(plot)item{NavigationPlot(fix,target,reference,unit,session)} else item{Box(Modifier.fillMaxWidth().height(200.dp)){
 Canvas(Modifier.fillMaxSize().padding(24.dp)){
 val r=size.minDimension/2;val c=center
 drawCircle(outline,r,c,style=Stroke(1.dp.toPx()))
 for(i in 0 until 36){
 val a=i*PI/18-Math.toRadians(reference?:0.0)
 val d=if(i%3==0)0.86f else 0.94f
 val inner=Offset(c.x+(sin(a)*r*d).toFloat(),c.y-(cos(a)*r*d).toFloat())
 val outer=Offset(c.x+(sin(a)*r).toFloat(),c.y-(cos(a)*r).toFloat())
 drawLine(outline,inner,outer,1.dp.toPx())
 }
 fun point(a:Double,k:Float)=Offset(c.x+(sin(Math.toRadians(a))*r*k).toFloat(),c.y-(cos(Math.toRadians(a))*r*k).toFloat())
 relative?.let{b->val tip=point(b,0.8f);val left=point(b-155,0.27f);val right=point(b+155,0.27f);drawPath(Path().apply{moveTo(tip.x,tip.y);lineTo(left.x,left.y);lineTo(c.x,c.y);lineTo(right.x,right.y);close()},accent)}
 if(reference!=null&&relative!=null)course?.let{drawLine(ink,c,point(relativeHeading(it,reference),0.95f),3.dp.toPx())}
 if(reference!=null){val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=ink.toArgb();textSize=14.dp.toPx();textAlign=android.graphics.Paint.Align.CENTER};listOf(0.0 to "N",90.0 to "E",180.0 to "S",270.0 to "W").forEach{(a,label)->val p=point(relativeHeading(a,reference),1.12f);drawContext.canvas.nativeCanvas.drawText(label,p.x,p.y-(paint.ascent()+paint.descent())/2,paint)}}
 if(relative!=null)drawCircle(ink,3.dp.toPx(),c)
 }
 if(relative==null&&!near)Text(tr(if(fix==null)"GPS unavailable" else if(bearing==null)"Direction uncertain" else "Compass unavailable"),Modifier.align(Alignment.Center),style=MaterialTheme.typography.bodySmall)
 }}
 if(!plot)item{Text(tr("Green: target • White: GPS course. Hold phone flat."))}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
 Text(tr("Distance: ")+(m?.let{ltrValue(distanceLabel(it.meters,unit))}?:"—"),style=MaterialTheme.typography.headlineSmall)
 Text(tr("Bearing true: ")+(bearing?.let{ltrValue(fmtPoint(it,1)+"°")}?:"—"))
 Text("SOG: "+(speed?.let{ltrValue(speedLabel(it,speedUnit))}?:"—"))
 Text(tr("Phone heading true: ")+(heading?.let{ltrValue(fmtPoint(it,1)+"°")}?:"—"))
 if(headingUp)Text(tr("Compass: ")+tr(headingInfo.status),style=MaterialTheme.typography.bodySmall)
 Text(tr("GPS fix age: ")+(fix?.let{((android.os.SystemClock.elapsedRealtime()-it.elapsedRealtimeNanos/1000000L).coerceAtLeast(0)/1000).toString()+" s"}?:"—"))
 Text("COG true: "+(course?.let{fmtPoint(it,1)+"°"}?:("— ("+tr(reason.orEmpty())+")")))
 Text(tr("ETA (current closing speed): ")+(eta?.let{fmtPoint(it/60,1)+" min"}?:"—"))
 Text(tr("GPS accuracy: ")+(accuracy?.let{"± "+fmtPoint(it.toDouble(),1)+" m"}?:"—"))

 }}}
 item{Text(tr("Direct bearing only. No charts, depth or obstacle avoidance. Keep a separate navigation aid."))}
 item{Row(verticalAlignment=Alignment.CenterVertically){Text(tr("Keep screen on"),Modifier.weight(1f));Switch(checked=keepOn,onCheckedChange={keepOn=it;prefs.edit().putBoolean("navigation_keep_on",it).apply()})}}
 }
 OutlinedButton(onClick=stop,modifier=Modifier.fillMaxWidth()){Text(tr("Stop navigation"))}
 }
}
