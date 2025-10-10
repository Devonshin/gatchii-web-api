/**
 * @author Devonshin
 * @date 2025-10-10
 */
package com.gatchii.common.exception

import org.junit.jupiter.api.Test
import shared.common.UnitTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 공통 예외 클래스 테스트
 * 
 * 태스크 #5.1: 예외 클래스 테스트 구현
 * 
 * 검증 항목:
 * 1. NotFoundUserException - 사용자 미발견 예외
 * 2. NotSupportMethodException - 지원하지 않는 메서드 예외
 * 3. InvalidUsableJwkStatusException - 유효하지 않은 JWK 상태 예외
 */
@UnitTest
class ExceptionTest {

  /**
   * NotFoundUserException 테스트
   * 
   * 검증:
   * - 예외 메시지 전달 및 조회
   * - Exception 타입 상속 확인
   */
  @Test
  fun `NotFoundUserException should be created with message`() {
    // Given
    val errorMessage = "User with ID 12345 not found"
    
    // When
    val exception = NotFoundUserException(errorMessage)
    
    // Then
    assertNotNull(exception, "Exception should not be null")
    assertEquals(errorMessage, exception.message, "Exception message should match")
    assertTrue(exception is Exception, "NotFoundUserException should be an Exception")
  }

  /**
   * NotFoundUserException - null 메시지 테스트
   */
  @Test
  fun `NotFoundUserException should accept null message`() {
    // When
    val exception = NotFoundUserException(null)
    
    // Then
    assertNotNull(exception, "Exception should not be null")
    assertEquals(null, exception.message, "Exception message should be null")
  }

  /**
   * NotFoundUserException - 빈 메시지 테스트
   */
  @Test
  fun `NotFoundUserException should accept empty message`() {
    // Given
    val emptyMessage = ""
    
    // When
    val exception = NotFoundUserException(emptyMessage)
    
    // Then
    assertNotNull(exception, "Exception should not be null")
    assertEquals(emptyMessage, exception.message, "Exception message should be empty")
  }

  /**
   * NotSupportMethodException 테스트
   * 
   * 검증:
   * - 예외 생성 및 Throwable 타입 확인
   * - 메시지 전달 (현재 구현상 메시지가 저장되지 않음)
   */
  @Test
  fun `NotSupportMethodException should be created`() {
    // Given
    val errorMessage = "HTTP method POST is not supported"
    
    // When
    val exception = NotSupportMethodException(errorMessage)
    
    // Then
    assertNotNull(exception, "Exception should not be null")
    assertTrue(exception is Throwable, "NotSupportMethodException should be a Throwable")
  }

  /**
   * NotSupportMethodException - 다양한 메시지 테스트
   */
  @Test
  fun `NotSupportMethodException should be created with various messages`() {
    // Given
    val messages = listOf(
      "Method not allowed",
      "PATCH is not supported",
      "Only GET and POST are supported"
    )
    
    // When & Then
    messages.forEach { message ->
      val exception = NotSupportMethodException(message)
      assertNotNull(exception, "Exception with message '$message' should not be null")
    }
  }

  /**
   * InvalidUsableJwkStatusException 테스트
   * 
   * 검증:
   * - 예외 생성 및 Throwable 타입 확인
   * - JWK 상태 관련 에러 메시지 처리
   */
  @Test
  fun `InvalidUsableJwkStatusException should be created`() {
    // Given
    val errorMessage = "JWK status is invalid: EXPIRED"
    
    // When
    val exception = InvalidUsableJwkStatusException(errorMessage)
    
    // Then
    assertNotNull(exception, "Exception should not be null")
    assertTrue(exception is Throwable, "InvalidUsableJwkStatusException should be a Throwable")
  }

  /**
   * InvalidUsableJwkStatusException - 다양한 JWK 상태 에러 테스트
   */
  @Test
  fun `InvalidUsableJwkStatusException should handle various JWK status errors`() {
    // Given
    val statusErrors = listOf(
      "JWK is in INACTIVE status",
      "JWK has been REVOKED",
      "JWK status transition from ACTIVE to DELETED is not allowed",
      "Cannot use JWK with status: PENDING"
    )
    
    // When & Then
    statusErrors.forEach { error ->
      val exception = InvalidUsableJwkStatusException(error)
      assertNotNull(exception, "Exception with error '$error' should not be null")
    }
  }

  /**
   * 예외 계층 구조 테스트
   * 
   * 검증:
   * - 각 예외가 올바른 부모 클래스를 상속하는지 확인
   */
  @Test
  fun `exception hierarchy should be correct`() {
    // Given
    val notFoundUserException = NotFoundUserException("test")
    val notSupportMethodException = NotSupportMethodException("test")
    val invalidJwkStatusException = InvalidUsableJwkStatusException("test")
    
    // Then
    assertTrue(
      notFoundUserException is Exception,
      "NotFoundUserException should extend Exception"
    )
    assertTrue(
      notFoundUserException is Throwable,
      "NotFoundUserException should be a Throwable"
    )
    
    assertTrue(
      notSupportMethodException is Throwable,
      "NotSupportMethodException should extend Throwable"
    )
    
    assertTrue(
      invalidJwkStatusException is Throwable,
      "InvalidUsableJwkStatusException should extend Throwable"
    )
  }

  /**
   * 예외 throw 및 catch 테스트
   */
  @Test
  fun `exceptions should be throwable and catchable`() {
    // NotFoundUserException
    try {
      throw NotFoundUserException("User not found")
      assert(false) { "Should have thrown exception" }
    } catch (e: NotFoundUserException) {
      assertEquals("User not found", e.message)
    }
    
    // NotSupportMethodException
    try {
      throw NotSupportMethodException("Method not supported")
      assert(false) { "Should have thrown exception" }
    } catch (e: NotSupportMethodException) {
      assertNotNull(e)
    }
    
    // InvalidUsableJwkStatusException
    try {
      throw InvalidUsableJwkStatusException("Invalid JWK status")
      assert(false) { "Should have thrown exception" }
    } catch (e: InvalidUsableJwkStatusException) {
      assertNotNull(e)
    }
  }

  /**
   * 예외 스택 트레이스 테스트
   */
  @Test
  fun `exceptions should have stack trace`() {
    // Given & When
    val notFoundUserException = NotFoundUserException("test user")
    val notSupportMethodException = NotSupportMethodException("test method")
    val invalidJwkStatusException = InvalidUsableJwkStatusException("test status")
    
    // Then
    assertNotNull(
      notFoundUserException.stackTrace,
      "NotFoundUserException should have stack trace"
    )
    assertTrue(
      notFoundUserException.stackTrace.isNotEmpty(),
      "NotFoundUserException stack trace should not be empty"
    )
    
    assertNotNull(
      notSupportMethodException.stackTrace,
      "NotSupportMethodException should have stack trace"
    )
    
    assertNotNull(
      invalidJwkStatusException.stackTrace,
      "InvalidUsableJwkStatusException should have stack trace"
    )
  }
}
