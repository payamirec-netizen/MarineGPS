package com.marinegps.app
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.*
internal fun readBounded(input:java.io.InputStream):ByteArray{
 val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192)
 while(true){val n=input.read(buf);if(n<0)break;require(out.size()+n<=8000000);out.write(buf,0,n)}
 return out.toByteArray()
}
@Composable internal fun FishingTools(s:GnssState,unit:String,back:()->Unit){
 val c=LocalContext.current;val ar=LocalArabic.current
 fun label(a:String,e:String)=if(ar)a else e
 val prefs=remember{c.getSharedPreferences("marine_fishing",0)}
 var log by remember{mutableStateOf(prefs.getString("log","").orEmpty())}
 var note by remember{mutableStateOf("")}
 val fix=currentFix(s)
 val anchor by AnchorMonitor.state.collectAsState()
 var radius by remember{mutableIntStateOf(50)}
 var drift by remember{mutableStateOf<Pair<RouteVertex,Long>?>(null)}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Text(label("أدوات الصيد","Fishing tools"),style=MaterialTheme.typography.titleLarge)
 fix?.let{val date=java.time.LocalDate.now();val times=sunTimes(date,it.latitude,it.longitude);Text(label("الشروق والغروب (تقريبي)","Sunrise / sunset (approx.)"));val fmt=java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault());Text(label("الشروق: ","Sunrise: ")+(times?.get(0)?.let{fmt.format(it)}?:"—"));Text(label("الغروب: ","Sunset: ")+(times?.get(1)?.let{fmt.format(it)}?:"—"))}
 Text(label("سجل المصيد","Catch log"))
 OutlinedTextField(note,{note=it.take(500)},label={Text(label("تفاصيل المصيد","Catch details"))})
 Button(onClick={val entry=java.text.DateFormat.getDateTimeInstance().format(java.util.Date())+" • "+note;log=(entry+"\n"+log).take(100000);prefs.edit().putString("log",log).apply();note=""},enabled=note.isNotBlank()){Text(label("حفظ","Save"))}
 Text(log)
 Text(label("قياس الانجراف","Drift measurement"))
 Button(onClick={fix?.let{drift=RouteVertex(it.latitude,it.longitude) to it.elapsedRealtimeNanos}},enabled=fix!=null){Text(label("بدء / إعادة القياس","Start / reset measurement"))}
 if(drift!=null)TextButton(onClick={drift=null}){Text(label("إيقاف القياس","Stop measurement"))}
 drift?.let{(p,t)->fix?.let{val sec=(it.elapsedRealtimeNanos-t)/1e9;val m=targetMetrics(p.lat,p.lon,it.latitude,it.longitude);if(sec>0)Text(ltrValue(speedLabel(m.meters/sec,"kn")+" • "+(m.bearing?.let{b->fmtPoint(b,1)+"°"}?:"—")))}}
 Text(label("تنبيه الرسو التجريبي — خدمة خلفية، لا تعتمد عليه وحده","Experimental background anchor alert — not a safety device"))
 Row{listOf(25,50,100).forEach{v->FilterChip(radius==v,{radius=v},enabled=!anchor.active,label={Text(ltrValue("$v m"))})}}
 Button(onClick={if(anchor.active)c.stopService(android.content.Intent(c,AnchorService::class.java)) else fix?.let{runCatching{androidx.core.content.ContextCompat.startForegroundService(c,android.content.Intent(c,AnchorService::class.java).putExtra("lat",it.latitude).putExtra("lon",it.longitude).putExtra("radius",radius))}}},enabled=anchor.active||fix!=null){Text(if(anchor.active)label("إيقاف","Stop") else label("تعيين نقطة الرسو","Set anchor"))}
 Text(when(anchor.status){"Monitoring"->label("مراقبة النطاق","Monitoring");"Outside radius"->label("خارج النطاق","Outside radius");"GPS unavailable"->label("GPS غير متاح","GPS unavailable");else->anchor.status})
 anchor.distance?.let{Text(ltrValue(distanceLabel(it,unit)))}
 OutlinedButton(onClick=back){Text(label("رجوع","Back"))}
 }
}
