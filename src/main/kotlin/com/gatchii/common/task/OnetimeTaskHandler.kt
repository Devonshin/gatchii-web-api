package com.gatchii.common.task

import io.ktor.util.logging.*

class OnetimeTaskHandler(
  taskName: String,
  private val task: () -> Unit
) : TaskLeadHandler(taskName) {

  private val logger = KtorSimpleLogger(this::class.simpleName ?: "OnetimeTaskLeadHandler")

  init {
    logger.info("OnetimeTaskHandler init")
  }

  private var isDone = false

  override fun startTask() {
    if (isDone) return
    task()
    afterTaskSuccess()
  }

  fun afterTaskSuccess() {
    isDone = true
    removeTask(taskName)
  }

}
