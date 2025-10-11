/**
 * @author Devonshin
 * @date 2025-10-11
 */
package com.gatchii.plugins

import com.gatchii.domain.login.LoginUserRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validation 플러그인 테스트 클래스
 * 
 * RequestValidation 플러그인을 통한 입력 검증 로직을 테스트합니다.
 */
class ValidatingTest {

  /**
   * 유효한 LoginUserRequest 검증 테스트
   */
  @Test
  fun `valid LoginUserRequest should pass validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 유효한 로그인 요청
    val validRequest = LoginUserRequest(
      prefixId = "user",
      suffixId = "example",
      password = "password123"
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(validRequest))
    }

    // Then: 200 OK 응답
    assertEquals(HttpStatusCode.OK, response.status)
  }

  /**
   * 빈 prefixId로 인한 검증 실패 테스트
   */
  @Test
  fun `LoginUserRequest with blank prefixId should fail validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      configureStatusPages()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 빈 prefixId를 가진 로그인 요청
    val invalidRequest = LoginUserRequest(
      prefixId = "",
      suffixId = "example",
      password = "password123"
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(invalidRequest))
    }

    // Then: 400 Bad Request 응답
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("Invalid login parameter"), "Response should contain validation error")
  }

  /**
   * 빈 suffixId로 인한 검증 실패 테스트
   */
  @Test
  fun `LoginUserRequest with blank suffixId should fail validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      configureStatusPages()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 빈 suffixId를 가진 로그인 요청
    val invalidRequest = LoginUserRequest(
      prefixId = "user",
      suffixId = "",
      password = "password123"
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(invalidRequest))
    }

    // Then: 400 Bad Request 응답
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("Invalid login parameter"), "Response should contain validation error")
  }

  /**
   * 빈 password로 인한 검증 실패 테스트
   */
  @Test
  fun `LoginUserRequest with blank password should fail validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      configureStatusPages()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 빈 password를 가진 로그인 요청
    val invalidRequest = LoginUserRequest(
      prefixId = "user",
      suffixId = "example",
      password = ""
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(invalidRequest))
    }

    // Then: 400 Bad Request 응답
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("Invalid login parameter"), "Response should contain validation error")
  }

  /**
   * 모든 필드가 빈 값인 경우 검증 실패 테스트
   */
  @Test
  fun `LoginUserRequest with all blank fields should fail validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      configureStatusPages()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 모든 필드가 빈 값인 로그인 요청
    val invalidRequest = LoginUserRequest(
      prefixId = "",
      suffixId = "",
      password = ""
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(invalidRequest))
    }

    // Then: 400 Bad Request 응답
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("Invalid login parameter"), "Response should contain validation error")
  }

  /**
   * 공백만 있는 필드 검증 실패 테스트
   */
  @Test
  fun `LoginUserRequest with whitespace-only fields should fail validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      configureStatusPages()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 공백만 있는 필드를 가진 로그인 요청
    val invalidRequest = LoginUserRequest(
      prefixId = "   ",
      suffixId = "   ",
      password = "   "
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(invalidRequest))
    }

    // Then: 400 Bad Request 응답
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    assertTrue(body.contains("Invalid login parameter"), "Response should contain validation error")
  }

  /**
   * 최소 유효한 값 검증 통과 테스트
   */
  @Test
  fun `LoginUserRequest with minimal valid values should pass validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 최소 유효한 값을 가진 로그인 요청
    val validRequest = LoginUserRequest(
      prefixId = "a",
      suffixId = "b",
      password = "c"
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(validRequest))
    }

    // Then: 200 OK 응답
    assertEquals(HttpStatusCode.OK, response.status)
  }

  /**
   * 긴 값으로 검증 통과 테스트
   */
  @Test
  fun `LoginUserRequest with long values should pass validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 긴 값을 가진 로그인 요청
    val validRequest = LoginUserRequest(
      prefixId = "a".repeat(100),
      suffixId = "b".repeat(100),
      password = "c".repeat(100)
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(validRequest))
    }

    // Then: 200 OK 응답
    assertEquals(HttpStatusCode.OK, response.status)
  }

  /**
   * 특수 문자 포함 값 검증 통과 테스트
   */
  @Test
  fun `LoginUserRequest with special characters should pass validation`() = testApplication {
    application {
      install(ContentNegotiation) {
        json()
      }
      configureValidation()
      routing {
        post("/test/login") {
          val request = call.receive<LoginUserRequest>()
          call.respond(HttpStatusCode.OK, request)
        }
      }
    }

    // Given: 특수 문자를 포함한 로그인 요청
    val validRequest = LoginUserRequest(
      prefixId = "user@example",
      suffixId = "test#123",
      password = "P@ssw0rd!"
    )

    // When: POST 요청 전송
    val response = client.post("/test/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(validRequest))
    }

    // Then: 200 OK 응답
    assertEquals(HttpStatusCode.OK, response.status)
  }
}
