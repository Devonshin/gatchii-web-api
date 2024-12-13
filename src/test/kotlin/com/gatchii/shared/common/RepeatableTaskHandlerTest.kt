package com.gatchii.shared.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger

class RepeatableTaskHandlerTest {

    // Test if exception is thrown when repeatInMinute is 0
    @Test
    fun testIfExceptionIsThrownWhenRepeatInMinuteIsZero() {
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
    fun testIfExceptionIsThrownWhenRepeatInMinuteIsNegative() {
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
    fun testTaskNameMethodReturnsCorrectTaskName() {
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
    fun testTaskRunsAtSpecifiedIntervals() {
        //given
        val taskName = "Interval Task"
        val executionCount = AtomicInteger(0)

        val task: () -> Unit = {
            executionCount.incrementAndGet()
        }

        val handler = RepeatableTaskLeadHandler(taskName, Integer.MIN_VALUE, task)

        //when
        handler.doTask()

        //then
        Thread.sleep(15 * 1000)
        println("Execution Count: ${executionCount.get()}")
        assertTrue(executionCount.get() > 0, "Expected task to have run at least once within 3 seconds")
    }
}