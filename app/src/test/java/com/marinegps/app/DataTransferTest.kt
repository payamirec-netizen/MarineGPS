package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class DataTransferTest {
 @Test fun roundTrip(){val d=TransferData(listOf(TransferPoint("A & B",12.0,45.0,"<note>")),listOf(TransferTrack("T",listOf(listOf(TransferPoint("",12.0,45.0)),listOf(TransferPoint("",13.0,46.0))))));assertEquals(d,parseGpx(gpx(d)))}
 @Test(expected=IllegalArgumentException::class) fun rejectsDtd(){parseGpx("<!DOCTYPE gpx><gpx/>")}
 @Test(expected=IllegalArgumentException::class) fun rejectsCoordinates(){parseGpx("<gpx><wpt lat=\"91\" lon=\"0\"/></gpx>")}
 @Test fun csvAndKml(){val d=TransferData(listOf(TransferPoint("A",12.0,45.0)),emptyList());assertTrue(csv(d).contains("12.0,45.0"));assertTrue(kml(d).contains("45.0,12.0"))}
}
