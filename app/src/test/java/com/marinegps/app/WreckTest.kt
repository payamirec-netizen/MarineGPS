package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class WreckTest{
 @Test fun nearby(){val w=Wreck("1","A",12.0,45.0,null,null,"","");assertEquals(1,nearbyWrecks(listOf(w),12.0,45.0,10).size);assertTrue(nearbyWrecks(listOf(w),15.0,45.0,10).isEmpty())}
}
