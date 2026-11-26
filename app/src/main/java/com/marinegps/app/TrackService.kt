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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class RecordingHealth(val running:Boolean=false,val lastFix:Long=0,val accuracy:Float?=null,val gaps:Int=0,val gapMillis:Long=0,val waitingSince:Long=0,val message:String="Service inactive")
internal object TrackHealth { val mutable=MutableStateFlow(RecordingHealth());val state=mutable.asStateFlow() }
internal fun shouldReconnect(now:Long,last:Long,retry:Long)=now-last>=30000L&&now-retry>=30000L

class TrackService:Service(),LocationListener {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private val events=Channel<suspend ()->Unit>(Channel.UNLIMITED)
 private lateinit var manager:LocationManager
 private lateinit var db:TrackDb
 private var track:Track?=null
 private var previous:Location?=null
 private var segment=0L
 private var lastTick=0L
 private var lastFixNanos=0L
 private var listening=false
 private var wake:PowerManager.WakeLock?=null
 private var lastRetry=0L
 private var gapStart=0L
 private var lastAccepted=0L
 private var generation=0L
 private fun releaseWake(){wake?.let{if(it.isHeld)it.release()};wake=null}
 private fun holdWake(){if(wake?.isHeld==true)return;wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MarineGPS:Track").apply{setReferenceCounted(false);acquire()}}
 private fun connect(){manager.removeUpdates(this);listening=false;lastRetry=SystemClock.elapsedRealtime();try{manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,this,Looper.getMainLooper());listening=true}catch(e:Exception){TrackHealth.mutable.value=TrackHealth.mutable.value.copy(message="GPS reconnect failed: "+e.javaClass.simpleName)}}
 private suspend fun checkpoint(){val t=track?:return;if(t.state!="recording")return;val now=SystemClock.elapsedRealtime();val updated=t.copy(activeMillis=t.activeMillis+(now-lastTick).coerceAtLeast(0));db.tracks().update(updated);track=updated;lastTick=now}
 private suspend fun monitor(){if(track?.state!="recording")return;checkpoint();val now=SystemClock.elapsedRealtime();val health=TrackHealth.mutable.value;val last=lastAccepted;val lost=now-last>10000L;if(lost&&gapStart==0L){gapStart=last;TrackHealth.mutable.value=health.copy(gaps=health.gaps+1,waitingSince=gapStart,message="Waiting for usable GPS")};if(shouldReconnect(now,last,lastRetry))connect();if(lost)getSystemService(NotificationManager::class.java).notify(50,notification("Recording — waiting for GPS"))}
 override fun onBind(intent:Intent?)=null
 override fun onCreate(){
 super.onCreate()
 db=TrackDb.get(this)
 manager=getSystemService(LocationManager::class.java)
 getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("tracks","Track recording",NotificationManager.IMPORTANCE_LOW))
 scope.launch { while(isActive){delay(5000);events.send{monitor()}} }
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
 events.trySend command@{
 val dao=db.tracks()
 if(track==null)track=dao.active()
 when(action){
 "stop"->{track?.let{dao.update(it.copy(ended=System.currentTimeMillis(),state="saved",activeMillis=it.activeMillis+pendingTime()))};track=null;generation++;manager.removeUpdates(this);listening=false;releaseWake();TrackHealth.mutable.value=RecordingHealth();stopSelf()}
 "pause"->{track?.let{track=it.copy(state="paused",activeMillis=it.activeMillis+pendingTime());dao.update(track!!)};generation++;manager.removeUpdates(this);listening=false;previous=null;releaseWake();TrackHealth.mutable.value=TrackHealth.mutable.value.copy(running=false,message="Paused",waitingSince=0);getSystemService(NotificationManager::class.java).notify(50,notification("Recording paused"))}
 else->{
 if(track==null&&action=="start"){val t=Track(started=System.currentTimeMillis());track=t.copy(id=dao.insert(t))}
 if(track==null){stopSelf()}else if(action!="restore"||track!!.state=="recording"){
 if(track!!.state=="recording"&&listening){if(action=="reconnect")connect();return@command}
 track=track!!.copy(state="recording");dao.update(track!!)
 generation++;previous=null;lastFixNanos=0L;segment=System.currentTimeMillis();lastTick=SystemClock.elapsedRealtime();lastAccepted=lastTick;gapStart=0L
 TrackHealth.mutable.value=RecordingHealth(running=true,waitingSince=lastTick,message="Acquiring GPS");holdWake();connect()
 }else getSystemService(NotificationManager::class.java).notify(50,notification("Recording paused"))
 }
 }
 }
 return START_STICKY
 }
 private fun pendingTime():Long=if(track?.state=="recording")(SystemClock.elapsedRealtime()-lastTick).coerceAtLeast(0L) else 0L
 override fun onLocationChanged(location:Location){
 val loc=Location(location);val epoch=generation
 events.trySend event@{
 val t=track?:return@event
 if(epoch!=generation)return@event
 if(t.state!="recording"||!loc.hasAccuracy()||loc.accuracy>30||!validCoordinates(loc.latitude,loc.longitude)||!freshFix(SystemClock.elapsedRealtime()-loc.elapsedRealtimeNanos/1000000))return@event
 if(loc.elapsedRealtimeNanos<=lastFixNanos)return@event
 val gap=lastFixNanos!=0L&&loc.elapsedRealtimeNanos-lastFixNanos>30_000_000_000L
 lastFixNanos=loc.elapsedRealtimeNanos
 lastAccepted=SystemClock.elapsedRealtime();val health=TrackHealth.mutable.value
 TrackHealth.mutable.value=health.copy(running=true,lastFix=loc.elapsedRealtimeNanos/1000000L,accuracy=loc.accuracy,gapMillis=health.gapMillis+if(gapStart>0)lastAccepted-gapStart else 0L,waitingSince=0,message="GPS connected");if(gapStart>0){gapStart=0;getSystemService(NotificationManager::class.java).notify(50,notification("Track recording"))}
 if(gap){segment=System.currentTimeMillis();previous=null}
 val old=previous
 val seconds=old?.let{(loc.elapsedRealtimeNanos-it.elapsedRealtimeNanos)/1e9}?:0.0
 if(old!=null&&seconds<=0)return@event
 val step=if(old==null)0.0 else trackStep(old.latitude,old.longitude,loc.latitude,loc.longitude,seconds.coerceAtMost(30.0),loc.accuracy.toDouble())
 val now=SystemClock.elapsedRealtime()
 val elapsed=(now-lastTick).coerceAtLeast(0L)
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
 override fun onDestroy(){releaseWake();TrackHealth.mutable.value=RecordingHealth();manager.removeUpdates(this);events.close();scope.cancel();super.onDestroy()}
}
