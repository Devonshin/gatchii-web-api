package com.gatchii.shared.common

import java.util.*
import kotlin.concurrent.timerTask

/** Package: com.gatchii.shared.common Created: Devonshin Date: 01/12/2024 */

class RepeatableTaskHandler(
    private val taskName: String,
    private val repeatInMinute: Int = Integer.MAX_VALUE,
    private val task: () -> Unit
): TaskLeadHandler() {

    init {
        if(repeatInMinute <= 0 && repeatInMinute > Integer.MIN_VALUE) {
            throw Exception("Repeat in minute must be greater than 0")
        }
    }

    override fun taskName(): String {
        return taskName
    }

    override fun doTask() {
        val mills = if(repeatInMinute == Integer.MIN_VALUE) {
            5 * 1000
        } else {
            repeatInMinute * 60 * 1000
        }
        Timer().scheduleAtFixedRate(
            timerTask {
                task()
            },
            0L,
            mills.toLong()
        )
    }
}