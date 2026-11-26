package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class RouteCoreTest {
 @Test fun duration(){assertEquals("00:00:59",routeDuration(59000))}
 @Test fun reversePreservesGaps(){val a=RouteVertex(1.0,2.0);val b=RouteVertex(2.0,3.0);assertEquals(listOf(listOf(b),listOf(a)),reverseTrackSegments(listOf(listOf(a),listOf(b))))}
 @Test fun remaining(){val a=RouteVertex(0.0,0.0);val b=RouteVertex(0.0,0.01);assertEquals(routeLength(listOf(a,b)),routeRemaining(a,listOf(b),0)!!,0.001);assertNull(routeRemaining(a,listOf(b),1))}
}
