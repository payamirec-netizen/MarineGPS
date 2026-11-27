package com.marinegps.app
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable internal fun MarineTools(s:GnssState,unit:String,navigate:(Waypoint)->Unit,back:()->Unit){
 val c=LocalContext.current;val ar=LocalArabic.current;val scope=rememberCoroutineScope()
 fun label(a:String,e:String)=if(ar)a else e
 val file=remember{java.io.File(c.filesDir,"wrecks-ukho.geojson")}
 var rows by remember{mutableStateOf(emptyList<Wreck>())};var message by remember{mutableStateOf("")}
 LaunchedEffect(Unit){rows=withContext(Dispatchers.IO){runCatching{loadWreckCatalogue(c)}.getOrDefault(emptyList())}}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{runCatching{withContext(Dispatchers.IO){val bytes=c.contentResolver.openInputStream(uri)!!.use{readBounded(it)};require(bytes.size<=8000000);val raw=bytes.toString(Charsets.UTF_8);val parsed=parseWrecks(raw);require(parsed.isNotEmpty());val atomic=android.util.AtomicFile(file);val stream=atomic.startWrite();try{stream.write(bytes);atomic.finishWrite(stream)}catch(e:Exception){atomic.failWrite(stream);throw e};parsed}}.onSuccess{rows=it;message=""}.onFailure{message=label("ملف غير صالح","Invalid file")}}}
 var extra by remember{mutableStateOf(false)}
 if(extra){FishingTools(s,unit){extra=false};return}
 var radius by remember{mutableIntStateOf(50)};var query by remember{mutableStateOf("")}
 val fix=currentFix(s);val nearby=(if(fix==null)rows.map{it to Double.NaN} else nearbyWrecks(rows,fix.latitude,fix.longitude,radius)).filter{it.first.name.contains(query,true)||it.first.id.contains(query)}
 Column(Modifier.fillMaxSize()){
 TextButton(onClick={extra=true}){Text(label("أدوات الصيد والانجراف والرسو","Fishing, drift & anchor tools"))}
 Text(label("الحطام البحري","Marine wrecks"),style=MaterialTheme.typography.titleLarge)
 Text(label("سجلات تاريخية — ليست للملاحة الآمنة","Historical records — not for safe navigation"),color=MaterialTheme.colorScheme.error)
 Text(if(file.exists())label("بيانات مستوردة — تحقق من المصدر","Imported data — verify source") else "UKHO / EMODnet • Open Government Licence")
 TextButton(onClick={picker.launch(arrayOf("*/*"))}){Text(label("استيراد ملف UKHO GeoJSON","Import UKHO GeoJSON"))}
 Text(label("النتائج: ","Results: ")+nearby.size+" / "+rows.size)
 if(message.isNotBlank())Text(message)
 if(fix==null)Text(label("انتظار GPS","Waiting for GPS"))
 OutlinedTextField(query,{query=it},label={Text(label("بحث","Search"))},modifier=Modifier.fillMaxWidth())
 Row{listOf(10,25,50,100).forEach{v->FilterChip(radius==v,{radius=v},label={Text(ltrValue("$v km"))},modifier=Modifier.weight(1f))}}
 LazyColumn(Modifier.weight(1f)){items(nearby,key={it.first.id}){(w,d)->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.padding(10.dp)){
 Text(if(ar&&w.name.startsWith("Wreck "))"حطام "+w.id else w.name);Text(if(d.isFinite())ltrValue(distanceLabel(d,unit)) else label("المسافة غير متاحة","Distance unavailable"));Text(ltrValue("${w.lat}, ${w.lon}"))
 Text(label("أقل عمق: ","Least depth: ")+(w.depth?.let{"$it m"}?:label("غير معروف","Unknown")))
 Text(label("عمق الماء: ","Water depth: ")+(w.water?.let{"$it m"}?:label("غير معروف","Unknown")))
 Text(listOf(w.status,w.quality).filterNot{it.isBlank()||it.equals("n/a",true)||it.equals("unknown",true)}.joinToString(" • ").ifBlank{label("البيانات غير متوفرة","Data unavailable")})
 TextButton(onClick={navigate(Waypoint(name=w.name,latitude=w.lat,longitude=w.lon,category="Hazard",notes="Historical wreck; not verified",createdAt=System.currentTimeMillis()))}){Text(label("الاتجاه نحو الإحداثيات","Go to coordinates"))}
 TextButton(onClick={scope.launch{WaypointDb.get(c).points().insert(Waypoint(name=w.name,latitude=w.lat,longitude=w.lon,category="Hazard",notes="Historical wreck ${w.id}; source: "+(if(file.exists())"user import" else "UKHO/EMODnet")+"; not verified; not for navigation",createdAt=System.currentTimeMillis()));message=label("تم الحفظ كموقع خطر","Saved as hazard waypoint")}}){Text(label("حفظ الموقع","Save waypoint"))}
 }}}}
 var licence by remember{mutableStateOf(false)}
 TextButton(onClick={licence=true}){Text(label("المصدر والترخيص","Source & licence"))}
 if(licence)AlertDialog(onDismissRequest={licence=false},confirmButton={TextButton(onClick={licence=false}){Text("OK")}},text={Text(ltrValue("Contains public sector information licensed under the Open Government Licence v3.0. Source: UKHO via EMODnet."))})
 OutlinedButton(onClick=back,modifier=Modifier.fillMaxWidth()){Text(label("رجوع","Back"))}
 }
}
