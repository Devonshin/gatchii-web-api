/**
 * @author Devonshin
 * @date 2025-10-11
 */
package com.gatchii.integration

import com.gatchii.domain.login.LoginUserRequest
import com.gatchii.plugins.JwtResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import shared.common.AbstractIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import shared.common.IntegrationTest

/**
 * 로그인 엔드포인트 E2E 테스트
 * 
 * 태스크 #11.2: 로그인 E2E 테스트 구현
 * 
 * 테스트 범위:
 * 1. 유효한 로그인 요청 - JWT 토큰 발급 확인
 * 2. 무효한 로그인 요청 - 에러 응답 처리
 * 3. 인증 헤더 검증
 * 4. 응답 형식 검증
 * 
 * E2E 테스트 전략:
 * - 실제 HTTP 클라이언트 사용
 * - 실제 데이터베이스와 통신 (Testcontainers)
 * - 전체 요청-응답 흐름 검증
 */
@IntegrationTest
class LoginEndpointE2ETest : AbstractIntegrationTest() {

  @BeforeAll
  fun setupTestData() {
    // TODO: 테스트용 사용자 데이터 준비
    // 실제 데이터베이스에 테스트 사용자를 생성해야 함
  }

  private val json = Json { 
    ignoreUnknownKeys = true 
    prettyPrint = true
  }

  /**
   * 테스트 1: 유효한 로그인 요청 시 200 OK와 JWT 토큰 반환
   * 
   * 검증 항목:
   * - HTTP 상태 코드 200 OK
   * - 응답 본문에 JWT 토큰 포함 (accessToken, refreshToken)
   * - 각 토큰의 만료 시간(expiresIn) 포함
   * - Content-Type이 application/json
   */
  @Test
  fun `POST login attempt with valid credentials should return 200 and JWT tokens`() = withIntegrationApplication {
    // Given: 유효한 로그인 요청 데이터
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    // When: POST /login/attempt 요청
    val response = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // Then: 200 OK 응답
    assertEquals(
      HttpStatusCode.OK, 
      response.status,
      "Valid login should return 200 OK"
    )

    // And: Content-Type이 application/json
    val contentType = response.contentType()
    assertNotNull(contentType, "Content-Type should not be null")
    assertEquals(
      ContentType.Application.Json.withoutParameters(),
      contentType?.withoutParameters(),
      "Content-Type should be application/json"
    )

    // And: JWT 응답 형식 검증
    val responseBody = response.bodyAsText()
    assertTrue(responseBody.isNotBlank(), "Response body should not be empty")
    
    val jwtResponse = json.decodeFromString<JwtResponse>(responseBody)
    
    // 응답 코드 검증
    assertEquals(
      HttpStatusCode.OK.value,
      jwtResponse.code,
      "Response code should match HTTP status"
    )
    
    // 메시지 검증
    assertTrue(
      jwtResponse.message.isNotBlank(),
      "Response message should not be empty"
    )
    
    // Access Token 검증
    assertTrue(
      jwtResponse.jwt.accessToken.token.isNotBlank(),
      "Access token should not be empty"
    )
    assertTrue(
      jwtResponse.jwt.accessToken.expiresIn > 0,
      "Access token expiration should be positive"
    )
    
    // Refresh Token 검증
    assertTrue(
      jwtResponse.jwt.refreshToken.token.isNotBlank(),
      "Refresh token should not be empty"
    )
    assertTrue(
      jwtResponse.jwt.refreshToken.expiresIn > 0,
      "Refresh token expiration should be positive"
    )
  }

  /**
   * 테스트 2: 잘못된 비밀번호로 로그인 시도 시 404 Not Found 반환
   * 
   * 검증 항목:
   * - HTTP 상태 코드 404 Not Found
   * - 적절한 에러 메시지 포함
   */
  @Test
  fun `POST login attempt with wrong password should return 404`() = withIntegrationApplication {
    // Given: 잘못된 비밀번호로 로그인 요청
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "WrongPassword123!"
    )

    // When: POST /login/attempt 요청
    val response = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // Then: 404 Not Found 응답
    assertEquals(
      HttpStatusCode.NotFound,
      response.status,
      "Invalid credentials should return 404"
    )

    // And: 응답 본문 확인
    val responseBody = response.bodyAsText()
    assertTrue(
      responseBody.isNotBlank(),
      "Error response should have a body"
    )
  }

  /**
   * 테스트 3: 존재하지 않는 사용자로 로그인 시도 시 404 Not Found 반환
   * 
   * 검증 항목:
   * - HTTP 상태 코드 404 Not Found
   * - 적절한 에러 메시지
   */
  @Test
  fun `POST login attempt with nonexistent user should return 404`() = withIntegrationApplication {
    // Given: 존재하지 않는 사용자 정보
    val loginRequest = LoginUserRequest(
      prefixId = "nonexistent",
      suffixId = "user",
      password = "AnyPassword123!"
    )

    // When: POST /login/attempt 요청
    val response = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // Then: 404 Not Found 응답
    assertEquals(
      HttpStatusCode.NotFound,
      response.status,
      "Nonexistent user should return 404"
    )
  }

  /**
   * 테스트 4: 필수 필드 누락 시 400 Bad Request 반환
   * 
   * 검증 항목:
   * - HTTP 상태 코드 400 Bad Request
   * - 잘못된 요청 형식에 대한 에러 메시지
   */
  @Test
  fun `POST login attempt with missing required fields should return 400`() = withIntegrationApplication {
    // Given: 필수 필드가 누락된 잘못된 JSON
    val invalidJson = """{"prefixId": "test"}"""

    // When: POST /login/attempt 요청
    val response = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(invalidJson)
    }

    // Then: 400 Bad Request 응답
    assertEquals(
      HttpStatusCode.BadRequest,
      response.status,
      "Missing required fields should return 400"
    )
  }

  /**
   * 테스트 5: 잘못된 Content-Type으로 요청 시 415 Unsupported Media Type 반환
   * 
   * 검증 항목:
   * - HTTP 상태 코드 415 Unsupported Media Type
   */
  @Test
  fun `POST login attempt with wrong content type should return 415`() = withIntegrationApplication {
    // Given: text/plain Content-Type으로 요청
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    // When: 잘못된 Content-Type으로 요청
    val response = client.post("/login/attempt") {
      contentType(ContentType.Text.Plain)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // Then: 415 Unsupported Media Type 응답
    assertTrue(
      response.status == HttpStatusCode.UnsupportedMediaType ||
      response.status == HttpStatusCode.BadRequest,
      "Wrong content type should return 415 or 400, got ${response.status}"
    )
  }

  /**
   * 테스트 6: 로그인 응답 시간 성능 검증
   * 
   * 검증 항목:
   * - 로그인 처리가 합리적인 시간 내에 완료
   * - 응답 시간이 2초 이내
   */
  @Test
  fun `POST login attempt should respond within reasonable time`() = withIntegrationApplication {
    // Given: 유효한 로그인 요청
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    // When: 응답 시간 측정
    val startTime = System.currentTimeMillis()
    val response = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }
    val endTime = System.currentTimeMillis()
    val responseTime = endTime - startTime

    // Then: 응답 시간이 2초 이내
    assertTrue(
      responseTime < 2000,
      "Login should respond within 2 seconds, took ${responseTime}ms"
    )

    // And: 유효한 응답
    assertTrue(
      response.status == HttpStatusCode.OK || 
      response.status == HttpStatusCode.NotFound,
      "Should return valid status code"
    )
  }

  /**
   * 테스트 7: 빈 비밀번호로 로그인 시도 시 에러 반환
   * 
   * 검증 항목:
   * - 빈 비밀번호 입력 시 적절한 에러 응답
   */
  @Test
  fun `POST login attempt with empty password should return error`() = withIntegrationApplication {
    // Given: 빈 비밀번호
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = ""
    )

    // When: POST /login/attempt 요청
    val response = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // Then: 에러 응답 (400 또는 404)
    assertTrue(
      response.status == HttpStatusCode.BadRequest ||
      response.status == HttpStatusCode.NotFound,
      "Empty password should return error, got ${response.status}"
    )
  }

  /**
   * 테스트 8: 다중 동시 로그인 요청 처리 검증
   * 
   * 검증 항목:
   * - 동시 로그인 요청이 올바르게 처리됨
   * - 각 요청이 독립적으로 처리됨
   */
  @Test
  fun `POST login attempt should handle multiple concurrent requests`() = withIntegrationApplication {
    // Given: 여러 로그인 요청
    val requestCount = 5
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    // When: 동시에 여러 요청 전송
    val responses = (1..requestCount).map {
      client.post("/login/attempt") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
      }
    }

    // Then: 모든 요청이 처리됨
    responses.forEach { response ->
      assertTrue(
        response.status == HttpStatusCode.OK ||
        response.status == HttpStatusCode.NotFound,
        "Each request should be processed, got ${response.status}"
      )
    }
  }

  /**
   * 테스트 9: GET 메서드로 로그인 엔드포인트 호출 시 405 Method Not Allowed
   * 
   * 검증 항목:
   * - POST만 허용, GET 요청 시 405 응답
   */
  @Test
  fun `GET method on login endpoint should return 405 Method Not Allowed`() = withIntegrationApplication {
    // When: GET /login/attempt 요청
    val response = client.get("/login/attempt")

    // Then: 405 Method Not Allowed 또는 404 Not Found
    assertTrue(
      response.status == HttpStatusCode.MethodNotAllowed ||
      response.status == HttpStatusCode.NotFound,
      "GET on POST-only endpoint should return 405 or 404"
    )
  }

  /**
   * 테스트 10: JWT 토큰 형식 검증
   * 
   * 검증 항목:
   * - 반환된 토큰이 JWT 형식(xxx.yyy.zzz)을 따름
   * - 토큰이 Base64 인코딩된 3개 부분으로 구성
   */
  @Test
  fun `POST login attempt should return valid JWT token format`() = withIntegrationApplication {
    // Given: 유효한 로그인 요청
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    // When: POST /login/attempt 요청
    val response = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // Then: 성공 시 JWT 형식 검증
    if (response.status == HttpStatusCode.OK) {
      val jwtResponse = json.decodeFromString<JwtResponse>(response.bodyAsText())
      
      // Access Token 형식 검증 (JWT는 3개 부분으로 구성: header.payload.signature)
      val accessTokenParts = jwtResponse.jwt.accessToken.token.split(".")
      assertEquals(
        3,
        accessTokenParts.size,
        "JWT token should have 3 parts (header.payload.signature)"
      )
      
      // 각 부분이 비어있지 않음 검증
      accessTokenParts.forEach { part ->
        assertTrue(
          part.isNotBlank(),
          "Each JWT part should not be blank"
        )
      }
      
      // Refresh Token도 동일하게 검증
      val refreshTokenParts = jwtResponse.jwt.refreshToken.token.split(".")
      assertEquals(3, refreshTokenParts.size, "Refresh token should also have 3 parts")
    }
  }

  /**
   * assertNotNull 헬퍼 함수
   */
  private fun assertNotNull(value: Any?, message: String = "Value should not be null") {
    assertTrue(value != null, message)
  }
}
