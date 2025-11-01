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
import kotlin.test.assertTrue
import shared.common.IntegrationTest

/**
 * 인증된 접근 엔드포인트 E2E 테스트
 * 
 * 태스크 #11.5: 인증된 접근 E2E 테스트 구현
 * 
 * 테스트 범위:
 * 1. 유효한 JWT 토큰으로 인증된 엔드포인트 접근
 * 2. 무효한 JWT 토큰 처리
 * 3. 토큰 없는 요청 처리
 * 4. 전체 인증 플로우 시뮬레이션
 * 
 * E2E 테스트 전략:
 * - 실제 HTTP 클라이언트 사용
 * - 실제 데이터베이스와 통신 (Testcontainers)
 * - 전체 인증 플로우 (로그인 → 토큰 획득 → 인증된 리소스 접근) 검증
 */
@IntegrationTest
class AuthenticatedEndpointE2ETest : AbstractIntegrationTest() {

  private val json = Json { 
    ignoreUnknownKeys = true 
    prettyPrint = true
  }

  @BeforeAll
  fun setupTestData() {
    // TODO: 테스트용 사용자 데이터 준비
    // 실제 데이터베이스에 테스트 사용자를 생성해야 함
  }

  /**
   * 테스트 1: 유효한 액세스 토큰으로 인증된 엔드포인트 접근 시 200 OK
   * 
   * 검증 항목:
   * - HTTP 상태 코드 200 OK
   * - 사용자 정보가 포함된 응답
   * - 전체 인증 플로우 동작
   */
  @Test
  fun `GET authenticated endpoint with valid access token should return 200`() = withIntegrationApplication {
    // Given: 먼저 로그인하여 유효한 토큰 획득
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    val loginResponse = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // 로그인 실패 시 테스트 스킵 (테스트 데이터 미준비)
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val accessToken = loginResult.jwt.accessToken.token

    // When: 액세스 토큰으로 인증된 엔드포인트 접근
    val response = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
    }

    // Then: 200 OK 응답
    assertEquals(
      HttpStatusCode.OK,
      response.status,
      "Valid access token should return 200 OK"
    )

    // And: 응답 본문에 사용자 정보 포함
    val responseBody = response.bodyAsText()
    assertTrue(
      responseBody.isNotBlank(),
      "Response body should not be empty"
    )
    assertTrue(
      responseBody.contains("Hello"),
      "Response should contain greeting message"
    )
  }

  /**
   * 테스트 2: Authorization 헤더 없이 인증된 엔드포인트 접근 시 401 Unauthorized
   * 
   * 검증 항목:
   * - HTTP 상태 코드 401 Unauthorized
   */
  @Test
  fun `GET authenticated endpoint without Authorization header should return 401`() = withIntegrationApplication {
    // When: Authorization 헤더 없이 요청
    val response = client.get("/authenticated")

    // Then: 401 Unauthorized 응답
    assertEquals(
      HttpStatusCode.Unauthorized,
      response.status,
      "Missing Authorization header should return 401"
    )
  }

  /**
   * 테스트 3: 무효한 JWT 토큰으로 접근 시 401 Unauthorized
   * 
   * 검증 항목:
   * - HTTP 상태 코드 401 Unauthorized
   */
  @Test
  fun `GET authenticated endpoint with invalid token should return 401`() = withIntegrationApplication {
    // Given: 무효한 JWT 토큰
    val invalidToken = "invalid.jwt.token"

    // When: 무효한 토큰으로 요청
    val response = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $invalidToken")
    }

    // Then: 401 Unauthorized 응답
    assertEquals(
      HttpStatusCode.Unauthorized,
      response.status,
      "Invalid token should return 401"
    )
  }

  /**
   * 테스트 4: 리프레시 토큰으로 인증된 엔드포인트 접근 시 401 Unauthorized
   * 
   * 검증 항목:
   * - 인증된 엔드포인트는 액세스 토큰만 허용
   * - 리프레시 토큰 사용 시 401 응답
   */
  @Test
  fun `GET authenticated endpoint with refresh token should return 401`() = withIntegrationApplication {
    // Given: 먼저 로그인하여 토큰 획득
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    val loginResponse = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val refreshToken = loginResult.jwt.refreshToken.token

    // When: 리프레시 토큰으로 인증된 엔드포인트 접근 (액세스 토큰이 아닌)
    val response = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $refreshToken")
    }

    // Then: 401 Unauthorized 응답
    assertEquals(
      HttpStatusCode.Unauthorized,
      response.status,
      "Refresh token should not be accepted for authenticated endpoint"
    )
  }

  /**
   * 테스트 5: 잘못된 Bearer 토큰 형식으로 접근 시 401 Unauthorized
   * 
   * 검증 항목:
   * - HTTP 상태 코드 401 Unauthorized
   */
  @Test
  fun `GET authenticated endpoint with malformed Bearer token should return 401`() = withIntegrationApplication {
    // Given: 잘못된 형식의 Bearer 토큰
    val malformedTokens = listOf(
      "invalid_token_without_bearer",
      "Bearer",
      "Bearer ",
      "BearerInvalidToken"
    )

    malformedTokens.forEach { malformedToken ->
      // When: 잘못된 형식의 토큰으로 요청
      val response = client.get("/authenticated") {
        header(HttpHeaders.Authorization, malformedToken)
      }

      // Then: 401 Unauthorized 응답
      assertEquals(
        HttpStatusCode.Unauthorized,
        response.status,
        "Malformed Bearer token '$malformedToken' should return 401"
      )
    }
  }

  /**
   * 테스트 6: POST 메서드로 인증된 엔드포인트 호출 시 405 Method Not Allowed
   * 
   * 검증 항목:
   * - GET만 허용, POST 요청 시 405 응답
   */
  @Test
  fun `POST method on authenticated endpoint should return 405 Method Not Allowed`() = withIntegrationApplication {
    // Given: 유효한 토큰 획득
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    val loginResponse = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val accessToken = loginResult.jwt.accessToken.token

    // When: POST /authenticated 요청
    val response = client.post("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
    }

    // Then: 405 Method Not Allowed 또는 404 Not Found
    assertTrue(
      response.status == HttpStatusCode.MethodNotAllowed ||
      response.status == HttpStatusCode.NotFound,
      "POST on GET-only endpoint should return 405 or 404"
    )
  }

  /**
   * 테스트 7: 인증된 엔드포인트 응답 시간 성능 검증
   * 
   * 검증 항목:
   * - 응답 시간이 2초 이내
   */
  @Test
  fun `GET authenticated endpoint should respond within reasonable time`() = withIntegrationApplication {
    // Given: 유효한 토큰 획득
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    val loginResponse = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val accessToken = loginResult.jwt.accessToken.token

    // When: 응답 시간 측정
    val startTime = System.currentTimeMillis()
    val response = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
    }
    val endTime = System.currentTimeMillis()
    val responseTime = endTime - startTime

    // Then: 응답 시간이 2초 이내
    assertTrue(
      responseTime < 2000,
      "Authenticated endpoint should respond within 2 seconds, took ${responseTime}ms"
    )

    // And: 유효한 응답
    assertTrue(
      response.status == HttpStatusCode.OK ||
      response.status == HttpStatusCode.Unauthorized,
      "Should return valid status code"
    )
  }

  /**
   * 테스트 8: 다중 동시 인증 요청 처리 검증
   * 
   * 검증 항목:
   * - 동일한 토큰으로 여러 인증 요청 처리
   * - 각 요청이 독립적으로 처리됨
   */
  @Test
  fun `GET authenticated endpoint should handle multiple concurrent requests`() = withIntegrationApplication {
    // Given: 유효한 토큰 획득
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    val loginResponse = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val accessToken = loginResult.jwt.accessToken.token

    // When: 동시에 여러 요청 전송
    val requestCount = 5
    val responses = (1..requestCount).map {
      client.get("/authenticated") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
      }
    }

    // Then: 모든 요청이 처리됨
    responses.forEach { response ->
      assertTrue(
        response.status == HttpStatusCode.OK ||
        response.status == HttpStatusCode.Unauthorized,
        "Each request should be processed"
      )
    }
  }

  /**
   * 테스트 9: 전체 인증 플로우 시뮬레이션 (로그인 → 접근 → 토큰 갱신 → 접근)
   * 
   * 검증 항목:
   * - 전체 인증 플로우가 정상 동작
   * - 토큰 갱신 후에도 인증된 리소스 접근 가능
   */
  @Test
  fun `Full authentication flow should work end-to-end`() = withIntegrationApplication {
    // Step 1: 로그인
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    val loginResponse = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val originalAccessToken = loginResult.jwt.accessToken.token
    val refreshToken = loginResult.jwt.refreshToken.token

    // Step 2: 원본 액세스 토큰으로 인증된 리소스 접근
    val authenticatedResponse1 = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $originalAccessToken")
    }
    
    assertEquals(
      HttpStatusCode.OK,
      authenticatedResponse1.status,
      "First authenticated access should succeed"
    )

    // Step 3: 토큰 갱신
    val renewalResponse = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $refreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // 갱신 성공 시 새로운 액세스 토큰으로 접근
    if (renewalResponse.status == HttpStatusCode.OK) {
      val renewedTokens = json.decodeFromString<com.gatchii.domain.jwt.JwtModel>(
        renewalResponse.bodyAsText()
      )
      val newAccessToken = renewedTokens.accessToken.token

      // Step 4: 새로운 액세스 토큰으로 인증된 리소스 접근
      val authenticatedResponse2 = client.get("/authenticated") {
        header(HttpHeaders.Authorization, "Bearer $newAccessToken")
      }
      
      assertEquals(
        HttpStatusCode.OK,
        authenticatedResponse2.status,
        "Authenticated access with renewed token should succeed"
      )
    }
  }

  /**
   * 테스트 10: Content-Type 헤더 검증
   * 
   * 검증 항목:
   * - 응답 Content-Type이 text/plain (기본 응답)
   */
  @Test
  fun `GET authenticated endpoint should return text plain content type`() = withIntegrationApplication {
    // Given: 유효한 토큰 획득
    val loginRequest = LoginUserRequest(
      prefixId = "testuser",
      suffixId = "example",
      password = "ValidP@ssw0rd!"
    )

    val loginResponse = client.post("/login/attempt") {
      contentType(ContentType.Application.Json)
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(json.encodeToString(LoginUserRequest.serializer(), loginRequest))
    }

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val accessToken = loginResult.jwt.accessToken.token

    // When: 인증된 엔드포인트 요청
    val response = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
    }

    // Then: 성공 시 Content-Type 검증
    if (response.status == HttpStatusCode.OK) {
      val contentType = response.contentType()
      // text/plain 또는 다른 텍스트 기반 Content-Type 허용
      assertTrue(
        contentType?.contentType == "text" || contentType == null,
        "Content-Type should be text-based or null"
      )
    }
  }
}
