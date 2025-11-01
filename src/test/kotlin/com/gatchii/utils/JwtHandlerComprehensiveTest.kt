package com.gatchii.utils

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.AlgorithmMismatchException
import com.auth0.jwt.exceptions.IncorrectClaimException
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.gatchii.common.utils.ECKeyPairHandler
import com.gatchii.plugins.JwtConfig
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.common.UnitTest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*

/**
 * JWT 생성, 검증, 만료, 서명 유효성 및 필수 클레임 검증을 위한 포괄적인 단위 테스트.
 *
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 2025-01-15
 */

@UnitTest
@DisplayName("JWT Handler 포괄적 테스트")
class JwtHandlerComprehensiveTest {

  private val generateKeyPair = ECKeyPairHandler.generateKeyPair()
  private val algorithm: Algorithm =
    Algorithm.ECDSA256(generateKeyPair.public as ECPublicKey, generateKeyPair.private as ECPrivateKey)

  private val jwtConfig = JwtConfig(
    audience = "testAudience",
    issuer = "testIssuer",
    expireSec = 3600
  )

  @BeforeEach
  fun setUp() {
    unmockkAll()
  }

  @Nested
  @DisplayName("1. JWT 생성 - 유효한 클레임으로 올바르게 생성됨")
  inner class JwtCreationTests {

    @Test
    @DisplayName("기본 클레임으로 JWT 생성")
    fun `JWT는 기본 클레임으로 올바르게 생성되어야 함`() {
      // Given
      val claims = mapOf(
        "username" to "testUser",
        "role" to "admin",
        "userId" to "123"
      )
      val jwtId = UUID.randomUUID().toString()

      // When
      val token = JwtHandler.generate(
        jwtId = jwtId,
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      assertThat(token).isNotBlank()
      assertThat(token.split(".")).hasSize(3) // JWT는 3개 부분으로 구성
    }

    @Test
    @DisplayName("다양한 데이터 타입의 클레임으로 JWT 생성")
    fun `다양한 타입의 클레임을 지원하는 JWT 생성`() {
      // Given
      val claims = mapOf(
        "username" to "testUser",
        "id" to 42,
        "score" to 98.5,
        "isAdmin" to true,
        "createdAt" to Date()
      )
      val jwtId = UUID.randomUUID().toString()

      // When
      val token = JwtHandler.generate(
        jwtId = jwtId,
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      assertThat(token).isNotBlank()
      val decodedJwt = JwtHandler.convert(token)
      assertThat(decodedJwt.getClaim("username").asString()).isEqualTo("testUser")
      assertThat(decodedJwt.getClaim("id").asInt()).isEqualTo(42)
      assertThat(decodedJwt.getClaim("score").asDouble()).isEqualTo(98.5)
      assertThat(decodedJwt.getClaim("isAdmin").asBoolean()).isTrue()
    }

    @Test
    @DisplayName("JWT 필수 클레임 포함 확인")
    fun `생성된 JWT는 필수 클레임을 포함해야 함`() {
      // Given
      val claims = mapOf("username" to "testUser")
      val jwtId = "test-jwt-id-123"

      // When
      val token = JwtHandler.generate(
        jwtId = jwtId,
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      val decodedJwt = JwtHandler.convert(token)
      assertThat(decodedJwt.issuer).isEqualTo(jwtConfig.issuer)
      assertThat(decodedJwt.audience).contains(jwtConfig.audience)
      assertThat(decodedJwt.id).isEqualTo(jwtId)
      assertThat(decodedJwt.issuedAt).isNotNull()
      assertThat(decodedJwt.expiresAt).isNotNull()
    }

    @Test
    @DisplayName("빈 클레임으로도 JWT 생성 가능")
    fun `빈 클레임 맵으로도 올바르게 JWT 생성`() {
      // Given
      val claims = emptyMap<String, Any>()
      val jwtId = UUID.randomUUID().toString()

      // When
      val token = JwtHandler.generate(
        jwtId = jwtId,
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      assertThat(token).isNotBlank()
      val decodedJwt = JwtHandler.convert(token)
      assertThat(decodedJwt.issuer).isEqualTo(jwtConfig.issuer)
      assertThat(decodedJwt.audience).contains(jwtConfig.audience)
    }

    @Test
    @DisplayName("동일한 클레임으로 생성된 JWT는 서로 다른 값")
    fun `동일 클레임으로 생성해도 JWT id가 다르면 서로 다른 토큰 생성`() {
      // Given
      val claims = mapOf("username" to "testUser")

      // When
      val token1 = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )
      val token2 = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      assertThat(token1).isNotEqualTo(token2)
    }
  }

  @Nested
  @DisplayName("2. JWT 검증 - 유효한 JWT는 성공적으로 검증됨")
  inner class JwtVerificationTests {

    @Test
    @DisplayName("유효한 JWT 검증 성공")
    fun `유효한 JWT는 검증에 성공해야 함`() = runTest {
      // Given
      // verify() 함수는 30분 미만 남은 토큰만 검증 가능 (리프레시 토큰용)
      val claims = mapOf("username" to "testUser", "role" to "user")
      val shortLivedConfig = JwtConfig(
        audience = "testAudience",
        issuer = "testIssuer",
        expireSec = 60 * 20 // 20분 (30분 미만)
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = shortLivedConfig
      )

      // When
      val result = JwtHandler.verify(
        token = token,
        algorithm = algorithm,
        jwtConfig = shortLivedConfig
      )

      // Then
      assertThat(result).isTrue()
    }

    @Test
    @DisplayName("올바른 Issuer로 JWT 검증 성공")
    fun `올바른 Issuer를 가진 JWT는 검증에 성공`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val config = JwtConfig(
        audience = "testAudience",
        issuer = "validIssuer",
        expireSec = 60 * 20 // 20분 (리프레시 토큰용: 30분 미만)
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = config
      )

      // When
      val result = JwtHandler.verify(token = token, algorithm = algorithm, jwtConfig = config)

      // Then
      assertThat(result).isTrue()
    }

    @Test
    @DisplayName("올바른 Audience로 JWT 검증 성공")
    fun `올바른 Audience를 가진 JWT는 검증에 성공`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val config = JwtConfig(
        audience = "validAudience",
        issuer = "testIssuer",
        expireSec = 60 * 20 // 20분 (리프레시 토큰용: 30분 미만)
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = config
      )

      // When
      val result = JwtHandler.verify(token = token, algorithm = algorithm, jwtConfig = config)

      // Then
      assertThat(result).isTrue()
    }

    @Test
    @DisplayName("올바른 Algorithm으로 JWT 검증 성공")
    fun `동일한 Algorithm으로 생성되고 검증되는 JWT는 성공`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val shortLivedConfig = JwtConfig(
        audience = jwtConfig.audience,
        issuer = jwtConfig.issuer,
        expireSec = 60 * 20 // 20분 (리프레시 토큰용: 30분 미만)
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = shortLivedConfig
      )

      // When
      val result = JwtHandler.verify(token = token, algorithm = algorithm, jwtConfig = shortLivedConfig)

      // Then
      assertThat(result).isTrue()
    }
  }

  @Nested
  @DisplayName("3. JWT 만료 검증 - 만료된 JWT는 검증 중 거부됨")
  inner class JwtExpirationTests {

    @Test
    @DisplayName("만료된 JWT 검증 실패")
    fun `만료된 JWT는 검증 중 TokenExpiredException 발생`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val expiredConfig = JwtConfig(
        audience = "testAudience",
        issuer = "testIssuer",
        expireSec = -60 // 60초 전에 만료됨
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = expiredConfig
      )

      // When & Then
      assertThrows<TokenExpiredException> {
        JwtHandler.verify(token = token, algorithm = algorithm, jwtConfig = jwtConfig)
      }
    }

    @Test
    @DisplayName("곧 만료될 JWT는 리프레시 임계값 때문에 거부")
    fun `만료까지 30분 이상 남아있는 토큰은 검증 시 JWTVerificationException 발생`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val longLivedConfig = JwtConfig(
        audience = "testAudience",
        issuer = "testIssuer",
        expireSec = 60 * 60 // 1시간
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = longLivedConfig
      )

      // When & Then - 토큰이 30분 이상 유효하면 리프레시 요청이 너무 이름으로 거부
      val shortLivedConfig = JwtConfig(
        audience = "testAudience",
        issuer = "testIssuer",
        expireSec = 60
      )
      assertThrows<com.auth0.jwt.exceptions.JWTVerificationException> {
        JwtHandler.verify(token = token, algorithm = algorithm, jwtConfig = shortLivedConfig)
      }
    }
  }

  @Nested
  @DisplayName("4. JWT 서명 검증 - 잘못된 서명을 가진 JWT는 거부됨")
  inner class JwtSignatureTests {

    @Test
    @DisplayName("서명이 변조된 JWT 검증 실패")
    fun `서명이 변조된 JWT는 JWTVerificationException 발생`() = runTest {
      // Given
      val shortLivedConfig = JwtConfig(
        audience = "testAudience",
        issuer = "testIssuer",
        expireSec = 60 * 20 // 20분 (리프레시 토큰용: 30분 미만)
      )
      val validToken = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = mapOf("username" to "testUser"),
        algorithm = algorithm,
        jwtConfig = shortLivedConfig
      )
      // 서명 부분 변조
      val parts = validToken.split(".")
      val tamperedToken = "${parts[0]}.${parts[1]}.invalidsignature"

      // When & Then - 변조된 서명은 JWTVerificationException을 발생시킴
      assertThrows<com.auth0.jwt.exceptions.JWTVerificationException> {
        JwtHandler.verify(token = tamperedToken, algorithm = algorithm, jwtConfig = shortLivedConfig)
      }
    }

    @Test
    @DisplayName("다른 Algorithm으로 생성된 JWT 검증 실패")
    fun `다른 Algorithm으로 검증하면 JWTVerificationException 발생`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val shortLivedConfig = JwtConfig(
        audience = "testAudience",
        issuer = "testIssuer",
        expireSec = 60 * 20 // 20분 (리프레시 토큰용: 30분 미만)
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = shortLivedConfig
      )
      // 다른 Algorithm으로 검증 시도
      val wrongAlgorithm = Algorithm.HMAC256("wrong-secret-key")

      // When & Then - 알고리즘 미스매치는 JWTVerificationException으로 나타남
      assertThrows<com.auth0.jwt.exceptions.JWTVerificationException> {
        JwtHandler.verify(token = token, algorithm = wrongAlgorithm, jwtConfig = shortLivedConfig)
      }
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패")
    fun `잘못된 형식의 토큰은 JWTDecodeException 발생`() = runTest {
      // Given
      val invalidToken = "invalid.token.format"

      // When & Then
      assertThrows<JWTDecodeException> {
        JwtHandler.verify(token = invalidToken, algorithm = algorithm, jwtConfig = jwtConfig)
      }
    }

    @Test
    @DisplayName("부분적으로 손상된 JWT 검증 실패")
    fun `부분적으로 손상된 JWT는 JWTDecodeException 발생`() = runTest {
      // Given
      val validToken = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = mapOf("username" to "testUser"),
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )
      val parts = validToken.split(".")
      // payload 부분 변조
      val tamperedToken = "${parts[0]}.invalid.${parts[2]}"

      // When & Then
      assertThrows<JWTDecodeException> {
        JwtHandler.verify(token = tamperedToken, algorithm = algorithm, jwtConfig = jwtConfig)
      }
    }
  }

  @Nested
  @DisplayName("5. JWT 필수 클레임 검증 - 필수 클레임이 없는 JWT는 거부됨")
  inner class JwtClaimValidationTests {

    @Test
    @DisplayName("잘못된 Issuer를 가진 JWT 검증 실패")
    fun `잘못된 Issuer를 가진 JWT는 IncorrectClaimException 발생`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val wrongIssuerConfig = JwtConfig(
        audience = "testAudience",
        issuer = "wrongIssuer",
        expireSec = 3600
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = wrongIssuerConfig
      )

      // When & Then
      val correctConfig = JwtConfig(
        audience = "testAudience",
        issuer = "correctIssuer",
        expireSec = 3600
      )
      assertThrows<com.auth0.jwt.exceptions.JWTVerificationException> {
        JwtHandler.verify(token = token, algorithm = algorithm, jwtConfig = correctConfig)
      }
    }

    @Test
    @DisplayName("잘못된 Audience를 가진 JWT 검증 실패")
    fun `잘못된 Audience를 가진 JWT는 IncorrectClaimException 발생`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val wrongAudienceConfig = JwtConfig(
        audience = "wrongAudience",
        issuer = "testIssuer",
        expireSec = 60 * 20 // 리프레시 토큰용: 30분 미만
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = wrongAudienceConfig
      )

      // When & Then
      val correctConfig = JwtConfig(
        audience = "correctAudience",
        issuer = "testIssuer",
        expireSec = 60 * 20
      )
      assertThrows<IncorrectClaimException> {
        JwtHandler.verify(token = token, algorithm = algorithm, jwtConfig = correctConfig)
      }
    }

    @Test
    @DisplayName("JWT ID 클레임 누락 또는 불일치 검증")
    fun `JWT ID가 일치하지 않으면 검증에 영향`() = runTest {
      // Given
      val claims = mapOf("username" to "testUser")
      val jwtId = "specific-jwt-id-123"
      val token = JwtHandler.generate(
        jwtId = jwtId,
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // When
      val decodedJwt = JwtHandler.convert(token)

      // Then
      assertThat(decodedJwt.id).isEqualTo(jwtId)
      assertThat(decodedJwt.id).isNotNull()
    }

    @Test
    @DisplayName("필수 클레임들이 모두 존재하는 JWT 검증")
    fun `모든 필수 클레임을 포함하는 JWT는 검증 성공`() = runTest {
      // Given
      val claims = mapOf(
        "username" to "testUser",
        "role" to "admin",
        "userId" to "12345"
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // When
      val decodedJwt = JwtHandler.convert(token)

      // Then
      assertThat(decodedJwt.issuer).isNotNull()
      assertThat(decodedJwt.audience).isNotNull()
      assertThat(decodedJwt.issuedAt).isNotNull()
      assertThat(decodedJwt.expiresAt).isNotNull()
      assertThat(decodedJwt.id).isNotNull()
    }

    @Test
    @DisplayName("클레임 추출 및 타입 검증")
    fun `추출한 클레임의 타입이 올바르게 유지됨`() = runTest {
      // Given
      val claims = mapOf(
        "username" to "testUser",
        "score" to 95,
        "rating" to 4.5,
        "verified" to true
      )
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // When
      val decodedJwt = JwtHandler.convert(token)

      // Then
      assertThat(decodedJwt.getClaim("username").asString()).isEqualTo("testUser")
      assertThat(decodedJwt.getClaim("score").asInt()).isEqualTo(95)
      assertThat(decodedJwt.getClaim("rating").asDouble()).isEqualTo(4.5)
      assertThat(decodedJwt.getClaim("verified").asBoolean()).isTrue()
    }
  }

  @Nested
  @DisplayName("추가 엣지 케이스 테스트")
  inner class EdgeCaseTests {

    @Test
    @DisplayName("아주 큰 클레임 맵으로 JWT 생성")
    fun `많은 클레임을 포함하는 JWT 생성 및 검증`() = runTest {
      // Given
      val largeClaimsMap = mutableMapOf<String, Any>()
      for (i in 1..50) {
        largeClaimsMap["claim_$i"] = "value_$i"
      }

      // When
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = largeClaimsMap,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      assertThat(token).isNotBlank()
      val decodedJwt = JwtHandler.convert(token)
      assertThat(decodedJwt).isNotNull()
    }

    @Test
    @DisplayName("특수 문자를 포함하는 클레임")
    fun `특수 문자를 포함한 클레임 값으로 JWT 생성`() = runTest {
      // Given
      val claims = mapOf(
        "username" to "test@user.com",
        "email" to "test+tag@example.com",
        "description" to "테스트 사용자 \"특수문자\" 포함"
      )

      // When
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      val decodedJwt = JwtHandler.convert(token)
      assertThat(decodedJwt.getClaim("username").asString()).isEqualTo("test@user.com")
      assertThat(decodedJwt.getClaim("email").asString()).isEqualTo("test+tag@example.com")
    }

    @Test
    @DisplayName("빈 문자열 클레임 값 처리")
    fun `빈 문자열 클레임도 정상 처리`() = runTest {
      // Given
      val claims = mapOf(
        "username" to "",
        "description" to ""
      )

      // When
      val token = JwtHandler.generate(
        jwtId = UUID.randomUUID().toString(),
        claim = claims,
        algorithm = algorithm,
        jwtConfig = jwtConfig
      )

      // Then
      val decodedJwt = JwtHandler.convert(token)
      assertThat(decodedJwt.getClaim("username").asString()).isEmpty()
      assertThat(decodedJwt.getClaim("description").asString()).isEmpty()
    }
  }
}
