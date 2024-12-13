package com.gatchii.shared.common

import io.ktor.util.logging.*


abstract class TaskLeadHandler {

    abstract fun doTask()
    abstract fun taskName(): String

    companion object {

        private val logger: Logger = KtorSimpleLogger("com.gatchii.shared.common.TaskHandler")
        private val tasks = mutableListOf<TaskLeadHandler>()
        private val taskNameSet = mutableSetOf<String>()

        // Adds a new task to the list, ensuring no duplicate task names.
        fun addTasks(taskLeadHandler: TaskLeadHandler) {
            logger.debug("Adding task {}, taskNameSet = {}", taskLeadHandler.taskName(), taskNameSet)
            if (taskNameSet.contains(taskLeadHandler.taskName()))
                throw Exception("Task ${taskLeadHandler.taskName()} already exists")
            taskNameSet.add(taskLeadHandler.taskName())
            tasks.add(taskLeadHandler)
        }

        // Returns the list of tasks.
        fun getTasks() = tasks

        // Returns the list of tasks.
        fun getTaskNameSet() = taskNameSet

        // Clears all tasks and their corresponding names from the sets.
        fun cleanTasks() {
            tasks.clear()
            taskNameSet.clear()
        }

        fun removeTask(taskName: String) {
            taskNameSet.remove(taskName)
            tasks.removeIf { it.taskName() == taskName }
        }

        // Executes all tasks by verifying if the current instance is the leader.
        fun runTasks() {
            logger.info("Run tasks...")
            tasks.forEach {
                if (isLeader()) {
                    logger.info("Run leader task...${it.taskName()}")
                    it.doTask()
                }
            }
        }

        // Determines if the current instance should execute leader tasks, currently returns true (stub).
        fun isLeader(): Boolean {
            //todo leader check logic
            return true
        }
    }

}
