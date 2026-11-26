package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class LocalizationTest{
 @Test fun language(){assertEquals("المواقع",translateText("Waypoints",true));assertEquals("Waypoints",translateText("Waypoints",false))}
 @Test fun qrGroup(){val d=TransferData(listOf(TransferPoint("صيد",12.0,45.0),TransferPoint("مرسى",13.0,46.0)),emptyList());assertEquals(d,decodeQrPayload("MarineGPS:"+backup(d)))}
}
