/**
 * @author Devonshin
 * @date 2025-10-10
 */
package com.gatchii.common.task

import org.junit.jupiter.api.Test
import shared.common.UnitTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * OnetimeTaskHandler 테스트
 * 
 * 태스크 #5.3: Task Handler 클래스 테스트 구현
 * 
 * 검증 항목:
 * 1. 일회성 작업이 한 번만 실행되는지 확인
 * 2. 작업 완료 후 중복 실행 방지 확인
 * 3. 작업 완료 후 자동 제거 확인
 * 4. 작업 이름 관리 확인
 */
@UnitTest
class OnetimeTaskHandlerTest {

  /**
   * 일회성 작업이 정확히 한 번만 실행되는지 테스트
   */
  @Test
  fun `test task executes exactly once`() {
    // Given: 카운터를 증가시키는 작업
    var taskExecutedCount = 0
    val task: () -> Unit = { taskExecutedCount++ }
    
    val handler = OnetimeTaskHandler(
      taskName = "testOnetimeTask",
      task = task
    )
    
    // When: startTask를 여러 번 호출
    handler.startTask()
    handler.startTask()
    handler.startTask()
    
    // Then: 작업이 한 번만 실행되어야 함
    assertEquals(1, taskExecutedCount, "Task should execute exactly once")
  }

  /**
   * 작업이 성공적으로 완료된 후 isDone 플래그가 설정되는지 테스트
   */
  @Test
  fun `test afterTaskSuccess sets isDone flag`() {
    // Given: 간단한 작업
    var taskExecuted = false
    val task: () -> Unit = { taskExecuted = true }
    
    val handler = OnetimeTaskHandler(
      taskName = "testAfterSuccess",
      task = task
    )
    
    // When: 작업 실행
    handler.startTask()
    
    // Then: 작업이 실행되었고, 중복 실행이 방지되어야 함
    assertTrue(taskExecuted, "Task should be executed")
    
    // 다시 실행 시도
    taskExecuted = false
    handler.startTask()
    assertFalse(taskExecuted, "Task should not be executed again")
  }

  /**
   * 작업 이름이 올바르게 반환되는지 테스트
   */
  @Test
  fun `test taskName returns correct name`() {
    // Given
    val expectedTaskName = "myTestTask"
    val handler = OnetimeTaskHandler(
      taskName = expectedTaskName,
      task = {}
    )
    
    // When
    val actualTaskName = handler.taskName()
    
    // Then
    assertEquals(expectedTaskName, actualTaskName, "Task name should match")
  }

  /**
   * 작업이 실행되지 않았을 때는 작업이 정상적으로 수행되는지 테스트
   */
  @Test
  fun `test task executes when not done`() {
    // Given: 외부 상태를 변경하는 작업
    var externalState = "initial"
    val task: () -> Unit = { externalState = "modified" }
    
    val handler = OnetimeTaskHandler(
      taskName = "testStateChange",
      task = task
    )
    
    // When
    handler.startTask()
    
    // Then
    assertEquals("modified", externalState, "External state should be modified by task")
  }

  /**
   * 여러 개의 OnetimeTaskHandler가 독립적으로 동작하는지 테스트
   */
  @Test
  fun `test multiple handlers work independently`() {
    // Given: 여러 핸들러
    var task1Count = 0
    var task2Count = 0
    var task3Count = 0
    
    val handler1 = OnetimeTaskHandler(
      taskName = "task1",
      task = { task1Count++ }
    )
    
    val handler2 = OnetimeTaskHandler(
      taskName = "task2",
      task = { task2Count++ }
    )
    
    val handler3 = OnetimeTaskHandler(
      taskName = "task3",
      task = { task3Count++ }
    )
    
    // When: 각 핸들러를 여러 번 실행
    handler1.startTask()
    handler1.startTask()
    
    handler2.startTask()
    
    handler3.startTask()
    handler3.startTask()
    handler3.startTask()
    
    // Then: 각 핸들러의 작업이 한 번씩만 실행되어야 함
    assertEquals(1, task1Count, "Handler1 task should execute once")
    assertEquals(1, task2Count, "Handler2 task should execute once")
    assertEquals(1, task3Count, "Handler3 task should execute once")
  }

  /**
   * 작업 실행 중 예외가 발생해도 안전하게 처리되는지 테스트
   */
  @Test
  fun `test task handles exception gracefully`() {
    // Given: 예외를 발생시키는 작업
    var executionAttempted = false
    val task: () -> Unit = {
      executionAttempted = true
      throw RuntimeException("Test exception")
    }
    
    val handler = OnetimeTaskHandler(
      taskName = "testException",
      task = task
    )
    
    // When & Then: 예외가 발생하더라도 애플리케이션이 중단되지 않아야 함
    try {
      handler.startTask()
    } catch (e: RuntimeException) {
      // 예외 발생 확인
      assertEquals("Test exception", e.message)
    }
    
    assertTrue(executionAttempted, "Task execution should have been attempted")
  }

  /**
   * 빈 작업도 정상적으로 처리되는지 테스트
   */
  @Test
  fun `test empty task executes successfully`() {
    // Given: 빈 작업
    val handler = OnetimeTaskHandler(
      taskName = "emptyTask",
      task = {}
    )
    
    // When & Then: 예외 없이 실행되어야 함
    handler.startTask()
    handler.startTask() // 두 번째 호출도 문제없이 처리
  }

  /**
   * 람다 작업이 올바르게 실행되는지 테스트
   */
  @Test
  fun `test lambda task executes correctly`() {
    // Given: 복잡한 람다 작업
    val results = mutableListOf<String>()
    val task: () -> Unit = {
      results.add("step1")
      results.add("step2")
      results.add("step3")
    }
    
    val handler = OnetimeTaskHandler(
      taskName = "lambdaTask",
      task = task
    )
    
    // When
    handler.startTask()
    
    // Then
    assertEquals(3, results.size, "Lambda should execute all steps")
    assertEquals(listOf("step1", "step2", "step3"), results, "Lambda steps should be in correct order")
  }

  /**
   * stopTask 호출이 OnetimeTaskHandler에서 안전하게 처리되는지 테스트
   */
  @Test
  fun `test stopTask can be called safely`() {
    // Given
    var taskExecuted = false
    val handler = OnetimeTaskHandler(
      taskName = "stoppableTask",
      task = { taskExecuted = true }
    )
    
    // When: 작업 실행 전후로 stopTask 호출
    handler.stopTask() // 실행 전 호출 (job이 null이므로 안전해야 함)
    handler.startTask()
    handler.stopTask() // 실행 후 호출
    
    // Then: 작업이 실행되었고, stopTask 호출이 예외를 발생시키지 않아야 함
    assertTrue(taskExecuted, "Task should have been executed")
  }

  /**
   * 작업 완료 후 afterTaskSuccess가 올바르게 동작하는지 테스트
   */
  @Test
  fun `test afterTaskSuccess completes successfully`() {
    // Given
    var taskExecuted = false
    val handler = OnetimeTaskHandler(
      taskName = "afterSuccessTask",
      task = { taskExecuted = true }
    )
    
    // When
    handler.startTask()
    
    // Then: 작업 실행 후 afterTaskSuccess가 내부적으로 호출되어야 함
    assertTrue(taskExecuted, "Task should be executed")
    
    // 중복 실행 방지 확인
    taskExecuted = false
    handler.startTask()
    assertFalse(taskExecuted, "After success, task should not execute again")
  }
}
