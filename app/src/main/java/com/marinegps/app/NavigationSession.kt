package com.marinegps.app
import androidx.compose.runtime.*
internal data class NavSample(val point:RouteVertex,val time:Long)
class NavigationSession {
 internal var start by mutableStateOf<RouteVertex?>(null)
 internal val trail=mutableStateListOf<NavSample>()
 var zoom by mutableFloatStateOf(1f)
 var plot by mutableStateOf(false)
 internal fun add(point:RouteVertex,time:Long){
 if(!point.lat.isFinite()||!point.lon.isFinite()||point.lat !in -90.0..90.0||point.lon !in -180.0..180.0)return
 if(trail.lastOrNull()?.let{time<=it.time}==true)return
 if(start==null)start=point
 trail.add(NavSample(point,time))
 if(trail.size>10000)trail.removeAt(0)
 }
}
