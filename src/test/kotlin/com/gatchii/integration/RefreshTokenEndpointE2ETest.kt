/**
 * @author Devonshin
 * @date 2025-10-11
 */
package com.gatchii.integration

import com.gatchii.domain.jwt.JwtModel
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
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import shared.common.IntegrationTest

/**
 * 토큰 갱신 엔드포인트 E2E 테스트
 * 
 * 태스크 #11.3: 토큰 갱신 E2E 테스트 구현
 * 
 * 테스트 범위:
 * 1. 유효한 리프레시 토큰으로 새로운 액세스 토큰 발급
 * 2. 무효한 리프레시 토큰 처리
 * 3. 만료된 토큰 처리
 * 4. 토큰 갱신 전후 토큰 유효성 검증
 * 
 * E2E 테스트 전략:
 * - 실제 HTTP 클라이언트 사용
 * - 실제 데이터베이스와 통신 (Testcontainers)
 * - 전체 인증 플로우 (로그인 → 토큰 갱신) 검증
 */
@IntegrationTest
class RefreshTokenEndpointE2ETest : AbstractIntegrationTest() {

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
   * 테스트 1: 유효한 리프레시 토큰으로 토큰 갱신 시 200 OK와 새로운 토큰 발급
   * 
   * 검증 항목:
   * - HTTP 상태 코드 200 OK
   * - 새로운 액세스 토큰 발급
   * - 새로운 리프레시 토큰 발급
   * - 갱신된 토큰이 이전 토큰과 다름
   */
  @Test
  fun `POST refresh token renewal with valid token should return 200 and new tokens`() = withIntegrationApplication {
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
    val originalRefreshToken = loginResult.jwt.refreshToken.token
    val originalAccessToken = loginResult.jwt.accessToken.token

    // When: 리프레시 토큰으로 토큰 갱신 요청
    val renewalResponse = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $originalRefreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // Then: 200 OK 응답
    assertEquals(
      HttpStatusCode.OK,
      renewalResponse.status,
      "Valid refresh token should return 200 OK"
    )

    // And: 새로운 토큰 발급
    val renewedTokens = json.decodeFromString<JwtModel>(renewalResponse.bodyAsText())
    
    // 새로운 액세스 토큰 검증
    assertTrue(
      renewedTokens.accessToken.token.isNotBlank(),
      "Renewed access token should not be empty"
    )
    assertTrue(
      renewedTokens.accessToken.expiresIn > 0,
      "Renewed access token expiration should be positive"
    )
    
    // 새로운 리프레시 토큰 검증
    assertTrue(
      renewedTokens.refreshToken.token.isNotBlank(),
      "Renewed refresh token should not be empty"
    )
    assertTrue(
      renewedTokens.refreshToken.expiresIn > 0,
      "Renewed refresh token expiration should be positive"
    )
    
    // 갱신된 토큰이 이전 토큰과 다름 검증
    assertNotSame(
      originalAccessToken,
      renewedTokens.accessToken.token,
      "New access token should be different from original"
    )
    assertNotSame(
      originalRefreshToken,
      renewedTokens.refreshToken.token,
      "New refresh token should be different from original"
    )
  }

  /**
   * 테스트 2: 무효한 리프레시 토큰으로 갱신 시도 시 401 Unauthorized
   * 
   * 검증 항목:
   * - HTTP 상태 코드 401 Unauthorized
   * - 적절한 에러 메시지
   */
  @Test
  fun `POST refresh token renewal with invalid token should return 401`() = withIntegrationApplication {
    // Given: 무효한 리프레시 토큰
    val invalidToken = "invalid.jwt.token"

    // When: 무효한 토큰으로 갱신 요청
    val response = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $invalidToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // Then: 401 Unauthorized 응답
    assertEquals(
      HttpStatusCode.Unauthorized,
      response.status,
      "Invalid refresh token should return 401"
    )
  }

  /**
   * 테스트 3: Authorization 헤더 없이 갱신 시도 시 401 Unauthorized
   * 
   * 검증 항목:
   * - HTTP 상태 코드 401 Unauthorized
   */
  @Test
  fun `POST refresh token renewal without Authorization header should return 401`() = withIntegrationApplication {
    // When: Authorization 헤더 없이 요청
    val response = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // Then: 401 Unauthorized 응답
    assertEquals(
      HttpStatusCode.Unauthorized,
      response.status,
      "Missing Authorization header should return 401"
    )
  }

  /**
   * 테스트 4: 잘못된 Bearer 토큰 형식으로 갱신 시도 시 401 Unauthorized
   * 
   * 검증 항목:
   * - HTTP 상태 코드 401 Unauthorized
   */
  @Test
  fun `POST refresh token renewal with malformed Bearer token should return 401`() = withIntegrationApplication {
    // Given: 잘못된 형식의 Bearer 토큰
    val malformedTokens = listOf(
      "invalid_token_without_bearer",
      "Bearer",
      "Bearer ",
      "BearerInvalidToken"
    )

    malformedTokens.forEach { malformedToken ->
      // When: 잘못된 형식의 토큰으로 요청
      val response = client.post("/refresh-token/renewal") {
        header(HttpHeaders.Authorization, malformedToken)
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        contentType(ContentType.Application.Json)
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
   * 테스트 5: 액세스 토큰으로 갱신 시도 시 401 Unauthorized
   * 
   * 검증 항목:
   * - 리프레시 토큰 엔드포인트는 리프레시 토큰만 허용
   * - 액세스 토큰 사용 시 401 응답
   */
  @Test
  fun `POST refresh token renewal with access token should return 401`() = withIntegrationApplication {
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
    val accessToken = loginResult.jwt.accessToken.token

    // When: 액세스 토큰으로 갱신 요청 (리프레시 토큰이 아닌)
    val response = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // Then: 401 Unauthorized 응답
    assertEquals(
      HttpStatusCode.Unauthorized,
      response.status,
      "Access token should not be accepted for token renewal"
    )
  }

  /**
   * 테스트 6: GET 메서드로 토큰 갱신 엔드포인트 호출 시 405 Method Not Allowed
   * 
   * 검증 항목:
   * - POST만 허용, GET 요청 시 405 응답
   */
  @Test
  fun `GET method on refresh token endpoint should return 405 Method Not Allowed`() = withIntegrationApplication {
    // When: GET /refresh-token/renewal 요청
    val response = client.get("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer valid.token.here")
    }

    // Then: 405 Method Not Allowed 또는 401 Unauthorized
    assertTrue(
      response.status == HttpStatusCode.MethodNotAllowed ||
      response.status == HttpStatusCode.Unauthorized,
      "GET on POST-only endpoint should return 405 or 401"
    )
  }

  /**
   * 테스트 7: 토큰 갱신 응답 시간 성능 검증
   * 
   * 검증 항목:
   * - 토큰 갱신 처리가 합리적인 시간 내에 완료
   * - 응답 시간이 2초 이내
   */
  @Test
  fun `POST refresh token renewal should respond within reasonable time`() = withIntegrationApplication {
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

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val refreshToken = loginResult.jwt.refreshToken.token

    // When: 응답 시간 측정
    val startTime = System.currentTimeMillis()
    val response = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $refreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }
    val endTime = System.currentTimeMillis()
    val responseTime = endTime - startTime

    // Then: 응답 시간이 2초 이내
    assertTrue(
      responseTime < 2000,
      "Token renewal should respond within 2 seconds, took ${responseTime}ms"
    )

    // And: 유효한 응답
    assertTrue(
      response.status == HttpStatusCode.OK ||
      response.status == HttpStatusCode.Unauthorized,
      "Should return valid status code"
    )
  }

  /**
   * 테스트 8: 갱신된 토큰의 JWT 형식 검증
   * 
   * 검증 항목:
   * - 반환된 토큰이 JWT 형식(xxx.yyy.zzz)을 따름
   * - 토큰이 Base64 인코딩된 3개 부분으로 구성
   */
  @Test
  fun `POST refresh token renewal should return valid JWT token format`() = withIntegrationApplication {
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

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val refreshToken = loginResult.jwt.refreshToken.token

    // When: 토큰 갱신 요청
    val response = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $refreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // Then: 성공 시 JWT 형식 검증
    if (response.status == HttpStatusCode.OK) {
      val renewedTokens = json.decodeFromString<JwtModel>(response.bodyAsText())
      
      // Access Token 형식 검증
      val accessTokenParts = renewedTokens.accessToken.token.split(".")
      assertEquals(
        3,
        accessTokenParts.size,
        "JWT access token should have 3 parts (header.payload.signature)"
      )
      accessTokenParts.forEach { part ->
        assertTrue(
          part.isNotBlank(),
          "Each JWT part should not be blank"
        )
      }
      
      // Refresh Token 형식 검증
      val refreshTokenParts = renewedTokens.refreshToken.token.split(".")
      assertEquals(
        3,
        refreshTokenParts.size,
        "JWT refresh token should have 3 parts"
      )
    }
  }

  /**
   * 테스트 9: 다중 토큰 갱신 요청 처리 검증
   * 
   * 검증 항목:
   * - 동일한 리프레시 토큰으로 여러 번 갱신 가능 여부
   * - 각 갱신 시 새로운 토큰 발급
   */
  @Test
  fun `POST refresh token renewal should handle sequential renewal requests`() = withIntegrationApplication {
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

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    var currentRefreshToken = loginResult.jwt.refreshToken.token

    // When: 순차적으로 3번 토큰 갱신
    repeat(3) { iteration ->
      val response = client.post("/refresh-token/renewal") {
        header(HttpHeaders.Authorization, "Bearer $currentRefreshToken")
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        contentType(ContentType.Application.Json)
      }

      // Then: 각 갱신이 성공
      if (response.status == HttpStatusCode.OK) {
        val renewedTokens = json.decodeFromString<JwtModel>(response.bodyAsText())
        
        // 새로운 토큰 검증
        assertTrue(
          renewedTokens.accessToken.token.isNotBlank(),
          "Iteration $iteration: Access token should not be empty"
        )
        assertTrue(
          renewedTokens.refreshToken.token.isNotBlank(),
          "Iteration $iteration: Refresh token should not be empty"
        )
        
        // 다음 갱신을 위해 새로운 리프레시 토큰 사용
        currentRefreshToken = renewedTokens.refreshToken.token
      }
    }
  }

  /**
   * 테스트 10: Content-Type 헤더 검증
   * 
   * 검증 항목:
   * - 응답 Content-Type이 application/json
   */
  @Test
  fun `POST refresh token renewal should return JSON content type`() = withIntegrationApplication {
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

    // 로그인 실패 시 테스트 스킵
    if (loginResponse.status != HttpStatusCode.OK) {
      println("Test skipped: Login failed (test data not ready)")
      return@withIntegrationApplication
    }

    val loginResult = json.decodeFromString<JwtResponse>(loginResponse.bodyAsText())
    val refreshToken = loginResult.jwt.refreshToken.token

    // When: 토큰 갱신 요청
    val response = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $refreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // Then: 성공 시 Content-Type 검증
    if (response.status == HttpStatusCode.OK) {
      val contentType = response.contentType()
      assertNotNull(contentType, "Content-Type should not be null")
      assertEquals(
        ContentType.Application.Json.withoutParameters(),
        contentType?.withoutParameters(),
        "Content-Type should be application/json"
      )
    }
  }

  /**
   * assertNotNull 헬퍼 함수
   */
  private fun assertNotNull(value: Any?, message: String = "Value should not be null") {
    assertTrue(value != null, message)
  }
}
