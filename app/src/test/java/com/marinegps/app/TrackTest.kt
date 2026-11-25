package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class TrackTest {
 @Test fun distance(){assertEquals(111.2,trackStep(0.0,0.0,0.001,0.0,10.0,5.0),0.5)}
 @Test fun jitter(){assertEquals(0.0,trackStep(0.0,0.0,0.000001,0.0,1.0,5.0),0.0)}
 @Test fun badAccuracy(){assertEquals(0.0,trackStep(0.0,0.0,0.001,0.0,10.0,50.0),0.0)}
 @Test fun gap(){assertEquals(0.0,trackStep(0.0,0.0,0.001,0.0,31.0,5.0),0.0)}
 @Test fun jump(){assertEquals(0.0,trackStep(0.0,0.0,1.0,0.0,1.0,5.0),0.0)}
}
