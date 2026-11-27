package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class NavigationSessionTest {
 @Test fun retainsOriginAndRejectsOldFix(){val s=NavigationSession();val a=RouteVertex(12.0,45.0);s.add(a,1000);s.add(RouteVertex(12.001,45.0),2000);s.add(a,1500);assertEquals(a,s.start);assertEquals(2,s.trail.size)}
 @Test fun retainsGap(){val s=NavigationSession();val a=RouteVertex(12.0,45.0);s.add(a,1000);s.add(a,12000);assertEquals(11000L,s.trail.last().time-s.trail.first().time)}
}
