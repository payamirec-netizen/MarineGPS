package com.marinegps.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlin.math.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Path
import java.util.Locale

private fun groups(s:GnssState)=s.satellites.groupBy{it.system to it.id}.values.toList()
private fun band(s:Satellite):String {
 val f=s.frequency?.div(1e6)?:return "Unknown band"
 return when(s.system){
 1,4->when{abs(f-1575.42)<2->"L1";abs(f-1176.45)<2->"L5";abs(f-1227.60)<2->"L2";else->"Other"}
 6->when{abs(f-1575.42)<2->"E1";abs(f-1176.45)<2->"E5a";abs(f-1207.14)<2->"E5b";abs(f-1278.75)<2->"E6";else->"Other"}
 3->when{f in 1590.0..1610.0->"G1";f in 1235.0..1255.0->"G2";else->"Other"}
 5->when{abs(f-1561.098)<2->"B1I";abs(f-1575.42)<2->"B1C";abs(f-1176.45)<2->"B2a";abs(f-1207.14)<2->"B2";else->"Other"}
 else->"Other"
 }
}
internal fun coordinate(v:Double,lat:Boolean,format:String):String {
 if(format=="DD")return number(v,6)+"°"
 val scale=if(format=="DDM")600000L else 360000L
 val total=(abs(v)*scale).roundToLong();val d=total/scale;val rem=total%scale;val m=if(format=="DDM")rem/10000.0 else rem/6000.0
 val h=if(lat){if(v<0)"S" else "N"}else{if(v<0)"W" else "E"}
 return if(format=="DDM") "$d° "+number(m,4)+"′ "+h else "$d° "+m.toInt()+"′ "+number((rem%6000)/100.0,2)+"″ "+h
}
@Composable private fun NavGlyph(i:Int){
 val color=LocalContentColor.current
 Canvas(Modifier.size(24.dp)){
 val u=size.width/24;fun pt(x:Int,y:Int)=Offset(x*u,y*u)
 when(i){
 5->{drawLine(color,pt(4,19),pt(10,5),2*u);drawLine(color,pt(10,5),pt(20,16),2*u);drawCircle(color,2*u,pt(20,16))}
 4->{drawCircle(color,6*u,pt(12,9),style=Stroke(2*u));drawLine(color,pt(7,13),pt(12,22),2*u);drawLine(color,pt(17,13),pt(12,22),2*u)}
 0->{drawCircle(color,9*u,style=Stroke(2*u));drawLine(color,pt(12,12),pt(17,7),2*u)}
 1->{for(j in 0..3)drawLine(color,pt(4+j*5,20),pt(4+j*5,14-j*3),3*u)}
 2->{drawCircle(color,9*u,style=Stroke(2*u));drawLine(color,pt(2,12),pt(22,12),u);drawLine(color,pt(12,2),pt(12,22),u)}
 3->{drawCircle(color,7*u,style=Stroke(3*u));drawCircle(color,2*u);for(j in 0..7){val a=j*PI/4;drawLine(color,Offset(center.x+(7*u*cos(a)).toFloat(),center.y+(7*u*sin(a)).toFloat()),Offset(center.x+(11*u*cos(a)).toFloat(),center.y+(11*u*sin(a)).toFloat()),2*u)}}
 }
 }
}

private fun number(v:Double,n:Int=1)=String.format(Locale.US,"%."+n+"f",v)
private fun satColor(s:Int)=when(s){1->Color(0xFF42A5F5);3->Color(0xFFFF7043);5->Color(0xFFEAB308);6->Color(0xFF26A69A);4->Color(0xFFAB78E8);else->Color(0xFF9CA3AF)}
@Composable fun MarineApp(s:GnssState,request:()->Unit,settings:()->Unit){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("marine_settings",0)}
 var tab by rememberSaveable{mutableIntStateOf(0)}
 var targetId by rememberSaveable{mutableStateOf<Long?>(null)}
 val dao=remember{WaypointDb.get(context).points()}
 val points by remember(dao){dao.observe()}.collectAsState(initial=emptyList())
 val target=points.firstOrNull{it.id==targetId}
 var units by remember{mutableStateOf(prefs.getString("speed","kn")?:"kn")}
 var distance by remember{mutableStateOf(prefs.getString("distance","NM")?:"NM")}
 var usedOnly by rememberSaveable{mutableStateOf(false)}
 val labels=listOf("Dashboard","Satellites","Sky View","Settings","Waypoints","Tracks")
 var appearance by remember{mutableStateOf(prefs.getString("appearance","System")?:"System")}
 var format by remember{mutableStateOf(prefs.getString("coordinates","DD")?:"DD")}
 val dark=when(appearance){"Dark"->true;"Light"->false;else->isSystemInDarkTheme()}
 MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFF65DBC0),secondaryContainer=Color(0xFF204C47))else lightColorScheme(primary=Color(0xFF006B59),secondaryContainer=Color(0xFFB5EBDD))){
 Scaffold(bottomBar={NavigationBar{labels.forEachIndexed{i,label->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={NavGlyph(i)},label={Text(label,fontSize=10.sp)})}}}){padding->
 Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)){
 Text("Marine GPS",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(top=12.dp))
 Text((if(tab==4&&target!=null)"Navigation" else labels[tab])+" • Native GNSS",color=MaterialTheme.colorScheme.primary)
 Spacer(Modifier.height(12.dp))
 when(tab){
 5->TrackScreen(request)
 0->MarineDashboard(s,units,format,request,settings)
 1->SignalScreen(s,usedOnly,{usedOnly=it})
 2->SkyScreen(s,usedOnly,{usedOnly=it})
 4->if(targetId!=null&&target!=null) NavigationScreen(s,target,distance,units){targetId=null} else WaypointScreen(s,distance,format){targetId=it.id}
 3->LazyColumn(verticalArrangement=Arrangement.spacedBy(4.dp)){
 item{Text("Speed units",style=MaterialTheme.typography.titleMedium)}
 items(listOf("kn","km/h","m/s")){u->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=units==u,onClick={units=u;prefs.edit().putString("speed",u).apply()});Text(u)}}
 item{Text("Distance units",style=MaterialTheme.typography.titleMedium)}
 items(listOf("NM","km","mi")){u->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=distance==u,onClick={distance=u;prefs.edit().putString("distance",u).apply()});Text(u)}}
 item{Text("Appearance",style=MaterialTheme.typography.titleMedium)}
 items(listOf("System","Light","Dark")){u->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=appearance==u,onClick={appearance=u;prefs.edit().putString("appearance",u).apply()});Text(u)}}
 item{Text("Coordinate format",style=MaterialTheme.typography.titleMedium)}
 items(listOf("DD","DDM","DMS")){u->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=format==u,onClick={format=u;prefs.edit().putString("coordinates",u).apply()});Text(u)}}
 item{Button(onClick=request){Text("Location permission")}}
 item{OutlinedButton(onClick=settings){Text("Android location settings")}}
 item{Text("Version 0.5.0 • Offline GNSS")}
 item{Text("Experimental navigation aid. Not a certified marine instrument.")}
 }
 }
 }
 }
}
}
@Composable private fun MarineDashboard(s:GnssState,units:String,format:String,request:()->Unit,settings:()->Unit){
 val loc=s.location
 val age=loc?.let{(s.now-it.elapsedRealtimeNanos/1000000L).coerceAtLeast(0)/1000}
 val fresh=loc!=null&&age!=null&&age<=5
 val speed=if(fresh&&loc!!.hasSpeed())number(loc.speed.toDouble()*when(units){"kn"->3600.0/1852;"km/h"->3.6;else->1.0})+" "+units else "—"
 LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp),contentPadding=PaddingValues(bottom=16.dp)){
 item{Text(if(fresh)"GPS FIX · "+age+"s ago" else if(loc!=null)"STALE FIX" else s.message,color=MaterialTheme.colorScheme.primary)}
 if(s.message.contains("permission"))item{Button(onClick=request){Text("Grant precise location")}}
 if(s.message.contains("Enable location"))item{Button(onClick=settings){Text("Enable GPS")}}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text("Speed over ground");Text(speed,fontSize=34.sp);Text("True course: "+if(fresh&&loc!!.hasBearing())number(loc.bearing.toDouble())+"°" else "—")}}}
 item{DataCard("Horizontal accuracy",if(fresh&&loc!!.hasAccuracy())"± "+number(loc.accuracy.toDouble())+" m · "+when{loc.accuracy<=5->"Good";loc.accuracy<=15->"Moderate";else->"Poor"} else "—")}
 item{DataCard("Coordinates",if(fresh)coordinate(loc!!.latitude,true,format)+"\n"+coordinate(loc.longitude,false,format) else "—")}
 item{DataCard("Altitude (ellipsoid)",if(fresh&&loc!!.hasAltitude())number(loc.altitude)+" m" else "—")}
 item{DataCard("Unique satellites",groups(s).count{g->g.any{it.used}}.toString()+" used / "+groups(s).size+" visible")}
 item{DataCard("GNSS signals",s.satellites.count{it.used}.toString()+" used / "+s.satellites.size+" reported")}
 item{Text("First fix: "+(s.ttff?.let{number(it/1000.0)+" s"}?:"—"))}
 }
}
@Composable private fun DataCard(label:String,value:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(8.dp)){Text(label,style=MaterialTheme.typography.bodySmall);Text(value,fontSize=20.sp)}}}
@Composable private fun Filter(usedOnly:Boolean,change:(Boolean)->Unit){Row(verticalAlignment=Alignment.CenterVertically){Text("Used satellites only",Modifier.weight(1f));Switch(checked=usedOnly,onCheckedChange=change)}}
@Composable private fun SignalScreen(s:GnssState,usedOnly:Boolean,change:(Boolean)->Unit){
 val rows=groups(s).filter{!usedOnly||it.any{v->v.used}}.sortedByDescending{g->g.maxOf{it.cn0}}
 Filter(usedOnly,change)
 Text("Satellites: ${groups(s).count{g->g.any{it.used}}} used / ${groups(s).size} visible")
 Text("Signals: ${s.satellites.count{it.used}} used / ${s.satellites.size} reported")
 if(rows.isEmpty())Text("Waiting for satellite signals")
 LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)){
 items(rows){group->Column{
 val first=group.first()
 Text(constellationName(first.system)+" "+first.id+" · "+group.size+if(group.size==1)" signal" else " signals",style=MaterialTheme.typography.titleMedium)
 group.sortedByDescending{it.cn0}.forEach{sat->
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(band(sat)+if(sat.used)" ✓" else "");Text(number(sat.cn0.toDouble())+" dB-Hz")}
 Canvas(Modifier.fillMaxWidth().height(8.dp)){drawRoundRect(color=Color.Gray.copy(alpha=0.25f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()));drawRoundRect(color=satColor(sat.system),size=androidx.compose.ui.geometry.Size(size.width*(sat.cn0/60f).coerceIn(0f,1f),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))}
 Text("Az "+number(sat.azimuth.toDouble(),0)+"° · El "+number(sat.elevation.toDouble(),0)+"°"+(sat.frequency?.let{" · "+number(it/1e6)+" MHz"}?:""),style=MaterialTheme.typography.bodySmall)
 }}}
 }
}
@Composable private fun SkyScreen(s:GnssState,usedOnly:Boolean,change:(Boolean)->Unit){
 val rows=groups(s).map{g->g.maxByOrNull{it.cn0}!!.copy(used=g.any{it.used})}.filter{(!usedOnly||it.used)&&it.elevation in 0f..90f&&it.azimuth.isFinite()}
 val grid=MaterialTheme.colorScheme.outline
 val ink=MaterialTheme.colorScheme.onSurface
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
 item{Filter(usedOnly,change)}
 item{Text("True north • Rings: 0°, 30°, 60° elevation")}
 item{Box(Modifier.fillMaxWidth().aspectRatio(1f)){
 Canvas(Modifier.fillMaxSize().padding(24.dp)){
 val c=center;val r=size.minDimension/2
 for(f in listOf(1f,2f/3,1f/3))drawCircle(grid,r*f,c,style=Stroke(1.dp.toPx()))
 drawLine(grid,Offset(c.x-r,c.y),Offset(c.x+r,c.y));drawLine(grid,Offset(c.x,c.y-r),Offset(c.x,c.y+r))
 val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=ink.toArgb();textSize=10.dp.toPx();textAlign=android.graphics.Paint.Align.CENTER;isFakeBoldText=true}
 val occupied=mutableListOf<androidx.compose.ui.geometry.Rect>()
 val positions=rows.sortedWith(compareByDescending<Satellite>{it.used}.thenBy{it.system}.thenBy{it.id}).map{sat->
 val a=sat.azimuth*PI/180;val d=r*(90-sat.elevation)/90
 sat to Offset(c.x+(d*sin(a)).toFloat(),c.y-(d*cos(a)).toFloat())
 }
 positions.forEach{(sat,pt)->drawCircle(satColor(sat.system),4.dp.toPx(),pt);if(sat.used)drawCircle(ink,6.dp.toPx(),pt,style=Stroke(1.dp.toPx()))}
 positions.forEach{(sat,pt)->
 val label=(when(sat.system){1->"G";3->"R";4->"J";5->"C";6->"E";2->"S";7->"I";else->"?"})+sat.id
 val w=paint.measureText(label)/2+3.dp.toPx();val h=7.dp.toPx()
 val candidates=(1..5).flatMap{ring->(0..7).map{k->val a=k*PI/4;val d=(12+ring*9).dp.toPx();Offset(pt.x+(d*cos(a)).toFloat(),pt.y+(d*sin(a)).toFloat())}}
 val chosen=candidates.firstOrNull{p->val rect=androidx.compose.ui.geometry.Rect(p.x-w,p.y-h,p.x+w,p.y+h);rect.left>=0&&rect.top>=0&&rect.right<=size.width&&rect.bottom<=size.height&&occupied.none{it.overlaps(rect)}&&positions.none{(_,q)->rect.inflate(5.dp.toPx()).contains(q)}}
 if(chosen!=null){occupied.add(androidx.compose.ui.geometry.Rect(chosen.x-w,chosen.y-h,chosen.x+w,chosen.y+h));drawLine(satColor(sat.system),pt,chosen,1.dp.toPx());drawContext.canvas.nativeCanvas.drawText(label,chosen.x,chosen.y-(paint.ascent()+paint.descent())/2,paint)}
 }

 }
 Text("N",Modifier.align(Alignment.TopCenter));Text("S",Modifier.align(Alignment.BottomCenter));Text("W",Modifier.align(Alignment.CenterStart));Text("E",Modifier.align(Alignment.CenterEnd))
 }}
 item{Text("Outlined dots = used in fix. Lines connect labels to true positions. Center = zenith.")}
 if(rows.isEmpty())item{Text("Waiting for satellite positions")}
 items(rows.groupBy{it.system}.entries.toList()){entry->Row(verticalAlignment=Alignment.CenterVertically){Canvas(Modifier.size(16.dp)){drawCircle(satColor(entry.key))};Spacer(Modifier.width(8.dp));Text(constellationName(entry.key)+" · "+entry.value.size+if(entry.value.size==1)" satellite" else " satellites")}}
 }
}
