package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class LegacyCatchTest{@Test fun merge(){assertEquals("a\nb\nc",mergeLegacyCatch("a\nb","b\nc"));assertEquals("a",mergeLegacyCatch("a","a"))}}
