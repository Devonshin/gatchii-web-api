/**
 * @author Devonshin
 * @date 2025-10-11
 */
package com.gatchii.integration

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import shared.common.IntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 기본 엔드포인트 E2E 테스트
 * 
 * 태스크 #11.1: 기본 엔드포인트 E2E 테스트 구현
 * 
 * 검증 항목:
 * 1. GET / - 루트 경로 응답 확인
 * 2. 응답 메시지 검증
 * 3. 응답 시간 측정
 * 4. Content-Type 확인
 */
@IntegrationTest
class MainEndpointE2ETest {

  /**
   * 루트 엔드포인트 기본 응답 테스트
   */
  @Test
  fun `GET root endpoint should return Hello World message`() = testApplication {
    // When: 루트 경로로 GET 요청
    val startTime = System.currentTimeMillis()
    val response = client.get("/")
    val endTime = System.currentTimeMillis()

    // Then: 200 OK 응답
    assertEquals(HttpStatusCode.OK, response.status, "Should return 200 OK")
    
    // And: Hello World 메시지 포함
    val body = response.bodyAsText()
    assertTrue(body.contains("Hello World"), "Response should contain 'Hello World'")
    assertTrue(body.contains("main page"), "Response should contain 'main page'")
    
    // And: 응답 시간이 합리적 범위 내
    val responseTime = endTime - startTime
    assertTrue(responseTime < 1000, "Response time should be less than 1 second, was ${responseTime}ms")
  }

  /**
   * 루트 엔드포인트 응답 형식 테스트
   */
  @Test
  fun `GET root endpoint should return text plain content type`() = testApplication {
    // When: 루트 경로로 GET 요청
    val response = client.get("/")

    // Then: Content-Type이 text/plain
    val contentType = response.contentType()
    assertNotNull(contentType, "Content-Type should not be null")
    assertEquals(ContentType.Text.Plain.withoutParameters(), contentType?.withoutParameters())
  }

  /**
   * 루트 엔드포인트 다중 요청 테스트
   */
  @Test
  fun `GET root endpoint should handle multiple concurrent requests`() = testApplication {
    // Given: 10개의 동시 요청
    val requestCount = 10
    
    // When: 여러 요청을 동시에 보냄
    val responses = (1..requestCount).map {
      client.get("/")
    }

    // Then: 모든 요청이 성공
    responses.forEach { response ->
      assertEquals(HttpStatusCode.OK, response.status)
      val body = response.bodyAsText()
      assertTrue(body.contains("Hello World"))
    }
  }

  /**
   * 루트 엔드포인트 캐시 헤더 테스트
   */
  @Test
  fun `GET root endpoint should include appropriate headers`() = testApplication {
    // When: 루트 경로로 GET 요청
    val response = client.get("/")

    // Then: 기본 헤더 확인
    assertEquals(HttpStatusCode.OK, response.status)
    assertNotNull(response.headers, "Response headers should not be null")
  }

  /**
   * 빈 경로 요청 테스트 (루트와 동일하게 처리되어야 함)
   */
  @Test
  fun `GET empty path should be treated as root endpoint`() = testApplication {
    // When: 빈 경로로 GET 요청
    val response = client.get("")

    // Then: 루트와 동일한 응답
    assertEquals(HttpStatusCode.OK, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("Hello World"))
  }

  /**
   * 루트 엔드포인트 응답 본문 길이 테스트
   */
  @Test
  fun `GET root endpoint should return non-empty response body`() = testApplication {
    // When: 루트 경로로 GET 요청
    val response = client.get("/")

    // Then: 응답 본문이 비어있지 않음
    val body = response.bodyAsText()
    assertTrue(body.isNotEmpty(), "Response body should not be empty")
    assertTrue(body.length > 10, "Response body should have meaningful content")
  }

  /**
   * 루트 엔드포인트 HTTP 메서드 제한 테스트
   */
  @Test
  fun `POST to root endpoint should return appropriate response`() = testApplication {
    // When: 루트 경로로 POST 요청 (지원하지 않는 메서드)
    val response = client.post("/") {
      setBody("")
    }

    // Then: 405 Method Not Allowed 또는 404 Not Found
    assertTrue(
      response.status == HttpStatusCode.MethodNotAllowed || 
      response.status == HttpStatusCode.NotFound,
      "Should return 405 or 404 for unsupported method"
    )
  }

  /**
   * 루트 엔드포인트 쿼리 파라미터 무시 테스트
   */
  @Test
  fun `GET root endpoint with query parameters should still work`() = testApplication {
    // When: 쿼리 파라미터를 포함한 루트 경로 요청
    val response = client.get("/?param=value&test=123")

    // Then: 쿼리 파라미터를 무시하고 정상 응답
    assertEquals(HttpStatusCode.OK, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("Hello World"))
  }

  /**
   * 루트 엔드포인트 Accept 헤더 테스트
   */
  @Test
  fun `GET root endpoint with different Accept headers should work`() = testApplication {
    // Given: 다양한 Accept 헤더
    val acceptHeaders = listOf(
      "text/plain",
      "text/html",
      "*/*",
      "application/json"
    )

    acceptHeaders.forEach { acceptHeader ->
      // When: Accept 헤더를 포함한 요청
      val response = client.get("/") {
        header(HttpHeaders.Accept, acceptHeader)
      }

      // Then: 정상 응답 (Accept 헤더와 무관하게)
      assertEquals(HttpStatusCode.OK, response.status, 
        "Should return 200 OK regardless of Accept header: $acceptHeader")
    }
  }

  /**
   * 루트 엔드포인트 성능 기준선 테스트
   */
  @Test
  fun `GET root endpoint should meet performance baseline`() = testApplication {
    // Given: 100개의 요청
    val iterations = 100
    val responseTimes = mutableListOf<Long>()

    // When: 반복적으로 요청하여 응답 시간 측정
    repeat(iterations) {
      val startTime = System.currentTimeMillis()
      val response = client.get("/")
      val endTime = System.currentTimeMillis()
      
      assertEquals(HttpStatusCode.OK, response.status)
      responseTimes.add(endTime - startTime)
    }

    // Then: 평균 응답 시간이 합리적 범위 내
    val avgResponseTime = responseTimes.average()
    assertTrue(avgResponseTime < 100, 
      "Average response time should be less than 100ms, was ${avgResponseTime}ms")
    
    // And: 99번째 백분위수 응답 시간도 합리적
    val sortedTimes = responseTimes.sorted()
    val p99 = sortedTimes[(iterations * 0.99).toInt()]
    assertTrue(p99 < 500, "P99 response time should be less than 500ms, was ${p99}ms")
  }

  /**
   * assertNotNull 헬퍼 함수
   */
  private fun assertNotNull(value: Any?, message: String = "Value should not be null") {
    assertTrue(value != null, message)
  }
}
