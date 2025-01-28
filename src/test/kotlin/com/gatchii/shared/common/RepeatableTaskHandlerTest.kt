package com.gatchii.shared.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger

class RepeatableTaskHandlerTest {

    // Test if exception is thrown when repeatInMinute is 0
    @Test
    fun testIfExceptionIsThrownWhenRepeatInMinuteIsZero() = runTest {
        //given
        val taskName = "Sample Task"
        val repeatInMinute = 0
        val task: () -> Unit = {}

        //when
        //then
        assertThrows<Exception> {
            RepeatableTaskLeadHandler(taskName, repeatInMinute, task)
        }
    }

    // Test if exception is thrown when repeatInMinute is negative
    @Test
    fun testIfExceptionIsThrownWhenRepeatInMinuteIsNegative() = runTest {
        //given
        val taskName = "Sample Task"
        val repeatInMinute = -5
        val task: () -> Unit = {}

        //when
        //then
        assertThrows<Exception> {
            RepeatableTaskLeadHandler(taskName, repeatInMinute, task)
        }
    }

    // Test if taskName method returns correct task name
    @Test
    fun testTaskNameMethodReturnsCorrectTaskName()  =  runTest{
        //given
        val taskName = "Sample Task"
        val repeatInMinute = 1
        val task: () -> Unit = {}

        val handler = RepeatableTaskLeadHandler(taskName, repeatInMinute, task)

        //when
        val result = handler.taskName()

        //then
        assertEquals(taskName, result)
    }

    // Test to verify that the task runs at specified intervals
    @Test
    @OptIn(ExperimentalCoroutinesApi::class) // Opt-in으로 실험적 API 사용 허용
    fun testTaskRunsAtSpecifiedIntervals() = runTest {
        //given
        val taskName = "Interval Task"
        val executionCount = AtomicInteger(0)

        val task: () -> Unit = {
            executionCount.incrementAndGet()
        }

        val handler = RepeatableTaskLeadHandler(taskName, Integer.MIN_VALUE, task)
        val delayTimeMills = (15 * 1000).toLong()
        //when
        handler.doTask()
        delay(delayTimeMills)
        //then
        advanceTimeBy(delayTimeMills)
        println("Execution Count: ${executionCount.get()}")
        assertTrue(executionCount.get() > 0, "Expected task to have run at least once within 3 seconds")
    }

    private suspend fun delay(delayTimeMills: Long) : String{
        kotlinx.coroutines.delay(delayTimeMills) // 코루틴 지연 함수 사용
        return "Finished : $delayTimeMills"
    }
}