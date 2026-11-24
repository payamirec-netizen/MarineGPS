package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class WaypointTest {
 @Test fun coordinates(){assertTrue(validCoordinates(90.0,-180.0));assertFalse(validCoordinates(91.0,0.0));assertFalse(validCoordinates(0.0,Double.NaN))}
 @Test fun freshness(){assertTrue(freshFix(5000));assertFalse(freshFix(5001));assertFalse(freshFix(-1))}
 @Test fun distance(){val m=targetMetrics(0.0,0.0,0.0,1.0);assertEquals(111195.0,m.meters,2.0);assertEquals(90.0,m.bearing!!,0.001)}
 @Test fun dateline(){val m=targetMetrics(0.0,179.0,0.0,-179.0);assertEquals(222390.0,m.meters,2.0);assertEquals(90.0,m.bearing!!,0.001)}
 @Test fun samePoint(){assertNull(targetMetrics(12.0,45.0,12.0,45.0).bearing)}
}
