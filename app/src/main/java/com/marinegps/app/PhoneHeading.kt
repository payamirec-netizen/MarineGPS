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

@Composable internal fun rememberTrueHeading(fix:Location?,enabled:Boolean):Double? {
 val context=LocalContext.current
 val owner=LocalLifecycleOwner.current
 val manager=remember(context){context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager}
 var reading by remember{mutableStateOf<HeadingReading?>(null)}
 var now by remember{mutableLongStateOf(SystemClock.elapsedRealtime())}
 LaunchedEffect(enabled){while(enabled){now=SystemClock.elapsedRealtime();delay(250)}}
 DisposableEffect(manager,owner,enabled){
  val rotation=manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  var previous:Double?=null
  var quality=SensorManager.SENSOR_STATUS_UNRELIABLE
  val listener=object:SensorEventListener{
   override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){quality=accuracy;if(accuracy<SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM){reading=null;previous=null}}
   override fun onSensorChanged(event:SensorEvent){
    quality=event.accuracy
    if(quality<SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM||(event.values.size>=5&&event.values[4]>=0&&event.values[4]>Math.toRadians(25.0))){reading=null;previous=null;return}
    val matrix=FloatArray(9);val remapped=FloatArray(9);val angles=FloatArray(3)
    SensorManager.getRotationMatrixFromVector(matrix,event.values)
    val display=(context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
    val axes=when(display){Surface.ROTATION_90->SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X;Surface.ROTATION_180->SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y;Surface.ROTATION_270->SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X;else->SensorManager.AXIS_X to SensorManager.AXIS_Y}
    if(!SensorManager.remapCoordinateSystem(matrix,axes.first,axes.second,remapped))return
    SensorManager.getOrientation(remapped,angles)
    if(abs(cos(angles[1].toDouble()))<0.25){reading=null;previous=null;return}
    val raw=Math.toDegrees(angles[0].toDouble())
    if(!raw.isFinite())return
    previous=smoothHeading(previous,raw)
    reading=HeadingReading(previous!!,SystemClock.elapsedRealtime(),quality)
   }
  }
  fun start(){if(enabled&&rotation!=null)manager.registerListener(listener,rotation,SensorManager.SENSOR_DELAY_GAME)}
  fun stop(){manager.unregisterListener(listener);reading=null;previous=null}
  val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->start();Lifecycle.Event.ON_PAUSE->stop();else->Unit}}
  owner.lifecycle.addObserver(observer)
  onDispose{owner.lifecycle.removeObserver(observer);stop()}
 }
 val declination=remember(fix?.latitude,fix?.longitude,fix?.altitude){fix?.let{GeomagneticField(it.latitude.toFloat(),it.longitude.toFloat(),it.altitude.toFloat(),System.currentTimeMillis()).declination.toDouble()}}
 return reading?.takeIf{enabled&&now-it.timestamp<=1500&&it.accuracy>=SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM}?.let{v->declination?.let{normalizeHeading(v.magnetic+it)}}
}
@Composable internal fun KeepNavigationScreenOn(enabled:Boolean){
 val view=androidx.compose.ui.platform.LocalView.current
 DisposableEffect(view,enabled){val old=view.keepScreenOn;if(enabled)view.keepScreenOn=true;onDispose{view.keepScreenOn=old}}
}
