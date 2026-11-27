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
 var anchor by remember{mutableStateOf<RouteVertex?>(null)}
 var radius by remember{mutableIntStateOf(50)}
 var drift by remember{mutableStateOf<Pair<RouteVertex,Long>?>(null)}
 var warned by remember{mutableStateOf(false)}
 val exceeded=anchor?.let{a->fix?.let{targetMetrics(a.lat,a.lon,it.latitude,it.longitude).meters>radius+(if(it.hasAccuracy())it.accuracy else 20f)}}==true
 LaunchedEffect(exceeded){if(exceeded&&!warned){val tone=android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM,100);try{tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2,800);kotlinx.coroutines.delay(900)}finally{tone.release()};warned=true};if(!exceeded)warned=false}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Text(label("أدوات الصيد","Fishing tools"),style=MaterialTheme.typography.titleLarge)
 Text(label("سجل المصيد","Catch log"))
 OutlinedTextField(note,{note=it.take(500)},label={Text(label("تفاصيل المصيد","Catch details"))})
 Button(onClick={val entry=java.text.DateFormat.getDateTimeInstance().format(java.util.Date())+" • "+note;log=(entry+"\n"+log).take(100000);prefs.edit().putString("log",log).apply();note=""},enabled=note.isNotBlank()){Text(label("حفظ","Save"))}
 Text(log)
 Text(label("قياس الانجراف","Drift measurement"))
 Button(onClick={fix?.let{drift=RouteVertex(it.latitude,it.longitude) to it.elapsedRealtimeNanos}},enabled=fix!=null){Text(label("بدء القياس","Start measurement"))}
 drift?.let{(p,t)->fix?.let{val sec=(it.elapsedRealtimeNanos-t)/1e9;val m=targetMetrics(p.lat,p.lon,it.latitude,it.longitude);if(sec>0)Text(ltrValue(speedLabel(m.meters/sec,"kn")+" • "+(m.bearing?.let{b->fmtPoint(b,1)+"°"}?:"—")))}}
 Text(label("تنبيه الرسو التجريبي — يعمل في هذه الشاشة فقط","Experimental anchor alert — this screen only"))
 Row{listOf(25,50,100).forEach{v->FilterChip(radius==v,{radius=v},label={Text("$v m")})}}
 Button(onClick={if(anchor!=null)anchor=null else fix?.let{anchor=RouteVertex(it.latitude,it.longitude)}},enabled=anchor!=null||fix!=null){Text(if(anchor==null)label("تعيين نقطة الرسو","Set anchor") else label("إيقاف","Stop"))}
 if(anchor!=null)Text(if(fix==null)label("GPS غير متاح","GPS unavailable") else if(exceeded)label("خارج النطاق","Outside radius") else label("مراقبة النطاق","Monitoring radius"))
 OutlinedButton(onClick=back){Text(label("رجوع","Back"))}
 }
}
