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

 val fix=currentFix(s)
 val anchor by AnchorMonitor.state.collectAsState()
 var radius by remember{mutableIntStateOf(50)}
 var anchorError by remember{mutableStateOf(false)}
 var drift by remember{mutableStateOf<android.location.Location?>(null)}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Text(label("أدوات الصيد","Fishing tools"),style=MaterialTheme.typography.titleLarge)
 fix?.let{val date=java.time.LocalDate.now();val times=sunTimes(date,it.latitude,it.longitude);Text(label("الشروق والغروب (تقريبي)","Sunrise / sunset (approx.)"));val fmt=java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault());Text(label("الشروق: ","Sunrise: ")+(times?.get(0)?.let{fmt.format(it)}?:"—"));Text(label("الغروب: ","Sunset: ")+(times?.get(1)?.let{fmt.format(it)}?:"—"))}
 CatchLog(fix)
 Text(log)
 Text(label("قياس الانجراف","Drift measurement"))
 Button(onClick={fix?.let{drift=android.location.Location(it)}},enabled=fix!=null){Text(label("بدء / إعادة القياس","Start / reset measurement"))}
 if(drift!=null)TextButton(onClick={drift=null}){Text(label("إيقاف القياس","Stop measurement"))}
 drift?.let{p->if(fix==null)Text(label("انتظار GPS","Waiting for GPS")) else {val sec=(fix.elapsedRealtimeNanos-p.elapsedRealtimeNanos)/1e9;val m=targetMetrics(p.latitude,p.longitude,fix.latitude,fix.longitude);Text(label("المدة: ","Duration: ")+sec.coerceAtLeast(0.0).toInt()+" s");Text(label("الإزاحة: ","Displacement: ")+ltrValue(distanceLabel(m.meters,unit)));Text(label("دقة GPS: ","GPS accuracy: ")+ltrValue(fmtPoint(fix.accuracy.toDouble(),1)+" m"));if(sec>=10&&m.meters>p.accuracy+fix.accuracy)Text(ltrValue(speedLabel(m.meters/sec,"kn")+" • "+(m.bearing?.let{b->fmtPoint(b,1)+"°"}?:"—")))else Text(label("الحركة غير مؤكدة","Movement uncertain"))}}
 Text(label("تنبيه الرسو التجريبي — خدمة خلفية، لا تعتمد عليه وحده","Experimental background anchor alert — not a safety device"))
 Row{listOf(25,50,100).forEach{v->FilterChip((if(anchor.active)anchor.radius else radius)==v,{radius=v},enabled=!anchor.active,label={Text(ltrValue("$v m"))})}}
 Button(onClick={if(anchor.active)c.stopService(android.content.Intent(c,AnchorService::class.java)) else fix?.let{anchorError=false;runCatching{androidx.core.content.ContextCompat.startForegroundService(c,android.content.Intent(c,AnchorService::class.java).putExtra("lat",it.latitude).putExtra("lon",it.longitude).putExtra("radius",radius))}.onFailure{anchorError=true}}},enabled=anchor.active||fix!=null){Text(if(anchor.active)label("إيقاف","Stop") else label("تعيين نقطة الرسو","Set anchor"))}
 if(anchorError)Text(label("تعذر تشغيل مراقبة الرسو","Unable to start anchor monitoring"),color=MaterialTheme.colorScheme.error)
 if(!androidx.core.app.NotificationManagerCompat.from(c).areNotificationsEnabled())Text(label("الإشعارات معطلة؛ فعّلها من إعدادات التطبيق","Notifications disabled; enable them in app settings"),color=MaterialTheme.colorScheme.error)
 Text(when(anchor.status){"Monitoring"->label("مراقبة النطاق","Monitoring");"Outside radius"->label("خارج النطاق","Outside radius");"GPS unavailable"->label("GPS غير متاح","GPS unavailable");else->anchor.status})
 anchor.distance?.let{Text(ltrValue(distanceLabel(it,unit)));AnchorRange(it,anchor.radius)}
 OutlinedButton(onClick=back){Text(label("رجوع","Back"))}
 }
}
