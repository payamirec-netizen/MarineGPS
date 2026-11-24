package com.marinegps.app
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale

private val categories=listOf("Fishing","Anchorage","Departure","Hazard","Other")
private fun currentFix(s:GnssState)=s.location?.takeIf{freshFix(SystemClock.elapsedRealtime()-it.elapsedRealtimeNanos/1000000L)&&validCoordinates(it.latitude,it.longitude)}
private fun fmtPoint(v:Double,n:Int)=String.format(Locale.US,"%."+n+"f",v)
@Composable fun WaypointScreen(s:GnssState,unit:String,format:String){
 val context=LocalContext.current
 val dao=remember{WaypointDb.get(context).points()}
 val flow=remember(dao){dao.observe()}
 val points by flow.collectAsState(initial=emptyList())
 val scope=rememberCoroutineScope()
 var search by remember{mutableStateOf("")}
 var edit by remember{mutableStateOf<Waypoint?>(null)}
 var remove by remember{mutableStateOf<Waypoint?>(null)}
 var error by remember{mutableStateOf("")}
 var busy by remember{mutableStateOf(false)}
 val fix=currentFix(s)
 fun create(gps:Boolean){
  val loc=if(gps)currentFix(s) else null
  if(gps&&loc==null){error="No fresh GPS fix";return}
  edit=Waypoint(name="",latitude=loc?.latitude?:Double.NaN,longitude=loc?.longitude?:Double.NaN,category="Fishing",notes="",createdAt=System.currentTimeMillis(),accuracy=loc?.takeIf{it.hasAccuracy()}?.accuracy)
 }
 LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={create(true)},enabled=fix!=null&&!busy,modifier=Modifier.weight(1f)){Text("Save GPS")};OutlinedButton(onClick={create(false)},enabled=!busy,modifier=Modifier.weight(1f)){Text("Add manually")}}}
 if(fix==null)item{Text("Waiting for a fresh GPS fix",color=MaterialTheme.colorScheme.error)}
 if(error.isNotEmpty())item{Text(error,color=MaterialTheme.colorScheme.error)}
 item{OutlinedTextField(search,{search=it},label={Text("Search waypoints")},singleLine=true,modifier=Modifier.fillMaxWidth())}
 item{Text("${points.size} saved waypoints")}
 val filtered=points.filter{it.name.contains(search,true)||it.category.contains(search,true)||it.notes.contains(search,true)}
 if(filtered.isEmpty())item{Text("No matching waypoints")}
 items(filtered,key={it.id}){p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
 Text(p.name,style=MaterialTheme.typography.titleLarge)
 Text(p.category+" · "+coordinate(p.latitude,true,format)+" / "+coordinate(p.longitude,false,format))
 p.accuracy?.let{Text("Saved GPS accuracy: ± "+fmtPoint(it.toDouble(),1)+" m")}
 if(p.notes.isNotBlank())Text(p.notes)
 if(fix!=null){val m=targetMetrics(fix.latitude,fix.longitude,p.latitude,p.longitude);val d=m.meters/when(unit){"NM"->1852.0;"mi"->1609.344;else->1000.0};Text(fmtPoint(d,3)+" "+unit+" · Bearing true: "+(m.bearing?.let{fmtPoint(it,1)+"°"}?:"—"))}else Text("Distance / bearing unavailable")
 Row{TextButton(onClick={edit=p},enabled=!busy){Text("Edit")};TextButton(onClick={remove=p},enabled=!busy){Text("Delete")}}
 }}}
 }
 edit?.let{p->WaypointEditor(p,{edit=null},{updated->busy=true;scope.launch{try{if(updated.id==0L)dao.insert(updated)else dao.update(updated);edit=null;error=""}catch(e:Exception){error="Unable to save waypoint"}finally{busy=false}}},busy)}
 remove?.let{p->AlertDialog(onDismissRequest={if(!busy)remove=null},title={Text("Delete waypoint?")},text={Text(p.name)},confirmButton={TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{dao.delete(p);remove=null;error=""}catch(e:Exception){error="Unable to delete waypoint"}finally{busy=false}}}){Text("Delete")}},dismissButton={TextButton(onClick={remove=null},enabled=!busy){Text("Cancel")}})}
}
@Composable private fun WaypointEditor(p:Waypoint,cancel:()->Unit,save:(Waypoint)->Unit,busy:Boolean){
 var name by remember(p){mutableStateOf(p.name)}
 var lat by remember(p){mutableStateOf(if(p.latitude.isFinite())p.latitude.toString() else "")}
 var lon by remember(p){mutableStateOf(if(p.longitude.isFinite())p.longitude.toString() else "")}
 var category by remember(p){mutableStateOf(p.category)}
 var notes by remember(p){mutableStateOf(p.notes)}
 var menu by remember{mutableStateOf(false)}
 val a=lat.trim().toDoubleOrNull();val b=lon.trim().toDoubleOrNull()
 val valid=name.isNotBlank()&&a!=null&&b!=null&&validCoordinates(a,b)
 AlertDialog(onDismissRequest={if(!busy)cancel()},title={Text(if(p.id==0L)"New waypoint" else "Edit waypoint")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 OutlinedTextField(name,{name=it},label={Text("Name")},singleLine=true)
 OutlinedTextField(lat,{lat=it},label={Text("Latitude (DD)")},singleLine=true)
 OutlinedTextField(lon,{lon=it},label={Text("Longitude (DD)")},singleLine=true)
 Text("Latitude -90 to 90; longitude -180 to 180")
 Box{OutlinedButton(onClick={menu=true}){Text(category)};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){categories.forEach{c->DropdownMenuItem(text={Text(c)},onClick={category=c;menu=false})}}}
 OutlinedTextField(notes,{notes=it},label={Text("Notes")})
 p.accuracy?.let{Text("Captured GPS accuracy: ± "+fmtPoint(it.toDouble(),1)+" m")}
 }},confirmButton={TextButton(enabled=valid&&!busy,onClick={save(p.copy(name=name.trim(),latitude=a!!,longitude=b!!,category=category,notes=notes.trim(),accuracy=if(a==p.latitude&&b==p.longitude)p.accuracy else null))}){Text("Save")}},dismissButton={TextButton(onClick=cancel,enabled=!busy){Text("Cancel")}})
}
