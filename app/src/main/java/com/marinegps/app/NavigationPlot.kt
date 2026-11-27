package com.marinegps.app
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
@Composable internal fun NavigationPlot(fix:Location?,target:Waypoint,reference:Double?,unit:String,session:NavigationSession){
 val context=androidx.compose.ui.platform.LocalContext.current
 var wrecks by remember{mutableStateOf(emptyList<Wreck>())}
 var showWrecks by remember{mutableStateOf(false)}
 LaunchedEffect(showWrecks){if(showWrecks)wrecks=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){runCatching{parseWrecks(context.assets.open("wrecks-ukho.geojson").bufferedReader().use{it.readText()})}.getOrDefault(emptyList())}}
 val trail=session.trail
 var zoom by session::zoom
 val ink=MaterialTheme.colorScheme.onSurface;val accent=MaterialTheme.colorScheme.primary
 if(fix==null||reference==null){Text(if(LocalArabic.current)"انتظار الموقع أو البوصلة" else "Waiting for GPS or compass");return}
 val origin=RouteVertex(fix.latitude,fix.longitude);val goal=RouteVertex(target.latitude,target.longitude)
 val distance=targetMetrics(origin.lat,origin.lon,goal.lat,goal.lon).meters
 val range=max(40.0,max(distance*1.4,session.start?.let{targetMetrics(origin.lat,origin.lon,it.lat,it.lon).meters*1.2}?:0.0))/zoom
 Canvas(Modifier.fillMaxWidth().height(300.dp)){
 val scale=size.minDimension/2/range
 fun pos(p:RouteVertex):Offset{val m=targetMetrics(origin.lat,origin.lon,p.lat,p.lon);val a=Math.toRadians((m.bearing?:0.0)-reference);return Offset(center.x+(sin(a)*m.meters*scale).toFloat(),center.y-(cos(a)*m.meters*scale).toFloat())}
 for(i in 1..4){val x=size.width*i/5;val y=size.height*i/5;drawLine(ink.copy(alpha=.12f),Offset(x,0f),Offset(x,size.height));drawLine(ink.copy(alpha=.12f),Offset(0f,y),Offset(size.width,y))}
 trail.zipWithNext().forEach{(a,b)->if(b.time-a.time<=10000)drawLine(Color(0xFF42A5F5),pos(a.point),pos(b.point),2.dp.toPx())}
 session.start?.let{val start=pos(it);drawLine(ink.copy(alpha=.6f),start,pos(goal),1.dp.toPx(),pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f,6f)));drawCircle(ink,4.dp.toPx(),start)}
 val bar=range/2;val y=size.height-12.dp.toPx();drawLine(ink,Offset(12.dp.toPx(),y),Offset(12.dp.toPx()+(bar*scale).toFloat(),y),2.dp.toPx())
 val paint=android.graphics.Paint(3).apply{color=ink.toArgb();textSize=12.dp.toPx()};drawContext.canvas.nativeCanvas.drawText(distanceLabel(bar,unit),12.dp.toPx(),y-5.dp.toPx(),paint)
 val north=Math.toRadians(-reference);val nc=Offset(size.width-30.dp.toPx(),30.dp.toPx());val tip=nc+Offset((sin(north)*18.dp.toPx()).toFloat(),(-cos(north)*18.dp.toPx()).toFloat());drawLine(ink,nc,tip,2.dp.toPx());drawContext.canvas.nativeCanvas.drawText("N",tip.x,tip.y,paint)
 if(showWrecks)wrecks.forEach{w->val q=pos(RouteVertex(w.lat,w.lon));if(q.x in 0f..size.width&&q.y in 0f..size.height){drawLine(Color.Red,q-Offset(5f,5f),q+Offset(5f,5f),2f);drawLine(Color.Red,q-Offset(5f,-5f),q+Offset(5f,-5f),2f)}}
 val end=pos(goal);drawLine(accent,center,end,2.dp.toPx());drawCircle(Color(0xFFFF9800),6.dp.toPx(),end);drawCircle(Color(0xFFFF9800),(20*scale).toFloat(),end,style=Stroke(1.dp.toPx()));drawCircle(accent,6.dp.toPx(),center)
 }
 Row{TextButton(onClick={zoom=(zoom/1.5f).coerceAtLeast(.25f)}){Text("−")};TextButton(onClick={zoom=1f}){Text(if(LocalArabic.current)"تلقائي" else "Auto")};TextButton(onClick={zoom=(zoom*1.5f).coerceAtMost(8f)}){Text("+")}}
 FilterChip(showWrecks,{showWrecks=!showWrecks},label={Text(if(LocalArabic.current)"الحطام التاريخي (غير مؤكد)" else "Historical wrecks (unverified)")})
 Text(ltrValue(if(distance<1)"< 1 m" else distanceLabel(distance,unit)))
 Text(if(LocalArabic.current)"الأخضر: موقعك وخط الهدف • البرتقالي: الهدف • الأزرق: أثر الحركة" else "Green: position and target line • Orange: target • Blue: trail")
}
