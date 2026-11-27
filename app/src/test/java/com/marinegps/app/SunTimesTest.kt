package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
import java.time.*
class SunTimesTest{
 @Test fun aden(){val t=sunTimes(LocalDate.of(2026,11,27),12.8,45.0)!!;assertTrue(t[0]<t[1]);assertTrue(Duration.between(t[0],t[1]).toHours() in 10..13)}
}
