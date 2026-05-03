package com.yting27.xox_mobile.controllers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmsServiceTest {

    @Test
    fun testParseSmsBody_validMessage() {
        val body = """
            DA: RM10.50, MA: RM5.00
            Active till 31/12/2026
            Data: 500MB(Exp: 01/01/2027)
            SeasonPass: 10GB
        """.trimIndent()

        val result = SmsService.parseSmsBody(body)

        assertNotNull(result)
        assertEquals("RM15.50", result?.totalBalance)
        assertEquals("31/12/2026", result?.activeTill)
        assertEquals("500MB", result?.dataAmount)
        assertEquals("01/01/2027", result?.dataExp)
        assertEquals("10GB", result?.seasonPass)
    }

    @Test
    fun testParseSmsBody_partialMessage() {
        val body = "DA: RM5.00, Active till 20/10/2025"
        val result = SmsService.parseSmsBody(body)

        assertNotNull(result)
        assertEquals("RM5.00", result?.totalBalance)
        assertEquals("20/10/2025", result?.activeTill)
        assertEquals("N/A", result?.dataAmount)
        assertEquals("N/A", result?.seasonPass)
    }

    @Test
    fun testParseSmsBody_invalidMessage() {
        val body = "Hello, this is not a balance message."
        val result = SmsService.parseSmsBody(body)

        assertNotNull(result)
        assertEquals("RM0.00", result?.totalBalance)
        assertEquals("N/A", result?.activeTill)
    }
}
