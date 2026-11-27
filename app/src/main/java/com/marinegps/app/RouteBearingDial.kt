package com.marinegps.app
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*
internal fun mergeLegacyCatch(old:String,incoming:String):String=(old.lineSequence()+incoming.lineSequence()).filter{it.isNotBlank()}.distinct().joinToString("\n")
@Composable internal fun RouteBearingDial(bearing:Double?,ar:Boolean){
 val ink=MaterialTheme.colorScheme.onSurface
 Text(if(ar)"اتجاه الوجهة — الشمال ثابت أعلى الرسم" else "Target bearing — north up")
 Text(ltrValue(bearing?.let{String.format(java.util.Locale.US,"%.1f°",it)}?: "—"))
 Canvas(Modifier.fillMaxWidth().height(140.dp)){
 val r=size.minDimension*.42f
 drawCircle(ink,r,style=Stroke(1.dp.toPx()))
 for(i in 0 until 12){val t=i*PI/6;val v=Offset(sin(t).toFloat(),-cos(t).toFloat());drawLine(ink,center+v*(r*.88f),center+v*r,2.dp.toPx())}
 drawCircle(ink,3.dp.toPx(),center)
 bearing?.let{val t=Math.toRadians(it);val v=Offset(sin(t).toFloat(),-cos(t).toFloat());val tip=center+v*r*.8f;val side=Offset(-v.y,v.x);drawLine(Color(0xff269b80),center,tip,4.dp.toPx());drawLine(Color(0xff269b80),tip,tip-v*14.dp.toPx()+side*7.dp.toPx(),3.dp.toPx());drawLine(Color(0xff269b80),tip,tip-v*14.dp.toPx()-side*7.dp.toPx(),3.dp.toPx())}
 }
}
