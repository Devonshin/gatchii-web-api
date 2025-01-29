package com.gatchii.shared.common

import io.ktor.util.logging.*

class OnetimeTaskHandler(
    private val taskName: String,
    private val task: () -> Unit
): TaskLeadHandler() {
    private val logger = KtorSimpleLogger(this::class.simpleName?: "OnetimeTaskLeadHandler")
    init {
        logger.info("OnetimeTaskHandler init")
    }
    private var isDone = false

    override fun doTask() {
        if(isDone) return
        task()
        afterTaskSuccess()
    }

    fun afterTaskSuccess() {
        isDone = true
        removeTask(taskName)
    }

    override fun taskName(): String {
        return taskName
    }

}
