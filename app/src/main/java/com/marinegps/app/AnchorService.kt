package com.marinegps.app
import android.app.*
import android.content.*
import android.location.*
import android.os.*
import android.media.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
internal data class AnchorState(val active:Boolean=false,val distance:Double?=null,val status:String="Stopped",val radius:Int=50)
internal object AnchorMonitor{val state=MutableStateFlow(AnchorState())}
internal fun anchorStatus(distance:Double?,radius:Int)=if(distance==null||!distance.isFinite()||distance<0)"GPS unavailable" else if(distance>=radius)"Outside radius" else "Monitoring"
class AnchorService:Service(),LocationListener{
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private lateinit var manager:LocationManager
 private var wake:PowerManager.WakeLock?=null
 private var origin:RouteVertex?=null
 private var radius=50
 private var last:Location?=null
 private var job:Job?=null
 private var lastNotificationStatus:String?=null
 override fun onBind(intent:Intent?)=null
 override fun onCreate(){super.onCreate();manager=getSystemService(LocationManager::class.java);getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("anchor","Anchor monitoring",NotificationManager.IMPORTANCE_LOW))}
 private fun notification(status:String):Notification{
 val stop=PendingIntent.getService(this,91,Intent(this,AnchorService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE)
 val open=PendingIntent.getActivity(this,92,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
 return Notification.Builder(this,"anchor").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Marine GPS — Anchor").setContentText(status).setOngoing(true).setContentIntent(open).addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
 if(intent?.action=="stop"){stopSelf();return START_NOT_STICKY}
 if(origin!=null)return START_NOT_STICKY
 val lat=intent?.getDoubleExtra("lat",Double.NaN)?:Double.NaN;val lon=intent?.getDoubleExtra("lon",Double.NaN)?:Double.NaN
 if(!validCoordinates(lat,lon)){stopSelf();return START_NOT_STICKY}
 radius=intent?.getIntExtra("radius",50)?.coerceIn(25,100)?:50
 try{startForeground(91,notification("Acquiring GPS"));origin=RouteVertex(lat,lon);AnchorMonitor.state.value=AnchorState(true,null,"Acquiring GPS",radius);wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MarineGPS:Anchor").apply{acquire()};manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,this,Looper.getMainLooper())}catch(e:Exception){stopSelf();return START_NOT_STICKY}
 job=scope.launch{while(isActive){val a=origin?:break;val l=last?.takeIf{SystemClock.elapsedRealtime()-it.elapsedRealtimeNanos/1000000 in 0..10000};val d=l?.let{targetMetrics(a.lat,a.lon,it.latitude,it.longitude).meters};val status=anchorStatus(d,radius);AnchorMonitor.state.value=AnchorState(true,d,status,radius);if(lastNotificationStatus!=status){getSystemService(NotificationManager::class.java).notify(91,notification(status));lastNotificationStatus=status};if(status!="Monitoring"){try{val t=ToneGenerator(AudioManager.STREAM_ALARM,100);try{t.startTone(ToneGenerator.TONE_PROP_BEEP2,800);delay(900)}finally{t.release()}}catch(e:CancellationException){throw e}catch(e:Exception){};runCatching{getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE))}};delay(4000)}}
 return START_NOT_STICKY
 }
 override fun onLocationChanged(l:Location){if(l.hasAccuracy()&&l.accuracy in 0f..30f&&validCoordinates(l.latitude,l.longitude)&&freshFix(SystemClock.elapsedRealtime()-l.elapsedRealtimeNanos/1000000))last=Location(l)}
 override fun onDestroy(){scope.cancel();runCatching{manager.removeUpdates(this)};wake?.let{if(it.isHeld)it.release()};AnchorMonitor.state.value=AnchorState();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()}
}
