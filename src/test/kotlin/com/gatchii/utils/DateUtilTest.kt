package com.gatchii.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import shared.common.UnitTest

@UnitTest
class DateUtilTest {

    @Test
    fun `test if formatSecondsToNaturalTime correctly formats seconds into days hours minutes seconds format for more than a day`() {
        // given
        val totalSeconds = 90061L // 1 day 1 hour 1 minute 1 second

        // when
        val result = DateUtil.toReaderbleTimeFromSeconds(totalSeconds)

        // then
        assertEquals("1일 1시간 1분 1초", result)
    }

    @Test
    fun `test if formatSecondsToNaturalTime correctly formats seconds into only hours minutes seconds when less than a day`() {
        // given
        val totalSeconds = 3661L // 1 hour 1 minute 1 second

        // when
        val result = DateUtil.toReaderbleTimeFromSeconds(totalSeconds)

        // then
        assertEquals("0일 1시간 1분 1초", result)
    }

    @Test
    fun `test if formatSecondsToNaturalTime correctly formats seconds into only minutes seconds when less than an hour`() {
        // given
        val totalSeconds = 61L // 1 minute 1 second

        // when
        val result = DateUtil.toReaderbleTimeFromSeconds(totalSeconds)

        // then
        assertEquals("0일 0시간 1분 1초", result)
    }

    @Test
    fun `test if formatSecondsToNaturalTime correctly formats seconds into only seconds when less than a minute`() {
        // given
        val totalSeconds = 59L // 59 seconds

        // when
        val result = DateUtil.toReaderbleTimeFromSeconds(totalSeconds)

        // then
        assertEquals("0일 0시간 0분 59초", result)
    }

    @Test
    fun `test if formatSecondsToNaturalTime returns all zeros when total seconds is zero`() {
        // given
        val totalSeconds = 0L

        // when
        val result = DateUtil.toReaderbleTimeFromSeconds(totalSeconds)

        // then
        assertEquals("0일 0시간 0분 0초", result)
    }

    @Test
    fun `test if formatSecondsToNaturalTime handles large values correctly`() {
        // given
        val totalSeconds = 900000L // 10 days 10 hours

        // when
        val result = DateUtil.toReaderbleTimeFromSeconds(totalSeconds)

        // then
        assertEquals("10일 10시간 0분 0초", result)
    }

    @Test
    fun `test if argument is minus then throws exception`() =
        assert(runCatching { DateUtil.toReaderbleTimeFromSeconds(-1) }.isFailure)

}