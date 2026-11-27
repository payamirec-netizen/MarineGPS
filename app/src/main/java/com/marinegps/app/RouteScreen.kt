package com.marinegps.app
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.*
internal fun routeXte(a:RouteVertex,b:RouteVertex,p:RouteVertex):Double {
 val ab=targetMetrics(a.lat,a.lon,b.lat,b.lon)
 val ap=targetMetrics(a.lat,a.lon,p.lat,p.lon)
 if(ab.meters<1||ab.bearing==null||ap.bearing==null)return 0.0
 return asin((sin(ap.meters/6371000.0)*sin(Math.toRadians(ap.bearing!!-ab.bearing!!))).coerceIn(-1.0,1.0))*6371000.0
}
@Composable internal fun RouteScreen(s:GnssState,unit:String,back:()->Unit){
 KeepNavigationScreenOn(true)
 val c=LocalContext.current;val scope=rememberCoroutineScope();val ar=LocalArabic.current
 val wd=remember{WaypointDb.get(c).points()};val td=remember{TrackDb.get(c).tracks()}
 val points by wd.observe().collectAsState(initial=emptyList());val tracks by td.observe().collectAsState(initial=emptyList())
 val prefs=remember{c.getSharedPreferences("marine_routes",0)}
 var library by remember{mutableStateOf(prefs.all.filterKeys{it.startsWith("saved:")}.mapNotNull{(k,v)->(v as? String)?.let{k.removePrefix("saved:") to it}}.toMap())}
 var routeName by remember{mutableStateOf("")}
 fun buzz(){runCatching{val v=c.getSystemService(android.os.Vibrator::class.java);v?.vibrate(android.os.VibrationEffect.createOneShot(180,android.os.VibrationEffect.DEFAULT_AMPLITUDE))}}
 var route by remember{mutableStateOf(decodeRoute(prefs.getString("route","")?:""))}
 var pending by remember{mutableStateOf<List<List<RouteVertex>>>(emptyList())}
 var alert by remember{mutableStateOf(false)}
 val tone=remember{runCatching{android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION,70)}.getOrNull()}
 DisposableEffect(tone){onDispose{tone?.release()}}
 var index by remember{mutableIntStateOf(0)};var running by remember{mutableStateOf(false)}
 var notice by remember{mutableStateOf("")};var gaps by remember{mutableStateOf(false)}
 val fix=s.location?.takeIf{freshFix(s.now-it.elapsedRealtimeNanos/1000000)&&it.hasAccuracy()&&it.accuracy<=30f}
 val current=fix?.let{RouteVertex(it.latitude,it.longitude)}
 val target=route.getOrNull(index)
 val plotSession=remember(target,running){NavigationSession()}
 LaunchedEffect(fix?.elapsedRealtimeNanos,running,target){if(running&&fix!=null)plotSession.add(RouteVertex(fix.latitude,fix.longitude),fix.elapsedRealtimeNanos/1000000)}
 val metrics=if(current!=null&&target!=null)targetMetrics(current.lat,current.lon,target.lat,target.lon)else null
 val deviation=if(index>0&&current!=null&&target!=null)abs(routeXte(route[index-1],target,current))else 0.0
 LaunchedEffect(deviation,running){if(running&&deviation>50&&!alert){alert=true;tone?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2,250);buzz()}else if(!running||deviation<30)alert=false}
 LaunchedEffect(fix?.elapsedRealtimeNanos,running,index){if(running&&metrics!=null&&fix!=null&&metrics.meters<=maxOf(15.0,fix.accuracy.toDouble()*2)){tone?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,180);buzz();if(index==route.lastIndex){running=false;notice=if(ar)"قرب نهاية المسار، تحقق بصرياً" else "Near route end; verify visually"}else{index++;notice=if(ar)"قرب النقطة السابقة، انتقل للتي تليها" else "Near waypoint; next leg"}}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(2.dp)){
 item{TextButton(onClick=back){Text(if(ar)"رجوع" else "Back")};Text(if(ar)"المسارات الملاحية" else "Routes",style=MaterialTheme.typography.titleLarge)}
 item{Text((if(ar)"عدد النقاط: " else "Points: ")+route.size);Row{TextButton(enabled=!running,onClick={route=route.asReversed();index=0;pending=emptyList();gaps=false}){Text(if(ar)"عكس" else "Reverse")};TextButton(enabled=!running,onClick={route=emptyList();index=0;gaps=false;pending=emptyList()}){Text(if(ar)"مسح" else "Clear")};Button(enabled=route.isNotEmpty(),onClick={if(running)running=false else{index=0;running=true}}){Text(if(running){if(ar)"إيقاف" else "Stop"}else{if(ar)"بدء" else "Start"})}}}
 if(gaps)item{Text(if(ar)"المسار المسجل متعدد المقاطع: لا يتم وصل الفجوات تلقائياً." else "Recorded track has gaps; segments are not joined.",color=MaterialTheme.colorScheme.error)}
 if(!running&&pending.isNotEmpty())item{Button(onClick={route=pending.first();pending=pending.drop(1);index=0;notice="";running=true}){Text(if(ar)"بدء المقطع التالي بعد التحقق من الطريق" else "Start next segment after verifying safe passage")}}
 if(!running)item{OutlinedTextField(routeName,{routeName=it.take(80)},label={Text(if(ar)"اسم المسار" else "Route name")});Row{TextButton(enabled=route.isNotEmpty()&&!gaps,onClick={val name=routeName.trim().ifBlank{"Route "+System.currentTimeMillis()};val encoded=encodeRoute(route);prefs.edit().putString("route",encoded).putString("saved:"+name,encoded).apply();library=library+(name to encoded);notice=if(ar)"تم حفظ المسار" else "Route saved"}){Text(if(ar)"حفظ" else "Save")};TextButton(enabled=route.isNotEmpty(),onClick={val data=TransferData(emptyList(),listOf(TransferTrack("Route",listOf(route.map{TransferPoint("",it.lat,it.lon,"")}))));val intent=android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,gpx(data));c.startActivity(android.content.Intent.createChooser(intent,null))}){Text(if(ar)"مشاركة GPX" else "Share GPX")}}}
 if(running||notice.isNotEmpty())item{if(alert)Text(if(ar)"انحراف عن المسار" else "Off route",color=MaterialTheme.colorScheme.error);Text(notice);Text((if(ar)"النقطة " else "Waypoint ")+(index+1)+"/"+route.size);Text((if(ar)"المسافة: " else "Distance: ")+ltrValue(metrics?.let{distanceLabel(it.meters,unit)}?:"—"));Text((if(ar)"الاتجاه: " else "Bearing: ")+ltrValue(metrics?.bearing?.let{String.format(java.util.Locale.US,"%.1f°",it)}?:"—"));Text((if(ar)"المتبقي: " else "Remaining: ")+(current?.let{routeRemaining(it,route,index)}?.let{ltrValue(distanceLabel(it,unit))}?:"—"));if(index>0&&current!=null&&target!=null)Text((if(ar)"الانحراف الجانبي: " else "XTE: ")+ltrValue(distanceLabel(abs(routeXte(route[index-1],target,current)),unit)))}
 if(running&&target!=null)item{RouteBearingDial(metrics?.bearing?.takeIf{metrics.meters>maxOf(15.0,fix?.accuracy?.toDouble()?:30.0)},ar);NavigationPlot(fix,Waypoint(name="Route",latitude=target.lat,longitude=target.lon,category="Other",notes="",createdAt=0),0.0,unit,plotSession)}
 if(!running){item{Text(if(ar)"المسارات المحفوظة" else "Saved routes")};items(library.entries.sortedBy{it.key}){entry->Row{TextButton(onClick={route=decodeRoute(entry.value);routeName=entry.key;index=0;pending=emptyList();gaps=false;notice=""}){Text(entry.key)};TextButton(onClick={prefs.edit().remove("saved:"+entry.key).apply();library=library-entry.key}){Text(if(ar)"حذف" else "Delete")}}};item{Text(if(ar)"أضف المواقع بالترتيب المطلوب" else "Add waypoints in order")};items(points){p->TextButton(onClick={route=route+RouteVertex(p.latitude,p.longitude);pending=emptyList();gaps=false}){Text("+ "+p.name)}};item{Text(if(ar)"العودة عبر المسار" else "TrackBack")};items(tracks.filter{it.ended!=null}){t->TextButton(onClick={scope.launch{val seg=td.points(t.id).first().groupBy{it.segment}.values.toList();val reversed=reverseTrackSegments(seg.map{part->part.map{RouteVertex(it.lat,it.lon)}}).filter{it.isNotEmpty()};gaps=reversed.size>1;route=reversed.firstOrNull()?:emptyList();pending=reversed.drop(1);index=0;notice=""}}){Text(t.name.ifBlank{localDate(t.started)})}}}
 item{Text(if(ar)"رسم ملاحي مساعد فقط، لا يكشف العوائق." else "Navigation aid only; does not detect hazards.")}
 }
}
