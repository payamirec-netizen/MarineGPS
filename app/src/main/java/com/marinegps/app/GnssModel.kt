package com.marinegps.app
import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Satellite(val system:Int,val id:Int,val cn0:Float,val used:Boolean,val azimuth:Float,val elevation:Float,val frequency:Float?)
data class GnssState(val location:Location?=null,val satellites:List<Satellite> = emptyList(),val ttff:Int?=null,val message:String="Waiting for GPS",val now:Long=SystemClock.elapsedRealtime())
class GnssModel(app:Application):AndroidViewModel(app){
 private val manager=app.getSystemService(LocationManager::class.java)
 private val handler=Handler(Looper.getMainLooper())
 private val mutable=MutableStateFlow(GnssState())
 val state=mutable.asStateFlow()
 private var running=false
 private var requestedAt=0L
 private var lastReceived=0L
 private var lastSatelliteUpdate=0L
 private val ticker=object:Runnable{override fun run(){val now=SystemClock.elapsedRealtime();mutable.update{it.copy(now=now)};if(running&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER)&&shouldReconnect(now,lastReceived,requestedAt)){requestedAt=now;runCatching{manager.removeUpdates(listener);manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,listener,Looper.getMainLooper())}.onFailure{mutable.update{it.copy(message="GPS reconnect failed")}}};if(running)handler.postDelayed(this,1000)}}
 private val listener=object:LocationListener{
 override fun onLocationChanged(location:Location){if(!running||!validCoordinates(location.latitude,location.longitude))return;val now=SystemClock.elapsedRealtime();if(!freshFix(now-location.elapsedRealtimeNanos/1000000))return;lastReceived=now;mutable.update{it.copy(location=Location(location),message="Location received",now=SystemClock.elapsedRealtime())}}
 override fun onProviderDisabled(provider:String){mutable.value=GnssState(message="Enable location in Android settings")}
 override fun onProviderEnabled(provider:String){mutable.update{it.copy(message="Acquiring GPS")}}
 @Deprecated("Legacy callback") override fun onStatusChanged(provider:String?,status:Int,extras:android.os.Bundle?){}
 }
 private val callback=object:GnssStatus.Callback(){
 override fun onFirstFix(ttffMillis:Int){if(!running)return;mutable.update{it.copy(ttff=ttffMillis)}}
 override fun onStopped(){if(!running)return;mutable.update{it.copy(satellites=emptyList(),location=null,message="Waiting for GPS")}}
 override fun onSatelliteStatusChanged(s:GnssStatus){if(!running)return;val now=SystemClock.elapsedRealtime();if(now-lastSatelliteUpdate<500)return;lastSatelliteUpdate=now;val rows=(0 until s.satelliteCount).map{i->Satellite(s.getConstellationType(i),s.getSvid(i),s.getCn0DbHz(i),s.usedInFix(i),s.getAzimuthDegrees(i),s.getElevationDegrees(i),if(s.hasCarrierFrequencyHz(i))s.getCarrierFrequencyHz(i) else null)};mutable.update{it.copy(satellites=rows)}}
 }
 fun start(){
 if(running)return
 if(getApplication<Application>().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){mutable.value=GnssState(message="Precise location permission required");return}
 requestedAt=SystemClock.elapsedRealtime();lastReceived=requestedAt;lastSatelliteUpdate=0L
 mutable.value=GnssState(message="Acquiring GPS")
 try{
 if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))mutable.update{it.copy(message="Enable location in Android settings")}
 manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,listener,Looper.getMainLooper())
 manager.registerGnssStatusCallback(callback,handler)
 running=true;handler.post(ticker)
 }catch(e:Exception){stop();mutable.value=GnssState(message="GPS unavailable: "+e.javaClass.simpleName)}
 }
 fun stop(){running=false;handler.removeCallbacks(ticker);runCatching{manager.removeUpdates(listener)};runCatching{manager.unregisterGnssStatusCallback(callback)}}
 override fun onCleared(){stop();super.onCleared()}
}
fun constellationName(type:Int)=when(type){1->"GPS";2->"SBAS";3->"GLONASS";4->"QZSS";5->"BeiDou";6->"Galileo";7->"NavIC";else->"Unknown"}
