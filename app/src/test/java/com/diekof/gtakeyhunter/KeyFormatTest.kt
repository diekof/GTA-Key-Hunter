package com.diekof.gtakeyhunter
import org.junit.Assert.assertEquals
import org.junit.Test
class KeyFormatTest {
    @Test fun extractsVoucher() {
        assertEquals(listOf("FHA34MK876CL"), KeyEngine.extract("KEY FHA3-4MK8-76CL"))
    }
}