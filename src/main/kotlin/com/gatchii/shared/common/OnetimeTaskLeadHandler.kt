package com.gatchii.shared.common

class OnetimeTaskLeadHandler(
    private val taskName: String,
    private val task: () -> Unit
): TaskLeadHandler() {
    init {
        println("OnetimeTaskHandler init")
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
