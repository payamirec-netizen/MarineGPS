package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class PerformanceTest{@Test fun checkpointPolicy(){assertFalse(shouldPersistTrack(false,false,4999));assertTrue(shouldPersistTrack(false,false,5000));assertTrue(shouldPersistTrack(true,false,0));assertTrue(shouldPersistTrack(false,true,0))}}
