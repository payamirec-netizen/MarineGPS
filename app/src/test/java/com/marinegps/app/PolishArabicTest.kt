package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class PolishArabicTest{
 @Test fun bidi(){assertEquals("\u206612.0 / 45.0\u2069",ltrValue("12.0 / 45.0"))}
 @Test fun labels(){assertEquals("المواقع المحفوظة",translateText("saved waypoints",true));assertEquals("فعّل الموقع من إعدادات أندرويد",translateText("Enable location in Android settings",true))}
 @Test fun distinctTimes(){assertNotEquals(trackKey(listOf(listOf(TransferPoint("",12.0,45.0,time=1)))),trackKey(listOf(listOf(TransferPoint("",12.0,45.0,time=2)))))}
}
