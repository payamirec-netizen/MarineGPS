package com.marinegps.app

import android.app.Activity
import android.hardware.*
import android.location.Location
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.*

internal fun normalizeHeading(a:Double)=((a%360)+360)%360
internal fun relativeHeading(bearing:Double,heading:Double)=normalizeHeading(bearing-heading+180)-180
internal fun smoothHeading(previous:Double?,next:Double,alpha:Double=0.25)=normalizeHeading(if(previous==null)next else previous+alpha*relativeHeading(next,previous))
internal data class HeadingReading(val magnetic:Double,val timestamp:Long,val accuracy:Int)

internal data class HeadingInfo(val heading:Double?,val status:String)
@Composable internal fun rememberTrueHeading(fix:Location?,enabled:Boolean):Double? = rememberHeadingInfo(fix,enabled).heading
@Composable internal fun rememberHeadingInfo(fix:Location?,enabled:Boolean):HeadingInfo {
 val context=LocalContext.current
 val owner=LocalLifecycleOwner.current
 val manager=remember(context){context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager}
 var reading by remember{mutableStateOf<HeadingReading?>(null)}
 var status by remember{mutableStateOf("Waiting for compass")}
 var now by remember{mutableLongStateOf(SystemClock.elapsedRealtime())}
 LaunchedEffect(enabled){while(enabled){now=SystemClock.elapsedRealtime();delay(250)}}
 DisposableEffect(manager,owner,enabled){
  val rotation=manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  var previous:Double?=null
  var quality=SensorManager.SENSOR_STATUS_UNRELIABLE
  val matrix=FloatArray(9);val remapped=FloatArray(9);val angles=FloatArray(3)
  val listener=object:SensorEventListener{
   override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){quality=accuracy;if(accuracy<SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM){reading=null;previous=null;status="Low accuracy — calibrate away from metal"}}
   override fun onSensorChanged(event:SensorEvent){
    quality=event.accuracy
    if(quality<SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM||(event.values.size>=5&&event.values[4]>=0&&event.values[4]>Math.toRadians(25.0))){reading=null;previous=null;status="Low accuracy or excessive tilt";return}
    if(event.values.size<3||event.values.any{!it.isFinite()}){reading=null;previous=null;status="Invalid sensor reading";return}
    SensorManager.getRotationMatrixFromVector(matrix,event.values)
    val display=(context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
    val axes=when(display){Surface.ROTATION_90->SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X;Surface.ROTATION_180->SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y;Surface.ROTATION_270->SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X;else->SensorManager.AXIS_X to SensorManager.AXIS_Y}
    if(!SensorManager.remapCoordinateSystem(matrix,axes.first,axes.second,remapped)){reading=null;status="Orientation unavailable";return}
    SensorManager.getOrientation(remapped,angles)
    if(abs(cos(angles[1].toDouble()))<0.25){reading=null;previous=null;status="Hold phone flatter";return}
    val raw=Math.toDegrees(angles[0].toDouble())
    if(!raw.isFinite()){reading=null;status="Invalid sensor reading";return}
    previous=smoothHeading(previous,raw,0.5)
    status=if(quality==SensorManager.SENSOR_STATUS_ACCURACY_HIGH)"High" else "Medium"
    reading=HeadingReading(previous!!,event.timestamp/1000000L,quality)
   }
  }
  fun start(){if(enabled){status=when{rotation==null->"Rotation sensor unavailable";manager.registerListener(listener,rotation,SensorManager.SENSOR_DELAY_GAME)->"Waiting for compass";else->"Sensor registration failed"}}}
  fun stop(){manager.unregisterListener(listener);reading=null;previous=null}
  val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->start();Lifecycle.Event.ON_PAUSE->stop();else->Unit}}
  owner.lifecycle.addObserver(observer)
  onDispose{owner.lifecycle.removeObserver(observer);stop()}
 }
 val declination=remember(fix?.latitude,fix?.longitude,fix?.altitude){fix?.let{GeomagneticField(it.latitude.toFloat(),it.longitude.toFloat(),it.altitude.toFloat(),System.currentTimeMillis()).declination.toDouble()}}
 val valid=reading?.takeIf{enabled&&SystemClock.elapsedRealtime()-it.timestamp in 0..1500&&it.accuracy>=SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM}
 val age=now
 return HeadingInfo(valid?.let{v->declination?.let{normalizeHeading(v.magnetic+it)}},if(!enabled)"North-up" else if(valid==null&&reading!=null&&age-reading!!.timestamp>1500)"Stale compass" else if(valid==null)status else if(declination==null)"Waiting for GPS" else status)
}
@Composable internal fun KeepNavigationScreenOn(enabled:Boolean){
 val view=androidx.compose.ui.platform.LocalView.current
 DisposableEffect(view,enabled){val old=view.keepScreenOn;if(enabled)view.keepScreenOn=true;onDispose{view.keepScreenOn=old}}
}
