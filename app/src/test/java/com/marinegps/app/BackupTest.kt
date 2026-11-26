package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class BackupTest {
 @Test fun roundTrip(){val p=TransferPoint("Fishing",12.0,45.0,"note",123L,"Other",4f);val d=TransferData(listOf(p),listOf(TransferTrack("Trip",listOf(listOf(p)),123,456,333,0.0,0.0)),mapOf("speed" to "kn"));assertEquals(d,restore(backup(d)))}
 @Test fun qr(){assertEquals(12.0,parseGeo("geo:12,45").lat,0.0)}
 @Test(expected=IllegalArgumentException::class) fun badQr(){parseGeo("geo:91,45")}
 @Test fun duplicate(){val p=TransferPoint("a",12.0,45.0);assertEquals(trackKey(listOf(listOf(p))),trackKey(listOf(listOf(p.copy(name="b")))))}
}
