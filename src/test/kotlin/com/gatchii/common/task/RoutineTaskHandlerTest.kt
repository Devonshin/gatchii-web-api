package com.gatchii.common.task

import com.gatchii.utils.DateUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import shared.common.UnitTest

/**
 * Test class for the `RoutineTaskHandler` class.
 *
 * `RoutineTaskHandler` is responsible for scheduling and running tasks based on a given schedule.
 * The `startTask` method starts a coroutine that periodically executes the assigned task according
 * to the schedule defined in a `RoutineScheduleExpression`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@UnitTest
class RoutineTaskHandlerTest {

    @BeforeEach
    fun setUp() {
        DateUtil.initTestDate("RoutineTaskHandler")
    }

    //
    @Test
    fun `test task executes after delay when current time is before scheduled time`() = runTest {
        // given
        val scheduleExpression = RoutineScheduleExpression(hour = 23, minute = 59, second = 59)
        var taskExecutedCount = 0
        val task: () -> Unit = { taskExecutedCount++ }
        val handler = RoutineTaskHandler(
            taskName = "testTask",
            scheduleExpression = scheduleExpression,
            task = task,
            period = 24 * 60 * 60, //1일
            scope = this
        )

        // when
        handler.startTask()
        advanceTimeBy(3 * 24 * 60 * 60 * 1000) //3일
        runCurrent()
        // then
        handler.stopTask()
        assert(taskExecutedCount == 3) { "Task should execute after the scheduled delay time" }
    }

    //
    @Test
    fun `test task executes immediately when current time is after scheduled time`() = runTest {
        // given
        val scheduleExpression = RoutineScheduleExpression(hour = 0, minute = 0, second = 0)
        var taskExecutedCount = 0
        val task: () -> Unit = { taskExecutedCount++ }
        val handler = RoutineTaskHandler(
            taskName = "testTask",
            scheduleExpression = scheduleExpression,
            task = task,
            period = 24 * 60 * 60, //1일
            scope = this
        )

        // when
        handler.startTask()
        advanceTimeBy(3 * 24 * 60 * 60 * 1000) //3일
        runCurrent()
        // then
        handler.stopTask()
        assert(taskExecutedCount == 4) { "Task should execute immediately when current time is after scheduled time" }
    }


}