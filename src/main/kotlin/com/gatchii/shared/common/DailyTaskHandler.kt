package com.gatchii.shared.common

import java.util.*
import kotlin.concurrent.timerTask

/** Package: com.gatchii.shared.common Created: Devonshin Date: 01/12/2024 */

class DailyTaskHandler(
    private val taskName: String,
    private val scheduledTime: Date,
    private val task: () -> Unit
): TaskLeadHandler() {
    override fun taskName(): String {
        return taskName
    }

    override fun doTask() {
        Timer().schedule(
            timerTask {
                task()
            },
            scheduledTime
        )
    }
}