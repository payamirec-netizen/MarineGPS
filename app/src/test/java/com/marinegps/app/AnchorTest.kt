package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class AnchorTest{@Test fun status(){assertEquals("GPS unavailable",anchorStatus(null,50));assertEquals("Outside radius",anchorStatus(50.0,50));assertEquals("Monitoring",anchorStatus(49.0,50))}}
