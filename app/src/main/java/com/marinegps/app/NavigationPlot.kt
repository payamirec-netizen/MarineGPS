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
@Composable internal fun NavigationPlot(fix:Location?,target:Waypoint,reference:Double?,unit:String){
 val trail=remember(target.id){mutableStateListOf<RouteVertex>()}
 LaunchedEffect(fix?.elapsedRealtimeNanos,target.id){if(fix!=null){val p=RouteVertex(fix.latitude,fix.longitude);if(trail.lastOrNull()!=p){trail.add(p);if(trail.size>2000)trail.removeAt(0)}}}
 var zoom by remember{mutableFloatStateOf(1f)}
 val ink=MaterialTheme.colorScheme.onSurface;val accent=MaterialTheme.colorScheme.primary
 if(fix==null||reference==null){Text(if(LocalArabic.current)"انتظار الموقع أو البوصلة" else "Waiting for GPS or compass");return}
 val origin=RouteVertex(fix.latitude,fix.longitude);val goal=RouteVertex(target.latitude,target.longitude)
 val distance=targetMetrics(origin.lat,origin.lon,goal.lat,goal.lon).meters
 val range=max(25.0,distance*1.4)/zoom
 Canvas(Modifier.fillMaxWidth().height(300.dp)){
 val scale=size.minDimension/2/range
 fun pos(p:RouteVertex):Offset{val m=targetMetrics(origin.lat,origin.lon,p.lat,p.lon);val a=Math.toRadians((m.bearing?:0.0)-reference);return Offset(center.x+(sin(a)*m.meters*scale).toFloat(),center.y-(cos(a)*m.meters*scale).toFloat())}
 for(i in 1..4){val x=size.width*i/5;val y=size.height*i/5;drawLine(ink.copy(alpha=.12f),Offset(x,0f),Offset(x,size.height));drawLine(ink.copy(alpha=.12f),Offset(0f,y),Offset(size.width,y))}
 trail.zipWithNext().forEach{(a,b)->drawLine(Color(0xFF42A5F5),pos(a),pos(b),2.dp.toPx())}
 val end=pos(goal);drawLine(accent,center,end,2.dp.toPx());drawCircle(Color(0xFFFF9800),6.dp.toPx(),end);drawCircle(Color(0xFFFF9800),(20*scale).toFloat(),end,style=Stroke(1.dp.toPx()));drawCircle(accent,6.dp.toPx(),center)
 }
 Row{TextButton(onClick={zoom=(zoom/1.5f).coerceAtLeast(.25f)}){Text("−")};TextButton(onClick={zoom=1f}){Text(if(LocalArabic.current)"تلقائي" else "Auto")};TextButton(onClick={zoom=(zoom*1.5f).coerceAtMost(8f)}){Text("+")}}
 Text(ltrValue(distanceLabel(distance,unit)))
 Text(if(LocalArabic.current)"الأخضر: موقعك وخط الهدف • البرتقالي: الهدف • الأزرق: أثر الحركة" else "Green: position and target line • Orange: target • Blue: trail")
}
