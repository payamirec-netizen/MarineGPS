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
import java.util.Locale

private fun number(v:Double,n:Int=1)=String.format(Locale.US,"%."+n+"f",v)
private fun satColor(s:Int)=when(s){1->Color(0xFF42A5F5);3->Color(0xFFFF7043);5->Color(0xFFEAB308);6->Color(0xFF26A69A);4->Color(0xFFAB78E8);else->Color(0xFF9CA3AF)}
@Composable fun MarineApp(s:GnssState,request:()->Unit,settings:()->Unit){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("marine_settings",0)}
 var tab by rememberSaveable{mutableIntStateOf(0)}
 var units by remember{mutableStateOf(prefs.getString("speed","kn")?:"kn")}
 var usedOnly by rememberSaveable{mutableStateOf(false)}
 val labels=listOf("Dashboard","Satellites","Sky View","Settings")
 val symbols=listOf("◉","▥","⊕","⚙")
 Scaffold(bottomBar={NavigationBar{labels.forEachIndexed{i,label->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(symbols[i],fontSize=24.sp)},label={Text(label,fontSize=10.sp)})}}}){padding->
 Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)){
 Text("Marine GPS",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(top=12.dp))
 Text(labels[tab]+" • Native GNSS",color=MaterialTheme.colorScheme.primary)
 Spacer(Modifier.height(12.dp))
 when(tab){
 0->MarineDashboard(s,units,request,settings)
 1->SignalScreen(s,usedOnly,{usedOnly=it})
 2->SkyScreen(s,usedOnly,{usedOnly=it})
 3->LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){
 item{Text("Speed units",style=MaterialTheme.typography.titleMedium)}
 items(listOf("kn","km/h","m/s")){u->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=units==u,onClick={units=u;prefs.edit().putString("speed",u).apply()});Text(u)}}
 item{Button(onClick=request){Text("Location permission")}}
 item{OutlinedButton(onClick=settings){Text("Android location settings")}}
 item{Text("Version 0.2.0 • Offline GNSS")}
 item{Text("Experimental navigation aid. Not a certified marine instrument.")}
 }
 }
 }
 }
}
@Composable private fun MarineDashboard(s:GnssState,units:String,request:()->Unit,settings:()->Unit){
 val loc=s.location
 val age=loc?.let{(s.now-it.elapsedRealtimeNanos/1000000L).coerceAtLeast(0)/1000}
 val fresh=loc!=null&&age!=null&&age<=5
 val speed=if(fresh&&loc!!.hasSpeed())number(loc.speed.toDouble()*when(units){"kn"->3600.0/1852;"km/h"->3.6;else->1.0})+" "+units else "—"
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=16.dp)){
 item{Text(if(fresh)"GPS FIX · "+age+"s ago" else if(loc!=null)"STALE FIX" else s.message,color=MaterialTheme.colorScheme.primary)}
 if(s.message.contains("permission"))item{Button(onClick=request){Text("Grant precise location")}}
 if(s.message.contains("Enable location"))item{Button(onClick=settings){Text("Enable GPS")}}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("Speed over ground");Text(speed,fontSize=48.sp);Text("True course: "+if(fresh&&loc!!.hasBearing())number(loc.bearing.toDouble())+"°" else "—")}}}
 item{DataCard("Horizontal accuracy",if(fresh&&loc!!.hasAccuracy())"± "+number(loc.accuracy.toDouble())+" m" else "—")}
 item{DataCard("Coordinates (decimal degrees)",if(fresh)number(loc!!.latitude,6)+"°\n"+number(loc.longitude,6)+"°" else "—")}
 item{DataCard("Altitude (ellipsoid)",if(fresh&&loc!!.hasAltitude())number(loc.altitude)+" m" else "—")}
 item{DataCard("GNSS signals",s.satellites.count{it.used}.toString()+" used / "+s.satellites.size+" reported")}
 item{Text("First fix: "+(s.ttff?.let{number(it/1000.0)+" s"}?:"—"))}
 }
}
@Composable private fun DataCard(label:String,value:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(label);Text(value,fontSize=25.sp)}}}
@Composable private fun Filter(usedOnly:Boolean,change:(Boolean)->Unit){Row(verticalAlignment=Alignment.CenterVertically){Text("Used in fix only",Modifier.weight(1f));Switch(checked=usedOnly,onCheckedChange=change)}}
@Composable private fun SignalScreen(s:GnssState,usedOnly:Boolean,change:(Boolean)->Unit){
 val rows=s.satellites.filter{!usedOnly||it.used}.sortedWith(compareByDescending<Satellite>{it.used}.thenByDescending{it.cn0})
 Filter(usedOnly,change)
 Text("${s.satellites.count{it.used}} used / ${s.satellites.size} reported")
 if(rows.isEmpty())Text("Waiting for satellite signals")
 LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)){
 items(rows){sat->Column{
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(constellationName(sat.system)+" "+sat.id+if(sat.used)" ✓" else "");Text(number(sat.cn0.toDouble())+" dB-Hz")}
 LinearProgressIndicator(progress={(sat.cn0/60f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(12.dp),color=satColor(sat.system))
 Text("Az "+number(sat.azimuth.toDouble(),0)+"° · El "+number(sat.elevation.toDouble(),0)+"°"+(sat.frequency?.let{" · "+number(it/1e6)+" MHz"}?:""),style=MaterialTheme.typography.bodySmall)
 }}
 }
}
@Composable private fun SkyScreen(s:GnssState,usedOnly:Boolean,change:(Boolean)->Unit){
 val rows=s.satellites.filter{(!usedOnly||it.used)&&it.elevation in 0f..90f&&it.azimuth.isFinite()}
 val grid=MaterialTheme.colorScheme.outline
 val ink=MaterialTheme.colorScheme.onSurface
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
 item{Filter(usedOnly,change)}
 item{Text("True north • Horizon at outer ring")}
 item{Box(Modifier.fillMaxWidth().aspectRatio(1f)){
 Canvas(Modifier.fillMaxSize().padding(24.dp)){
 val c=center;val r=size.minDimension/2
 for(f in listOf(1f,2f/3,1f/3))drawCircle(grid,r*f,c,style=Stroke(1.dp.toPx()))
 drawLine(grid,Offset(c.x-r,c.y),Offset(c.x+r,c.y));drawLine(grid,Offset(c.x,c.y-r),Offset(c.x,c.y+r))
 rows.forEach{sat->val a=sat.azimuth*PI/180;val d=r*(90-sat.elevation)/90;val pt=Offset(c.x+(d*sin(a)).toFloat(),c.y-(d*cos(a)).toFloat());drawCircle(satColor(sat.system),7.dp.toPx(),pt);if(sat.used)drawCircle(ink,9.dp.toPx(),pt,style=Stroke(2.dp.toPx()))}
 }
 Text("N",Modifier.align(Alignment.TopCenter));Text("S",Modifier.align(Alignment.BottomCenter));Text("W",Modifier.align(Alignment.CenterStart));Text("E",Modifier.align(Alignment.CenterEnd))
 }}
 item{Text("Outlined markers = used in fix. Center = zenith.")}
 if(rows.isEmpty())item{Text("Waiting for satellite positions")}
 items(rows.groupBy{it.system}.entries.toList()){entry->Row(verticalAlignment=Alignment.CenterVertically){Canvas(Modifier.size(16.dp)){drawCircle(satColor(entry.key))};Spacer(Modifier.width(8.dp));Text(constellationName(entry.key)+" · "+entry.value.size+" signals")}}
 }
}
