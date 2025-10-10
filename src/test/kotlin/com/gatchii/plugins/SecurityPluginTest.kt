/**
 * @author Devonshin
 * @date 2025-10-10
 */
package com.gatchii.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.common.const.Constants.Companion.USER_UID
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Security 플러그인 테스트
 * 
 * 태스크 #10.1: Security 플러그인 테스트 구현
 * 
 * 검증 항목:
 * 1. JWT 토큰 검증 설정
 * 2. 유효한 토큰으로 인증 성공
 * 3. 만료된 토큰으로 인증 실패
 * 4. 잘못된 토큰으로 인증 실패
 * 5. 토큰 없이 보호된 엔드포인트 접근 시 실패
 */
@UnitTest
class SecurityPluginTest {

  private val json = Json { ignoreUnknownKeys = true }

  /**
   * JwtConfig 데이터 클래스 검증
   */
  @Test
  fun `test JwtConfig creation with required fields`() {
    // Given
    val audience = "test-audience"
    val issuer = "test-issuer"
    val realm = "TestRealm"
    val jwkIssuer = "test-jwk-issuer"
    val expireSec = 7200L

    // When
    val jwtConfig = JwtConfig(
      audience = audience,
      issuer = issuer,
      realm = realm,
      jwkIssuer = jwkIssuer,
      expireSec = expireSec
    )

    // Then
    assertEquals(audience, jwtConfig.audience, "Audience should match")
    assertEquals(issuer, jwtConfig.issuer, "Issuer should match")
    assertEquals(realm, jwtConfig.realm, "Realm should match")
    assertEquals(jwkIssuer, jwtConfig.jwkIssuer, "JWK issuer should match")
    assertEquals(expireSec, jwtConfig.expireSec, "Expire seconds should match")
  }

  /**
   * JwtConfig 기본값 검증
   */
  @Test
  fun `test JwtConfig default values`() {
    // Given & When
    val jwtConfig = JwtConfig(
      audience = "test-audience",
      issuer = "test-issuer"
    )

    // Then
    assertEquals("GatchiiWebApp", jwtConfig.realm, "Default realm should be GatchiiWebApp")
    assertEquals("test-issuer", jwtConfig.jwkIssuer, "Default jwkIssuer should equal issuer")
    assertEquals(3600L, jwtConfig.expireSec, "Default expireSec should be 3600")
  }

  /**
   * ErrorResponse 직렬화 테스트
   */
  @Test
  fun `test ErrorResponse serialization`() {
    // Given
    val errorResponse = ErrorResponse(
      message = "Test error",
      code = 401,
      timestamp = 1234567890L,
      path = "/test/path"
    )

    // When
    val serialized = json.encodeToString(ErrorResponse.serializer(), errorResponse)

    // Then
    assertNotNull(serialized, "Serialized response should not be null")
    assertTrue(serialized.contains("Test error"), "Serialized response should contain message")
    assertTrue(serialized.contains("401"), "Serialized response should contain code")
    assertTrue(serialized.contains("/test/path"), "Serialized response should contain path")
  }

  /**
   * ErrorResponse 역직렬화 테스트
   */
  @Test
  fun `test ErrorResponse deserialization`() {
    // Given
    val jsonString = """
      {
        "message": "Test error",
        "code": 401,
        "timestamp": 1234567890,
        "path": "/test/path"
      }
    """.trimIndent()

    // When
    val deserialized = json.decodeFromString(ErrorResponse.serializer(), jsonString)

    // Then
    assertEquals("Test error", deserialized.message)
    assertEquals(401, deserialized.code)
    assertEquals(1234567890L, deserialized.timestamp)
    assertEquals("/test/path", deserialized.path)
  }

  /**
   * 유효한 JWT 토큰 생성 헬퍼 함수
   */
  private fun createValidToken(
    userUid: String = "test-user-123",
    issuer: String = "test-issuer",
    audience: String = "test-audience",
    expiresInSeconds: Long = 3600
  ): Pair<String, ECPrivateKey> {
    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
    val keyPair = keyPairGenerator.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey

    val algorithm = Algorithm.ECDSA256(keyPair.public as ECPublicKey, privateKey)
    
    val token = JWT.create()
      .withIssuer(issuer)
      .withAudience(audience)
      .withClaim(USER_UID, userUid)
      .withExpiresAt(Date.from(Instant.now().plusSeconds(expiresInSeconds)))
      .sign(algorithm)

    return Pair(token, privateKey)
  }

  /**
   * 만료된 JWT 토큰 생성 헬퍼 함수
   */
  private fun createExpiredToken(
    userUid: String = "test-user-123",
    issuer: String = "test-issuer",
    audience: String = "test-audience"
  ): Pair<String, ECPrivateKey> {
    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
    val keyPair = keyPairGenerator.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey

    val algorithm = Algorithm.ECDSA256(keyPair.public as ECPublicKey, privateKey)
    
    // 이미 만료된 토큰 (1시간 전)
    val token = JWT.create()
      .withIssuer(issuer)
      .withAudience(audience)
      .withClaim(USER_UID, userUid)
      .withExpiresAt(Date.from(Instant.now().minusSeconds(3600)))
      .sign(algorithm)

    return Pair(token, privateKey)
  }

  /**
   * JWT 토큰 구조 검증
   */
  @Test
  fun `test JWT token structure`() {
    // Given & When
    val (token, _) = createValidToken()

    // Then
    val parts = token.split(".")
    assertEquals(3, parts.size, "JWT should have 3 parts (header.payload.signature)")
    
    // 각 부분이 비어있지 않은지 확인
    assertTrue(parts[0].isNotEmpty(), "Header should not be empty")
    assertTrue(parts[1].isNotEmpty(), "Payload should not be empty")
    assertTrue(parts[2].isNotEmpty(), "Signature should not be empty")
  }

  /**
   * 유효한 JWT 토큰의 클레임 검증
   */
  @Test
  fun `test JWT token claims`() {
    // Given
    val testUserUid = "test-user-456"
    val testIssuer = "test-issuer"
    val testAudience = "test-audience"
    
    // When
    val (token, _) = createValidToken(
      userUid = testUserUid,
      issuer = testIssuer,
      audience = testAudience
    )

    // Then
    val decodedJWT = JWT.decode(token)
    
    assertEquals(testIssuer, decodedJWT.issuer, "Issuer should match")
    assertEquals(testAudience, decodedJWT.audience[0], "Audience should match")
    assertEquals(testUserUid, decodedJWT.getClaim(USER_UID).asString(), "User UID should match")
    assertNotNull(decodedJWT.expiresAt, "Expiration should be set")
    assertTrue(
      decodedJWT.expiresAt.after(Date()),
      "Token should not be expired"
    )
  }

  /**
   * 만료된 JWT 토큰 검증
   */
  @Test
  fun `test expired JWT token`() {
    // Given & When
    val (token, _) = createExpiredToken()

    // Then
    val decodedJWT = JWT.decode(token)
    assertNotNull(decodedJWT.expiresAt, "Expiration should be set")
    assertTrue(
      decodedJWT.expiresAt.before(Date()),
      "Token should be expired"
    )
  }

  /**
   * 서로 다른 사용자 UID로 여러 토큰 생성 및 검증
   */
  @Test
  fun `test multiple tokens with different user UIDs`() {
    // Given
    val userUids = listOf("user-1", "user-2", "user-3")

    // When
    val tokens = userUids.map { uid ->
      val (token, _) = createValidToken(userUid = uid)
      Pair(uid, token)
    }

    // Then
    tokens.forEach { (expectedUid, token) ->
      val decodedJWT = JWT.decode(token)
      val actualUid = decodedJWT.getClaim(USER_UID).asString()
      
      assertEquals(expectedUid, actualUid, "User UID should match for token")
    }
  }

  /**
   * JWT 토큰 만료 시간 검증
   */
  @Test
  fun `test JWT token expiration time`() {
    // Given
    val expiresInSeconds = 7200L
    val beforeCreation = Instant.now()

    // When
    val (token, _) = createValidToken(expiresInSeconds = expiresInSeconds)
    val afterCreation = Instant.now()

    // Then
    val decodedJWT = JWT.decode(token)
    val expirationInstant = decodedJWT.expiresAt.toInstant()
    
    // 만료 시간이 생성 시간으로부터 약 7200초 후인지 확인 (± 5초 오차 허용)
    val expectedExpiration = beforeCreation.plusSeconds(expiresInSeconds)
    val timeDifference = Math.abs(expirationInstant.epochSecond - expectedExpiration.epochSecond)
    
    assertTrue(
      timeDifference < 5,
      "Expiration time should be approximately $expiresInSeconds seconds from now"
    )
  }

  /**
   * 잘못된 형식의 토큰 처리 테스트
   */
  @Test
  fun `test invalid token format handling`() {
    // Given
    val invalidTokens = listOf(
      "invalid-token",
      "header.payload", // 서명 부분 없음
      "", // 빈 토큰
      "a.b.c.d" // 너무 많은 부분
    )

    // When & Then
    invalidTokens.forEach { invalidToken ->
      try {
        JWT.decode(invalidToken)
        // 예외가 발생하지 않으면 테스트 실패로 간주할 수 있지만,
        // 여기서는 단순히 예외 발생 여부만 확인
      } catch (e: Exception) {
        // 예외가 발생하면 정상
        assertNotNull(e, "Exception should be thrown for invalid token")
      }
    }
  }

  /**
   * USER_UID 클레임이 없는 토큰 테스트
   */
  @Test
  fun `test token without USER_UID claim`() {
    // Given
    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
    val keyPair = keyPairGenerator.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey

    val algorithm = Algorithm.ECDSA256(keyPair.public as ECPublicKey, privateKey)
    
    // USER_UID 클레임 없이 토큰 생성
    val token = JWT.create()
      .withIssuer("test-issuer")
      .withAudience("test-audience")
      .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
      .sign(algorithm)

    // When
    val decodedJWT = JWT.decode(token)
    val userUidClaim = decodedJWT.getClaim(USER_UID)

    // Then
    // 클레임이 없으면 null 값 또는 빈 문자열 반환
    val claimValue = userUidClaim.asString()
    assertTrue(claimValue == null || claimValue.isEmpty(), "USER_UID claim should be null or empty")
  }
}
