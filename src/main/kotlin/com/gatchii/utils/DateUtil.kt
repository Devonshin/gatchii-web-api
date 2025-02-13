package com.gatchii.utils

import org.jetbrains.annotations.TestOnly
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 09/02/2025
 */

class DateUtil {
    companion object {

        var testDateCountMap = mutableMapOf<String, Clock>()

        //테스트 전용으로만 사용해야함 - 날짜 동기화를 위해
        @TestOnly
        fun initTestDate(testName: String) {
            testDateCountMap[testName] = Clock.fixed(Instant.now(), Clock.systemDefaultZone().zone)
        }
        //테스트 전용으로만 사용해야함 - 날짜 동기화를 위해
        @TestOnly
        fun getTestDate(testName: String): Clock {
            return testDateCountMap[testName]!!
        }
        //테스트 전용으로만 사용해야함 - 날짜 동기화를 위해
        @TestOnly
        fun applyTestDateCount(testName: String, value: Long) : Clock {
            testDateCountMap[testName] =
                Clock.fixed(Instant.ofEpochMilli(testDateCountMap[testName]!!.millis() + value), Clock.systemDefaultZone().zone)
            return testDateCountMap[testName]!!
        }

        fun getCurrentDate(): OffsetDateTime {
            return OffsetDateTime.now()
        }

        fun toReaderbleTimeFromSeconds(totalSeconds: Long): String {
            if(totalSeconds < 0) throw IllegalArgumentException()
            val day = totalSeconds / (3600 * 24)
            val hours = (totalSeconds % (3600 * 24)) / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return "${day}일 ${hours}시간 ${minutes}분 ${seconds}초"
        }

    }
}
