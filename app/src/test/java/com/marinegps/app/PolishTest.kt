package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class PolishTest {
 @Test fun courseQuality(){assertNull(courseReason(2.0,90.0,10.0));assertNotNull(courseReason(0.1,90.0,10.0));assertNotNull(courseReason(2.0,null,null));assertNotNull(courseReason(2.0,90.0,31.0));assertNotNull(courseReason(2.0,Double.NaN,null))}
 @Test fun units(){assertEquals("36.0 km/h",speedLabel(10.0,"km/h"));assertEquals("34 m",distanceLabel(34.0,"NM"))}
 @Test fun scale(){assertEquals(20.0,niceScale(37.0),0.001);assertEquals(0.5,niceScale(0.8),0.001)}
}
