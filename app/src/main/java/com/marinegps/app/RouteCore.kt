package com.marinegps.app
import kotlin.math.*
import java.util.Locale
internal fun routeDuration(ms:Long):String { val s=ms.coerceAtLeast(0)/1000;return String.format(Locale.ROOT,"%02d:%02d:%02d",s/3600,s/60%60,s%60) }
internal data class RouteVertex(val lat:Double,val lon:Double)
internal fun routeLength(points:List<RouteVertex>):Double=points.zipWithNext().sumOf{(a,b)->targetMetrics(a.lat,a.lon,b.lat,b.lon).meters}
internal fun reverseTrackSegments(segments:List<List<RouteVertex>>):List<List<RouteVertex>> = segments.asReversed().map{it.asReversed()}
internal fun routeRemaining(current:RouteVertex,points:List<RouteVertex>,index:Int):Double? {if(index !in points.indices)return null;val p=points[index];return targetMetrics(current.lat,current.lon,p.lat,p.lon).meters+routeLength(points.drop(index))}

internal fun encodeRoute(points:List<RouteVertex>)=points.joinToString(";"){"${it.lat},${it.lon}"}
internal fun decodeRoute(raw:String):List<RouteVertex> = raw.split(";").mapNotNull{val p=it.split(",");val a=p.getOrNull(0)?.toDoubleOrNull();val b=p.getOrNull(1)?.toDoubleOrNull();if(a!=null&&b!=null&&validCoordinates(a,b))RouteVertex(a,b)else null}
