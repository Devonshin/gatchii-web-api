/**
 * @author Devonshin
 * @date 2025-10-11
 */
package com.gatchii.plugins

import com.gatchii.common.exception.NotFoundUserException
import com.gatchii.domain.login.LoginUserRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * StatusPages 플러그인 테스트 클래스
 * 
 * 예외 처리 및 HTTP 상태 코드 핸들링을 테스트합니다.
 */
class StatusPagesTest {

  /**
   * 404 Not Found 응답 테스트
   */
  @Test
  fun `404 Not Found should return error response with proper format`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
    }

    // When: 존재하지 않는 경로로 GET 요청
    val response = client.get("/non-existent-path")

    // Then: 404 응답과 ErrorResponse 형식 반환
    assertEquals(HttpStatusCode.NotFound, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("\"code\":404"))
    assertTrue(body.contains("/non-existent-path"))
    assertTrue(body.contains("\"timestamp\""))
  }

  /**
   * 401 Unauthorized 응답 테스트
   */
  @Test
  fun `401 Unauthorized should return error response with proper format`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
      routing {
        get("/test/unauthorized") {
          call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
        }
      }
    }

    // When: Unauthorized를 반환하는 엔드포인트 호출
    val response = client.get("/test/unauthorized")

    // Then: 401 응답과 ErrorResponse 형식 반환
    assertEquals(HttpStatusCode.Unauthorized, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("\"code\":401"))
    assertTrue(body.contains("/test/unauthorized"))
  }

  /**
   * NotFoundUserException 예외 처리 테스트
   */
  @Test
  fun `NotFoundUserException should return 404 with error response`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
      routing {
        get("/test/user-not-found") {
          throw NotFoundUserException("User with ID 123 not found")
        }
      }
    }

    // When: NotFoundUserException을 발생시키는 엔드포인트 호출
    val response = client.get("/test/user-not-found")

    // Then: 404 응답과 사용자 지정 에러 메시지
    assertEquals(HttpStatusCode.NotFound, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("\"code\":404"))
    assertTrue(body.contains("User with ID 123 not found"))
    assertTrue(body.contains("/test/user-not-found"))
  }

  /**
   * RequestValidationException 예외 처리 테스트
   */
  @Test
  fun `RequestValidationException should return 400 with error response`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
      configureValidation()
      routing {
        post("/test/validate") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 유효하지 않은 로그인 요청
    val invalidRequest = LoginUserRequest(
      prefixId = "",
      suffixId = "example",
      password = "password"
    )

    // When: 유효성 검사에 실패하는 요청 전송
    val response = client.post("/test/validate") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(invalidRequest))
    }

    // Then: 400 응답과 검증 에러 메시지
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("\"code\":400"))
    assertTrue(body.contains("Invalid login parameter"))
    assertTrue(body.contains("/test/validate"))
  }

  /**
   * 일반 예외(Throwable) 처리 테스트
   */
  @Test
  fun `Generic exception should return 500 with error response`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
      routing {
        get("/test/generic-error") {
          throw RuntimeException("Unexpected server error")
        }
      }
    }

    // When: 일반 예외를 발생시키는 엔드포인트 호출
    val response = client.get("/test/generic-error")

    // Then: 500 응답과 에러 메시지
    assertEquals(HttpStatusCode.InternalServerError, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("\"code\":500"))
    assertTrue(body.contains("Unexpected server error"))
    assertTrue(body.contains("/test/generic-error"))
  }

  /**
   * ErrorResponse 타임스탬프 검증 테스트
   */
  @Test
  fun `ErrorResponse should contain valid timestamp`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
    }

    // When: 404 응답 요청
    val response = client.get("/non-existent")
    val body = response.bodyAsText()

    // Then: 타임스탬프가 포함되어 있음
    assertTrue(body.contains("\"timestamp\""))
    // 타임스탬프 값 추출하여 유효성 검증
    val timestampMatch = Regex("\"timestamp\":(\\d+)").find(body)
    assertTrue(timestampMatch != null, "Timestamp should be present in response")
    val timestamp = timestampMatch!!.groupValues[1].toLong()
    val currentTime = System.currentTimeMillis()
    val timeDiff = currentTime - timestamp
    assertTrue(timeDiff >= 0 && timeDiff < 5000, "Timestamp should be within 5 seconds of current time")
  }

  /**
   * 여러 예외가 순서대로 처리되는지 테스트
   */
  @Test
  fun `Multiple exceptions should be handled correctly in order`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
      routing {
        get("/test/multiple/{type}") {
          when (val type = call.parameters["type"]) {
            "not-found-user" -> throw NotFoundUserException("User not found")
            "runtime" -> throw RuntimeException("Runtime error")
            else -> call.respond(HttpStatusCode.NotFound)
          }
        }
      }
    }

    // When & Then: 각 예외 유형별로 올바른 응답 확인
    // NotFoundUserException
    val notFoundUserResponse = client.get("/test/multiple/not-found-user")
    assertEquals(HttpStatusCode.NotFound, notFoundUserResponse.status)
    assertTrue(notFoundUserResponse.bodyAsText().contains("User not found"))

    // RuntimeException
    val runtimeResponse = client.get("/test/multiple/runtime")
    assertEquals(HttpStatusCode.InternalServerError, runtimeResponse.status)
    assertTrue(runtimeResponse.bodyAsText().contains("Runtime error"))

    // 404 Status
    val notFoundResponse = client.get("/test/multiple/other")
    assertEquals(HttpStatusCode.NotFound, notFoundResponse.status)
  }

  /**
   * ErrorResponse JSON 직렬화 검증 테스트
   */
  @Test
  fun `ErrorResponse should be properly serialized to JSON`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
    }

    // When: 404 응답 요청
    val response = client.get("/test-path")
    val body = response.bodyAsText()

    // Then: JSON 형식이 올바르게 직렬화됨
    assertTrue(body.contains("\"message\""))
    assertTrue(body.contains("\"code\""))
    assertTrue(body.contains("\"timestamp\""))
    assertTrue(body.contains("\"path\""))
    assertTrue(body.contains("404"))
    assertTrue(body.contains("/test-path"))
  }

  /**
   * POST 요청에서도 올바른 경로가 ErrorResponse에 포함되는지 테스트
   */
  @Test
  fun `ErrorResponse should include correct path for POST requests`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
      routing {
        post("/test/post-error") {
          throw RuntimeException("POST error")
        }
      }
    }

    // When: POST 요청에서 예외 발생
    val response = client.post("/test/post-error") {
      contentType(ContentType.Application.Json)
      setBody("{}")
    }

    // Then: ErrorResponse에 POST 경로가 포함됨
    assertEquals(HttpStatusCode.InternalServerError, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("/test/post-error"))
  }

  /**
   * 쿼리 파라미터가 포함된 경로도 ErrorResponse에 올바르게 표시되는지 테스트
   */
  @Test
  fun `ErrorResponse should include query parameters in path`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureStatusPages()
      routing {
        get("/test/with-params") {
          throw RuntimeException("Error with params")
        }
      }
    }

    // When: 쿼리 파라미터가 있는 요청에서 예외 발생
    val response = client.get("/test/with-params?id=123&name=test")

    // Then: ErrorResponse에 쿼리 파라미터 포함된 전체 경로 표시
    assertEquals(HttpStatusCode.InternalServerError, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("/test/with-params"))
    assertTrue(body.contains("id=123"))
    assertTrue(body.contains("name=test"))
  }
}
