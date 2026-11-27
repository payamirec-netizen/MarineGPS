package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class CoordinateInputTest{@Test fun arabic(){assertEquals(12.75,parseCoordinateInput("١٢٫٧٥")!!,0.0);assertEquals(-45.1,parseCoordinateInput("−۴۵٫۱")!!,0.0);assertNull(parseCoordinateInput("NaN"))}}
