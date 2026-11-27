package com.marinegps.app
import java.time.*
import kotlin.math.*
internal fun sunTimes(date:LocalDate,lat:Double,lon:Double):List<Instant>?{
 if(!validCoordinates(lat,lon))return null
 val g=2*PI/365*(date.dayOfYear-1)
 val eq=229.18*(0.000075+0.001868*cos(g)-0.032077*sin(g)-0.014615*cos(2*g)-0.040849*sin(2*g))
 val dec=0.006918-0.399912*cos(g)+0.070257*sin(g)-0.006758*cos(2*g)+0.000907*sin(2*g)-0.002697*cos(3*g)+0.00148*sin(3*g)
 val phi=Math.toRadians(lat)
 val h=(cos(Math.toRadians(90.833))/(cos(phi)*cos(dec))-tan(phi)*tan(dec))
 if(!h.isFinite()||h !in -1.0..1.0)return null
 val noon=720-4*lon-eq;val delta=4*Math.toDegrees(acos(h))
 val midnight=date.atStartOfDay(ZoneOffset.UTC).toInstant()
 return listOf(noon-delta,noon+delta).map{midnight.plusSeconds((it*60).roundToLong())}
}
