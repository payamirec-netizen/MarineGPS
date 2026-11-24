package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class NavigationTest {
 @Test fun closeBearing(){assertNull(usableBearing(TargetMetrics(5.0,90.0),3f,3f))}
 @Test fun uncertain(){assertNull(usableBearing(TargetMetrics(20.0,90.0),15f,10f))}
 @Test fun farBearing(){assertEquals(90.0,usableBearing(TargetMetrics(100.0,90.0),5f,5f)!!,0.01)}
 @Test fun missingAccuracy(){assertNull(usableBearing(TargetMetrics(100.0,90.0),null,null))}
 @Test fun eta(){assertEquals(50.0,etaSeconds(100.0,2.0,90.0,90.0)!!,0.01);assertNull(etaSeconds(100.0,2.0,270.0,90.0));assertNull(etaSeconds(100.0,0.1,90.0,90.0))}
 @Test fun shortDistance(){assertEquals("12 m",distanceLabel(12.0,"NM"))}
}
