package com.example.echoai.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MathUtilsTest {

    @Test
    fun testAdd() {
        val mathUtils = MathUtils()
        assertEquals(4, mathUtils.add(2, 2))
    }
}
