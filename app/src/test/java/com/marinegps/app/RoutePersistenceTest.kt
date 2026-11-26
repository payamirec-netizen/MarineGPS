package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class RoutePersistenceTest {
 @Test fun roundTrip(){val p=listOf(RouteVertex(12.7,45.0),RouteVertex(12.8,45.1));assertEquals(p,decodeRoute(encodeRoute(p)))}
 @Test fun invalid(){assertTrue(decodeRoute("91,45;bad;12,181").isEmpty())}
}