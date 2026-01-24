package com.music.vivi.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {

    @Test
    fun makeTimeString_formatsCorrectly() {
        assertEquals("0:00", makeTimeString(0))
        assertEquals("0:59", makeTimeString(59000))
        assertEquals("1:00", makeTimeString(60000))
        assertEquals("1:05", makeTimeString(65000))
        assertEquals("1:00:00", makeTimeString(3600000))
        assertEquals("1:01:01", makeTimeString(3661000))
        assertEquals("1:01:01:01", makeTimeString(90061000)) // 1 day, 1 hour, 1 min, 1 sec
    }

    @Test
    fun makeTimeString_handlesNullOrNegative() {
        assertEquals("", makeTimeString(null))
        assertEquals("", makeTimeString(-100))
    }

    @Test
    fun joinByBullet_joinsStrings() {
        assertEquals("A • B", joinByBullet("A", "B"))
        assertEquals("A", joinByBullet("A"))
        assertEquals("A • B", joinByBullet("A", null, "B", ""))
        assertEquals("", joinByBullet(null, ""))
    }

    @Test
    fun md5_generation() {
        // MD5 of "hello" is "5d41402abc4b2a76b9719d911017c592"
        assertEquals("5d41402abc4b2a76b9719d911017c592", md5("hello"))
    }
}
