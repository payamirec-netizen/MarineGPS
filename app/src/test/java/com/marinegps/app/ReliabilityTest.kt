package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class ReliabilityTest {
 @Test fun retryThrottle(){assertFalse(shouldReconnect(29999,0,0));assertTrue(shouldReconnect(30000,0,0));assertFalse(shouldReconnect(35000,0,10000))}
 @Test fun headingWrap(){assertEquals(15.6,relativeHeading(3.9,348.3),0.001);assertEquals(0.0,smoothHeading(359.0,1.0,0.5),0.001)}
}
