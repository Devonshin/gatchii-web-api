package com.gatchii.utils

import java.time.OffsetDateTime

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 09/02/2025
 */

class DateUtil {
    companion object {

        fun getCurrentDate(): OffsetDateTime {
            return OffsetDateTime.now()
        }

        fun formatSecondsToNaturalTime(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return "${hours}시간 ${minutes}분 ${seconds}초"
        }

    }
}
