package com.marinegps.app
import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
class MainActivity:ComponentActivity(){
 private lateinit var model:GnssModel
 private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){if(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))model.start()}
 override fun onCreate(savedInstanceState:Bundle?){
 super.onCreate(savedInstanceState)
 model=ViewModelProvider(this)[GnssModel::class.java]
 setContent{
 val state by model.state.collectAsStateWithLifecycle()
 val dark=androidx.compose.foundation.isSystemInDarkTheme()
 MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFF65DBC0))else lightColorScheme(primary=Color(0xFF006B59))){
 Dashboard(state,{permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))},{startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))})
 }
 }
 }
 override fun onResume(){super.onResume();model.start()}
 override fun onPause(){model.stop();super.onPause()}
}
private fun fmt(value:Double,digits:Int=1)=String.format(Locale.US,"%."+digits+"f",value)
@Composable fun Dashboard(s:GnssState,request:()->Unit,settings:()->Unit){
 val loc=s.location
 val age=loc?.let{(s.now-it.elapsedRealtimeNanos/1000000L).coerceAtLeast(0)/1000}
 val fresh=loc!=null&&age!=null&&age<=5
 Scaffold{padding->
 LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=20.dp)){
 item{Text("Marine GPS",style=MaterialTheme.typography.headlineLarge);Text("Native GNSS • Offline",color=MaterialTheme.colorScheme.primary)}
 item{Text(if(fresh)"GPS FIX · ${age}s ago" else if(loc!=null)"STALE FIX · ${age}s ago" else s.message,color=MaterialTheme.colorScheme.primary)}
 if(s.message.contains("permission"))item{Button(onClick=request){Text("Grant precise location")}}
 if(s.message.contains("Enable location"))item{Button(onClick=settings){Text("Enable GPS")}}
 item{Metric("Horizontal accuracy",if(fresh&&loc!!.hasAccuracy())"± "+fmt(loc.accuracy.toDouble())+" m" else "—")}
 item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Box(Modifier.weight(1f)){Metric("Speed",if(fresh&&loc!!.hasSpeed())fmt(loc.speed*3600.0/1852)+" kn" else "—")};Box(Modifier.weight(1f)){Metric("Course (true)",if(fresh&&loc!!.hasBearing())fmt(loc.bearing.toDouble())+"°" else "—")}}}
 item{Metric("Coordinates",if(fresh)fmt(loc!!.latitude,6)+"°\n"+fmt(loc.longitude,6)+"°" else "—")}
 item{Metric("Altitude (ellipsoid)",if(fresh&&loc!!.hasAltitude())fmt(loc.altitude)+" m" else "—")}
 item{Text("First fix: "+(s.ttff?.let{fmt(it/1000.0)+" s"}?:"—"));Text("Satellite signals: "+s.satellites.count{it.used}+" used / "+s.satellites.size+" reported",style=MaterialTheme.typography.titleMedium)}
 items(s.satellites.sortedWith(compareByDescending<Satellite>{it.used}.thenByDescending{it.cn0})){sat->
 Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(constellationName(sat.system)+" "+sat.id+if(sat.used)" • used" else "");Text(fmt(sat.cn0.toDouble())+" dB-Hz")};LinearProgressIndicator(progress={(sat.cn0/60f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth());Text("Az "+fmt(sat.azimuth.toDouble(),0)+"° · El "+fmt(sat.elevation.toDouble(),0)+"°"+(sat.frequency?.let{" · "+fmt(it/1e6)+" MHz"}?:""),style=MaterialTheme.typography.bodySmall)}
 }
 item{Text("Not a certified marine navigation instrument.",style=MaterialTheme.typography.bodySmall)}
 }
 }
}
@Composable private fun Metric(label:String,value:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(label,style=MaterialTheme.typography.labelLarge);Text(value,fontSize=26.sp)}}}
