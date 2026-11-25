package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class HeadingTest {
 @Test fun cardinalDirections(){assertEquals(90.0,relativeHeading(90.0,0.0),0.001);assertEquals(-90.0,relativeHeading(0.0,90.0),0.001);assertEquals(0.0,relativeHeading(135.0,135.0),0.001)}
 @Test fun wrap(){assertEquals(2.0,relativeHeading(1.0,359.0),0.001);assertEquals(-2.0,relativeHeading(359.0,1.0),0.001)}
 @Test fun smoothing(){assertEquals(0.0,smoothHeading(359.0,1.0,0.5),0.001);assertEquals(0.0,smoothHeading(1.0,359.0,0.5),0.001)}
 @Test fun geographicCardinals(){assertEquals(0.0,targetMetrics(0.0,0.0,1.0,0.0).bearing!!,0.001);assertEquals(90.0,targetMetrics(0.0,0.0,0.0,1.0).bearing!!,0.001);assertEquals(270.0,targetMetrics(0.0,0.0,0.0,-1.0).bearing!!,0.001);assertEquals(180.0,targetMetrics(0.0,0.0,-1.0,0.0).bearing!!,0.001)}
 @Test fun dateline(){assertEquals(90.0,targetMetrics(0.0,179.9,0.0,-179.9).bearing!!,0.001)}
}
