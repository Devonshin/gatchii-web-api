package com.gatchii.common.task

import io.ktor.util.logging.*
import kotlinx.coroutines.Job

abstract class TaskLeadHandler(
  protected val taskName: String,
) {

  abstract fun startTask()

  protected var job: Job? = null

  fun stopTask() {
    this.job?.cancel()
  }

  fun taskName(): String {
    return taskName
  }

  companion object {

    private val logger: Logger = KtorSimpleLogger("TaskLeadHandler")
    private val tasks = mutableListOf<TaskLeadHandler>()
    private val taskNameSet = mutableSetOf<String>()

    // Adds a new task to the list, ensuring no duplicate task names.
    fun addTasks(taskHandler: TaskLeadHandler) {
      val taskName = taskHandler.taskName()
      if (taskNameSet.contains(taskName)) throw Exception("Task [${taskName}] already exists")
      logger.info("Add task {}", taskName)
      taskNameSet.add(taskName)
      tasks.add(taskHandler)
      logger.info("Added Task Names: {}", taskNameSet)
    }

    fun removeTask(taskName: String) {
      taskNameSet.remove(taskName)
      tasks.removeIf { it.taskName() == taskName }
      logger.info("Removed task $taskName")
    }

    // Executes all tasks by verifying if the current instance is the leader.
    fun runTasks() {
      logger.info("Running tasks...")
      if (isLeader()) {
        tasks.forEach {
          logger.info("Run leader task...${it.taskName()}")
          it.startTask()
        }
      }
    }

    fun isLeader(): Boolean {
      //todo leader check logic
      return true
    }
  }

}
