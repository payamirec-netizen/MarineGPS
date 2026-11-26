package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class RouteXteTest{
 @Test fun onLine(){assertEquals(0.0,routeXte(RouteVertex(0.0,0.0),RouteVertex(0.0,1.0),RouteVertex(0.0,0.5)),0.01)}
 @Test fun offLine(){assertTrue(kotlin.math.abs(routeXte(RouteVertex(0.0,0.0),RouteVertex(0.0,1.0),RouteVertex(0.01,0.5)))>1000)}
}
