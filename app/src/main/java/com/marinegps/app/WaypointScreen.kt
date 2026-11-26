package com.marinegps.app
import android.os.SystemClock
import androidx.room.withTransaction
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
internal fun currentFix(s:GnssState)=s.location?.takeIf{freshFix(SystemClock.elapsedRealtime()-it.elapsedRealtimeNanos/1000000L)&&validCoordinates(it.latitude,it.longitude)}
internal fun fmtPoint(v:Double,n:Int)=String.format(Locale.US,"%."+n+"f",v)
@Composable fun WaypointScreen(s:GnssState,unit:String,format:String,importData:()->Unit,navigate:(Waypoint)->Unit){
 val context=LocalContext.current
 val dao=remember{WaypointDb.get(context).points()}
 val flow=remember(dao){dao.observe()}
 val points by flow.collectAsState(initial=emptyList())
 val scope=rememberCoroutineScope()
 var selection by remember{mutableStateOf(setOf<Long>())}
 var bulkDelete by remember{mutableStateOf(false)}
 val picked=points.filter{it.id in selection}
 var categoryFilter by remember{mutableStateOf("")}
 var categoryMenu by remember{mutableStateOf(false)}
 var search by remember{mutableStateOf("")}
 var edit by remember{mutableStateOf<Waypoint?>(null)}
 var remove by remember{mutableStateOf<Waypoint?>(null)}
 var error by remember{mutableStateOf("")}
 var busy by remember{mutableStateOf(false)}
 val fix=currentFix(s)
 fun create(gps:Boolean){
  val loc=if(gps)currentFix(s) else null
  if(gps&&loc==null){error="No fresh GPS fix";return}
  edit=Waypoint(name=if(gps)"Fishing "+java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(java.util.Date()) else "",latitude=loc?.latitude?:Double.NaN,longitude=loc?.longitude?:Double.NaN,category="Fishing",notes="",createdAt=System.currentTimeMillis(),accuracy=loc?.takeIf{it.hasAccuracy()}?.accuracy)
 }
 LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={create(true)},enabled=fix!=null&&!busy,modifier=Modifier.weight(1f)){Text(tr("Save GPS"))};OutlinedButton(onClick={create(false)},enabled=!busy,modifier=Modifier.weight(1f)){Text(tr("Add manually"))}}}
 if(fix==null)item{Text(tr("Waiting for a fresh GPS fix"),color=MaterialTheme.colorScheme.error)}
 if(error.isNotEmpty())item{Text(tr(error),color=MaterialTheme.colorScheme.error)}
 item{OutlinedTextField(search,{search=it},label={Text(tr("Search waypoints"))},singleLine=true,modifier=Modifier.fillMaxWidth())}
 item{TextButton(onClick=importData){Text(tr("Import locations"))};Box{OutlinedButton(onClick={categoryMenu=true}){Text(tr(categoryFilter.ifBlank{"All categories"}))};DropdownMenu(categoryMenu,{categoryMenu=false}){(listOf("")+categories).forEach{v->DropdownMenuItem(text={Text(tr(v.ifBlank{"All categories"}))},onClick={categoryFilter=v;categoryMenu=false})}}}}
 item{Row{TextButton(onClick={selection=points.map{it.id}.toSet()}){Text(tr("Select all"))};TextButton(onClick={selection=emptySet()}){Text(tr("Clear selection"))}}}
 if(picked.isNotEmpty())item{SelectionActions(TransferData(picked.map{TransferPoint(it.name,it.latitude,it.longitude,it.notes,it.createdAt,it.category,it.accuracy)},emptyList()),{bulkDelete=true})}
 item{Text("${points.size} saved waypoints")}
 val filtered=points.filter{(categoryFilter.isEmpty()||it.category==categoryFilter)&&(it.name.contains(search,true)||it.category.contains(search,true)||it.notes.contains(search,true))}
 if(filtered.isEmpty())item{Text(tr("No matching waypoints"))}
 items(filtered,key={it.id}){p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
 Row{Checkbox(p.id in selection,{checked->selection=if(checked)selection+p.id else selection-p.id});Text(p.name,style=MaterialTheme.typography.titleLarge,color=if(p.category=="Hazard")MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)}
 Text((if(p.category=="Hazard")"⚠ " else "")+tr(p.category)+" · "+coordinate(p.latitude,true,format)+" / "+coordinate(p.longitude,false,format))
 p.accuracy?.let{Text(tr("Saved GPS accuracy: ± ")+fmtPoint(it.toDouble(),1)+" m")}
 if(p.notes.isNotBlank())Text(p.notes)
 if(fix!=null){val m=targetMetrics(fix.latitude,fix.longitude,p.latitude,p.longitude);val reliable=usableBearing(m,fix.takeIf{it.hasAccuracy()}?.accuracy,p.accuracy);Text(distanceLabel(m.meters,unit)+" · Bearing true: "+(reliable?.let{fmtPoint(it,1)+"°"}?:"Uncertain"))}else Text(tr("Distance / bearing unavailable"))
 Row{TextButton(onClick={navigate(p)},enabled=!busy){Text(tr("Navigate"))};TextButton(onClick={edit=p},enabled=!busy){Text(tr("Edit"))};TextButton(onClick={remove=p},enabled=!busy){Text(tr("Delete"))}}
 }}}
 }
 if(bulkDelete)AlertDialog(onDismissRequest={bulkDelete=false},title={Text(tr("Confirm deletion"))},text={Text(picked.size.toString())},confirmButton={TextButton(onClick={bulkDelete=false;scope.launch{WaypointDb.get(context).withTransaction{picked.forEach{dao.delete(it)}};selection=emptySet()}}){Text(tr("Delete"))}},dismissButton={TextButton(onClick={bulkDelete=false}){Text(tr("Cancel"))}})
 edit?.let{p->WaypointEditor(p,{edit=null},{updated->busy=true;scope.launch{try{if(updated.id==0L)dao.insert(updated)else dao.update(updated);edit=null;error=""}catch(e:Exception){error="Unable to save waypoint"}finally{busy=false}}},busy)}
 remove?.let{p->AlertDialog(onDismissRequest={if(!busy)remove=null},title={Text(tr("Delete waypoint?"))},text={Text(p.name)},confirmButton={TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{dao.delete(p);remove=null;error=""}catch(e:Exception){error="Unable to delete waypoint"}finally{busy=false}}}){Text(tr("Delete"))}},dismissButton={TextButton(onClick={remove=null},enabled=!busy){Text(tr("Cancel"))}})}
}
@Composable private fun WaypointEditor(p:Waypoint,cancel:()->Unit,save:(Waypoint)->Unit,busy:Boolean){
 var name by remember(p){mutableStateOf(p.name)}
 var lat by remember(p){mutableStateOf(if(p.latitude.isFinite())fmtPoint(p.latitude,6) else "")}
 var lon by remember(p){mutableStateOf(if(p.longitude.isFinite())fmtPoint(p.longitude,6) else "")}
 var category by remember(p){mutableStateOf(p.category)}
 var notes by remember(p){mutableStateOf(p.notes)}
 var menu by remember{mutableStateOf(false)}
 val a=if(p.latitude.isFinite()&&lat==fmtPoint(p.latitude,6))p.latitude else lat.trim().toDoubleOrNull();val b=if(p.longitude.isFinite()&&lon==fmtPoint(p.longitude,6))p.longitude else lon.trim().toDoubleOrNull()
 val valid=name.isNotBlank()&&a!=null&&b!=null&&validCoordinates(a,b)
 AlertDialog(onDismissRequest={if(!busy)cancel()},title={Text(tr(if(p.id==0L)"New waypoint" else "Edit waypoint"))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 OutlinedTextField(name,{name=it},label={Text(tr("Name"))},singleLine=true)
 OutlinedTextField(lat,{lat=it},label={Text(tr("Latitude (DD)"))},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal))
 OutlinedTextField(lon,{lon=it},label={Text(tr("Longitude (DD)"))},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal))
 Text(tr("Latitude -90 to 90; longitude -180 to 180"))
 Box{OutlinedButton(onClick={menu=true}){Text(tr(category)+" ▾")};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){categories.forEach{c->DropdownMenuItem(text={Text(tr(c))},onClick={category=c;menu=false})}}}
 OutlinedTextField(notes,{notes=it},label={Text(tr("Notes"))})
 p.accuracy?.let{Text(tr("Captured GPS accuracy: ± ")+fmtPoint(it.toDouble(),1)+" m")}
 }},confirmButton={TextButton(enabled=valid&&!busy,onClick={save(p.copy(name=name.trim(),latitude=a!!,longitude=b!!,category=category,notes=notes.trim(),accuracy=if(a==p.latitude&&b==p.longitude)p.accuracy else null))}){Text(tr("Save"))}},dismissButton={TextButton(onClick=cancel,enabled=!busy){Text(tr("Cancel"))}})
}
