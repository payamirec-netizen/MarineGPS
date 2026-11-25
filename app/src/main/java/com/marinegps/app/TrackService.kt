package com.marinegps.app
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class TrackService:Service(),LocationListener {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private val events=Channel<suspend ()->Unit>(Channel.UNLIMITED)
 private lateinit var manager:LocationManager
 private lateinit var db:TrackDb
 private var track:Track?=null
 private var previous:Location?=null
 private var segment=0L
 private var lastTick=0L
 override fun onBind(intent:Intent?)=null
 override fun onCreate(){
 super.onCreate()
 db=TrackDb.get(this)
 manager=getSystemService(LocationManager::class.java)
 getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("tracks","Track recording",NotificationManager.IMPORTANCE_LOW))
 scope.launch { for(event in events){try{event()}catch(e:CancellationException){throw e}catch(e:Exception){android.util.Log.e("TrackService","Recording failed",e);stopSelf();break}} }
 }
 private fun notification(message:String):Notification {
 val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
 return Notification.Builder(this,"tracks").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Marine GPS").setContentText(message).setContentIntent(open).setOngoing(true).build()
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
 if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY}
 try{startForeground(50,notification("Track recording"))}catch(e:Exception){stopSelf();return START_NOT_STICKY}
 val action=intent?.action?:"restore"
 events.trySend {
 val dao=db.tracks()
 if(track==null)track=dao.active()
 when(action){
 "stop"->{track?.let{dao.update(it.copy(ended=System.currentTimeMillis(),state="saved"))};track=null;manager.removeUpdates(this);stopSelf()}
 "pause"->{track?.let{track=it.copy(state="paused");dao.update(track!!)};manager.removeUpdates(this);previous=null;getSystemService(NotificationManager::class.java).notify(50,notification("Recording paused"))}
 else->{
 if(track==null&&action=="start"){val t=Track(started=System.currentTimeMillis());track=t.copy(id=dao.insert(t))}
 if(track==null){stopSelf()}else if(action!="restore"||track!!.state=="recording"){
 track=track!!.copy(state="recording");dao.update(track!!)
 previous=null;segment=System.currentTimeMillis();lastTick=SystemClock.elapsedRealtime()
 manager.removeUpdates(this);manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,this)
 }else getSystemService(NotificationManager::class.java).notify(50,notification("Recording paused"))
 }
 }
 }
 return START_STICKY
 }
 override fun onLocationChanged(location:Location){
 val loc=Location(location)
 events.trySend event@{
 val t=track?:return@event
 if(t.state!="recording"||!loc.hasAccuracy()||loc.accuracy>30||!validCoordinates(loc.latitude,loc.longitude)||!freshFix(SystemClock.elapsedRealtime()-loc.elapsedRealtimeNanos/1000000))return@event
 val old=previous
 val seconds=old?.let{(loc.elapsedRealtimeNanos-it.elapsedRealtimeNanos)/1e9}?:0.0
 if(old!=null&&seconds<=0)return@event
 if(seconds>30){segment=System.currentTimeMillis();previous=null}
 val step=if(old==null)0.0 else trackStep(old.latitude,old.longitude,loc.latitude,loc.longitude,seconds,loc.accuracy.toDouble())
 val now=SystemClock.elapsedRealtime()
 val elapsed=(now-lastTick).coerceIn(0L,30000L)
 val speed=if(loc.hasSpeed()&&loc.speed in 0f..60f)loc.speed.toDouble() else 0.0
 val updated=t.copy(meters=t.meters+step,activeMillis=t.activeMillis+elapsed,maxSpeed=maxOf(t.maxSpeed,speed))
 db.withTransaction {
 if(previous==null||step>0){db.tracks().point(TrackPoint(trackId=t.id,time=loc.time,lat=loc.latitude,lon=loc.longitude,accuracy=loc.accuracy,segment=segment))}
 db.tracks().update(updated)
 }
 if(previous==null||step>0)previous=loc
 lastTick=now;track=updated
 }
 }
 override fun onDestroy(){manager.removeUpdates(this);events.close();scope.cancel();super.onDestroy()}
}
