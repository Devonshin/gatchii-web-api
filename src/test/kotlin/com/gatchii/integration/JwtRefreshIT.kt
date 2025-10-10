/**
 * @author Devonshin
 * @date 2025-10-09
 */
package com.gatchii.integration

import com.gatchii.common.const.Constants.Companion.USER_UID
import com.gatchii.domain.jwk.JwkModel
import com.gatchii.domain.jwk.JwkService
import com.gatchii.domain.jwt.JwtModel
import com.gatchii.domain.jwt.JwtService
import com.gatchii.domain.jwt.JwtServiceImpl
import com.gatchii.domain.jwt.RefreshTokenService
import com.gatchii.domain.jwt.refreshTokenRoute
import com.gatchii.domain.main.mainRoute
import com.gatchii.plugins.JwtConfig
import com.gatchii.plugins.securitySetup
import com.typesafe.config.ConfigFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.logging.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.UnitTest
import shared.common.setupCommonApp
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * JWT Refresh Token 통합 테스트
 * 
 * 토큰 갱신 플로우의 전체 시나리오를 검증합니다:
 * - 유효한 Refresh Token으로 새로운 Access Token과 Refresh Token 발급
 * - 갱신된 토큰의 유효성 검증
 * - 갱신 전후 토큰이 다름을 확인
 */
@UnitTest
class JwtRefreshIT {

  companion object {
    private val logger: Logger = KtorSimpleLogger("JwtRefreshIT")
    private val jwkServer = TestJwkServer()

    @BeforeAll
    @JvmStatic
    fun init() {
      jwkServer.start()
    }

    @AfterAll
    @JvmStatic
    fun destroy() {
      jwkServer.stop()
    }
  }

  /**
   * HoconApplicationConfig에서 JWT 설정을 추출하는 헬퍼 함수
   */
  private fun HoconApplicationConfig.jwtCfg(prefix: String): JwtConfig = JwtConfig(
    audience = this.config(prefix).property("audience").getString(),
    issuer = this.config(prefix).property("issuer").getString(),
    realm = this.config(prefix).property("realm").getString(),
    jwkIssuer = this.config(prefix).property("jwkIssuer").getString(),
    expireSec = this.config(prefix).property("expireSec").getString().toLong(),
  )

  /**
   * 태스크 #8.1 - 토큰 갱신 성공 시나리오 테스트
   * 
   * 검증 항목:
   * 1. 유효한 Refresh Token으로 /refresh-token/renewal 호출 시 200 응답
   * 2. 새로운 Access Token과 Refresh Token이 발급됨
   * 3. 갱신된 토큰이 이전 토큰과 다름 (토큰이 실제로 새로 생성됨)
   * 4. 갱신된 Access Token으로 인증 보호 엔드포인트 접근 가능
   */
  @Test
  fun `successful token renewal with valid refresh token`() = testApplication {
    // 환경 설정 로드
    environment {
      config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")
    val refreshJwtConfig = cfg.jwtCfg("rfrst")

    // 테스트용 사용자 정보
    val userId = UUID.randomUUID()
    val userRole = "USER"
    val userPrefixId = "testuser"

    // Mock 의존성 설정
    val jwkService: JwkService = mockk()
    val jwtService: JwtService = JwtServiceImpl(accessJwtConfig, jwkService)
    val refreshTokenService: RefreshTokenService = mockk()

    // JWK 키 페어 설정
    val kp = jwkServer.getGeneratedKeyPair()
    val jwkModel = JwkModel(
      publicKey = kp.public.encoded.encodeBase64(),
      privateKey = kp.private.encoded.encodeBase64(),
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    coEvery { jwkService.getRandomJwk() } returns jwkModel
    coEvery { jwkService.getProvider(any()) } returns jwkServer.getJwkProvider()
    coEvery { jwkService.convertAlgorithm(any()) } returns com.auth0.jwt.algorithms.Algorithm.ECDSA256(jwkServer.getJwkProvider())

    // RefreshTokenService Mock 설정
    coEvery { refreshTokenService.config() } returns refreshJwtConfig
    coEvery { refreshTokenService.generate(any()) } coAnswers {
      val claim = arg<Map<String, String>>(0)
      jwtService.generate(claim)
    }

    // 초기 토큰 생성 (로그인 시나리오 시뮬레이션)
    val initialClaim = mapOf(
      USER_UID to userId.toString(),
      "role" to userRole,
      "userId" to userPrefixId
    )
    val initialAccessToken = jwtService.generate(initialClaim)
    val initialRefreshToken = jwtService.generate(mapOf(USER_UID to userId.toString()))

    // renewal() Mock: 새로운 토큰 쌍 생성
    coEvery { refreshTokenService.renewal(any()) } coAnswers {
      val renewedAccessToken = jwtService.generate(initialClaim)
      val renewedRefreshToken = jwtService.generate(mapOf(USER_UID to userId.toString()))
      com.gatchii.utils.JwtHandler.newJwtModel(
        renewedAccessToken,
        accessJwtConfig,
        renewedRefreshToken,
        refreshJwtConfig
      )
    }

    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
          jwt("refresh-jwt") { securitySetup("refresh-jwt", this@jwt, refreshJwtConfig) }
        }
      }
    )
    application {
      routing {
        refreshTokenRoute(refreshTokenService)
        mainRoute() // /authenticated 엔드포인트 포함
      }
    }

    // === 테스트 시작 ===

    // 1) Refresh Token으로 토큰 갱신 요청
    logger.info("Requesting token renewal with initial refresh token")
    val renewalResponse = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $initialRefreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // 2) 응답 검증
    val renewalBody = renewalResponse.bodyAsText()
    logger.info("Renewal response status=${renewalResponse.status}, body=$renewalBody")
    assertEquals(HttpStatusCode.OK, renewalResponse.status, "Token renewal should return 200 OK")

    val renewedTokens = Json.decodeFromString<JwtModel>(renewalBody)

    // 3) 갱신된 토큰이 유효한지 확인
    assertTrue(renewedTokens.accessToken.token.isNotBlank(), "Renewed access token should not be blank")
    assertTrue(renewedTokens.refreshToken.token.isNotBlank(), "Renewed refresh token should not be blank")

    // 4) 갱신된 토큰이 이전 토큰과 다른지 확인 (실제로 새로 생성되었는지)
    assertNotEquals(
      initialAccessToken,
      renewedTokens.accessToken.token,
      "Renewed access token should be different from initial token"
    )
    assertNotEquals(
      initialRefreshToken,
      renewedTokens.refreshToken.token,
      "Renewed refresh token should be different from initial token"
    )

    // 5) 갱신된 Access Token으로 보호된 리소스 접근 가능한지 확인
    logger.info("Testing protected resource with renewed access token")
    val protectedResponse = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer ${renewedTokens.accessToken.token}")
      header(HttpHeaders.Accept, "*/*")
    }

    assertEquals(
      HttpStatusCode.OK,
      protectedResponse.status,
      "Protected resource should be accessible with renewed access token"
    )
    assertTrue(
      protectedResponse.bodyAsText().contains("Hello"),
      "Protected resource response should contain greeting"
    )

    logger.info("✅ Token renewal test completed successfully")
  }

  /**
   * 태스크 #8.2 - 토큰 만료 시나리오 테스트 (1/2)
   * 
   * 검증 항목:
   * 1. 만료된 Refresh Token으로 갱신 시도 시 401 Unauthorized 응답
   * 2. 만료 오류 메시지 확인
   */
  @Test
  fun `token renewal fails with expired refresh token`() = testApplication {
    // 환경 설정 로드
    environment {
      config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")
    // 매우 짧은 만료 시간 설정 (1초)
    val refreshJwtConfig = cfg.jwtCfg("rfrst").copy(expireSec = 1L)

    // 테스트용 사용자 정보
    val userId = UUID.randomUUID()

    // Mock 의존성 설정
    val jwkService: JwkService = mockk()
    val jwtService: JwtService = JwtServiceImpl(accessJwtConfig, jwkService)
    val refreshTokenService: RefreshTokenService = mockk()

    // JWK 키 페어 설정
    val kp = jwkServer.getGeneratedKeyPair()
    val jwkModel = JwkModel(
      publicKey = kp.public.encoded.encodeBase64(),
      privateKey = kp.private.encoded.encodeBase64(),
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    coEvery { jwkService.getRandomJwk() } returns jwkModel
    coEvery { jwkService.getProvider(any()) } returns jwkServer.getJwkProvider()
    coEvery { jwkService.convertAlgorithm(any()) } returns com.auth0.jwt.algorithms.Algorithm.ECDSA256(jwkServer.getJwkProvider())
    coEvery { jwkService.findJwk(any()) } returns jwkModel

    // RefreshTokenService Mock 설정
    coEvery { refreshTokenService.config() } returns refreshJwtConfig

    // 만료된 토큰 생성 (1초 만료)
    val expiredRefreshToken = com.gatchii.utils.JwtHandler.generate(
      jwtId = UUID.randomUUID().toString(),
      claim = mapOf("userUid" to userId.toString()),
      algorithm = com.auth0.jwt.algorithms.Algorithm.ECDSA256(jwkServer.getJwkProvider()),
      jwtConfig = refreshJwtConfig
    )

    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
          jwt("refresh-jwt") { securitySetup("refresh-jwt", this@jwt, refreshJwtConfig) }
        }
      }
    )
    application {
      routing {
        refreshTokenRoute(refreshTokenService)
        mainRoute()
      }
    }

    // === 테스트 시작 ===

    // 1) 토큰 만료를 위해 2초 대기 (만료 시간 1초 + 여유 1초)
    logger.info("Waiting for token to expire...")
    Thread.sleep(2000)

    // 2) 만료된 Refresh Token으로 갱신 시도
    logger.info("Attempting renewal with expired refresh token")
    val renewalResponse = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer $expiredRefreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }

    // 3) 응답 검증: 401 Unauthorized 기대
    val renewalBody = renewalResponse.bodyAsText()
    logger.info("Expired token renewal response status=${renewalResponse.status}, body=$renewalBody")
    
    assertEquals(
      HttpStatusCode.Unauthorized,
      renewalResponse.status,
      "Expired refresh token should result in 401 Unauthorized"
    )

    // 4) 오류 메시지 검증 - Unauthorized 메시지가 포함됨
    assertTrue(
      renewalBody.contains("Unauthorized") || renewalBody.contains("expired") || renewalBody.contains("invalid"),
      "Response should contain Unauthorized, expiration or invalid token message. Actual body: $renewalBody"
    )

    logger.info("✅ Expired refresh token test completed successfully")
  }

  /**
   * 태스크 #8.2 - 토큰 만료 시나리오 테스트 (2/2)
   * 
   * 검증 항목:
   * 1. 만료된 Access Token으로 보호된 리소스 접근 시 401 Unauthorized 응답
   * 2. 만료 오류 메시지 확인
   */
  @Test
  fun `protected resource access fails with expired access token`() = testApplication {
    // 환경 설정 로드
    environment {
      config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    // 매우 짧은 만료 시간 설정 (1초)
    val accessJwtConfig = cfg.jwtCfg("jwt").copy(expireSec = 1L)
    val refreshJwtConfig = cfg.jwtCfg("rfrst")

    // 테스트용 사용자 정보
    val userId = UUID.randomUUID()
    val userRole = "USER"
    val userPrefixId = "testuser"

    // Mock 의존성 설정
    val jwkService: JwkService = mockk()
    val jwtService: JwtService = JwtServiceImpl(accessJwtConfig, jwkService)
    val refreshTokenService: RefreshTokenService = mockk()

    // JWK 키 페어 설정
    val kp = jwkServer.getGeneratedKeyPair()
    val jwkModel = JwkModel(
      publicKey = kp.public.encoded.encodeBase64(),
      privateKey = kp.private.encoded.encodeBase64(),
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    coEvery { jwkService.getRandomJwk() } returns jwkModel
    coEvery { jwkService.getProvider(any()) } returns jwkServer.getJwkProvider()
    coEvery { jwkService.convertAlgorithm(any()) } returns com.auth0.jwt.algorithms.Algorithm.ECDSA256(jwkServer.getJwkProvider())
    coEvery { jwkService.findJwk(any()) } returns jwkModel

    // RefreshTokenService Mock 설정
    coEvery { refreshTokenService.config() } returns refreshJwtConfig

    // 만료될 Access Token 생성 (1초 만료)
    val expiredAccessToken = com.gatchii.utils.JwtHandler.generate(
      jwtId = UUID.randomUUID().toString(),
      claim = mapOf(
        USER_UID to userId.toString(),
        "role" to userRole,
        "userId" to userPrefixId
      ),
      algorithm = com.auth0.jwt.algorithms.Algorithm.ECDSA256(jwkServer.getJwkProvider()),
      jwtConfig = accessJwtConfig
    )

    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
          jwt("refresh-jwt") { securitySetup("refresh-jwt", this@jwt, refreshJwtConfig) }
        }
      }
    )
    application {
      routing {
        refreshTokenRoute(refreshTokenService)
        mainRoute()
      }
    }

    // === 테스트 시작 ===

    // 1) 토큰 만료를 위해 2초 대기 (만료 시간 1초 + 여유 1초)
    logger.info("Waiting for access token to expire...")
    Thread.sleep(2000)

    // 2) 만료된 Access Token으로 보호된 리소스 접근 시도
    logger.info("Attempting to access protected resource with expired access token")
    val protectedResponse = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $expiredAccessToken")
      header(HttpHeaders.Accept, "*/*")
    }

    // 3) 응답 검증: 401 Unauthorized 기대
    val protectedBody = protectedResponse.bodyAsText()
    logger.info("Expired access token response status=${protectedResponse.status}, body=$protectedBody")
    
    assertEquals(
      HttpStatusCode.Unauthorized,
      protectedResponse.status,
      "Expired access token should result in 401 Unauthorized"
    )

    // 4) 오류 메시지 검증 - Unauthorized 메시지가 포함됨
    assertTrue(
      protectedBody.contains("Unauthorized") || protectedBody.contains("expired") || protectedBody.contains("invalid"),
      "Response should contain Unauthorized, expiration or invalid token message. Actual body: $protectedBody"
    )

    logger.info("✅ Expired access token test completed successfully")
  }

  /**
   * 태스크 #8.3 - 실제 시간 기반 만료 테스트
   * 
   * 참고: 태스크 #8.2에서 이미 실제 시간 기반 만료 테스트를 구현했습니다.
   * - `token renewal fails with expired refresh token`: 1초 만료, 2초 대기 후 테스트
   * - `protected resource access fails with expired access token`: 1초 만료, 2초 대기 후 테스트
   * 
   * 두 테스트 모두 Thread.sleep()을 사용하여 실제 시간이 경과하도록 하고,
   * 만료된 토큰으로 요청 시 401 Unauthorized 응답을 검증합니다.
   * 
   * 따라서 태스크 #8.3은 태스크 #8.2에서 이미 달성되었습니다.
   */

  /**
   * 태스크 #8.4 - Mock 시간 활용 테스트 (과거 시간으로 토큰 생성)
   * 
   * 검증 항목:
   * 1. 과거 시간으로 토큰을 생성하여 즉시 만료된 상태를 시뮬레이션
   * 2. 실제 대기 시간 없이 빠르게 만료 시나리오 테스트
   * 3. 만료된 토큰으로 요청 시 401 Unauthorized 응답
   * 
   * Mock 시간 개념:
   * - 실제 시간 기반 테스트는 Thread.sleep()으로 대기 시간이 필요
   * - Mock 시간 테스트는 과거 시간으로 토큰을 생성하여 즉시 테스트 가능
   * - JWT 라이브러리를 직접 사용하여 issuedAt과 expiresAt을 과거로 설정
   */
  @Test
  fun `expired token created with past time fails immediately without waiting`() = testApplication {
    // 환경 설정 로드
    environment {
      config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")
    val refreshJwtConfig = cfg.jwtCfg("rfrst")

    // 테스트용 사용자 정보
    val userId = UUID.randomUUID()
    val userRole = "USER"
    val userPrefixId = "testuser"

    // Mock 의존성 설정
    val jwkService: JwkService = mockk()
    val refreshTokenService: RefreshTokenService = mockk()

    // JWK 키 페어 설정
    val kp = jwkServer.getGeneratedKeyPair()
    val jwkModel = JwkModel(
      publicKey = kp.public.encoded.encodeBase64(),
      privateKey = kp.private.encoded.encodeBase64(),
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    coEvery { jwkService.getRandomJwk() } returns jwkModel
    coEvery { jwkService.getProvider(any()) } returns jwkServer.getJwkProvider()
    coEvery { jwkService.convertAlgorithm(any()) } returns com.auth0.jwt.algorithms.Algorithm.ECDSA256(jwkServer.getJwkProvider())
    coEvery { jwkService.findJwk(any()) } returns jwkModel

    // RefreshTokenService Mock 설정
    coEvery { refreshTokenService.config() } returns refreshJwtConfig

    // Mock 시간: 과거 시간 (10분 전)으로 토큰 생성
    val pastTime = OffsetDateTime.now().minusMinutes(10)
    val expiredAccessToken = com.auth0.jwt.JWT.create()
      .withAudience(accessJwtConfig.audience)
      .withIssuer(accessJwtConfig.issuer)
      .withKeyId(jwkModel.id.toString())
      .withIssuedAt(pastTime.toInstant())  // 과거 시간으로 발급
      .withExpiresAt(pastTime.plusSeconds(accessJwtConfig.expireSec).toInstant())  // 이미 만료됨
      .withJWTId(UUID.randomUUID().toString())
      .withClaim(USER_UID, userId.toString())
      .withClaim("role", userRole)
      .withClaim("userId", userPrefixId)
      .sign(com.auth0.jwt.algorithms.Algorithm.ECDSA256(jwkServer.getJwkProvider()))

    logger.info(
      "Created token with past time: issuedAt=${pastTime}, " +
          "expiresAt=${pastTime.plusSeconds(accessJwtConfig.expireSec)}, " +
          "current time=${OffsetDateTime.now()}"
    )

    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
          jwt("refresh-jwt") { securitySetup("refresh-jwt", this@jwt, refreshJwtConfig) }
        }
      }
    )
    application {
      routing {
        refreshTokenRoute(refreshTokenService)
        mainRoute()
      }
    }

    // === 테스트 시작 ===

    // 1) 과거 시간으로 생성된 토큰으로 즉시 접근 시도 (대기 시간 없음)
    logger.info("Testing with token created with past time (no waiting required)")
    val protectedResponse = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $expiredAccessToken")
      header(HttpHeaders.Accept, "*/*")
    }

    // 2) 응답 검증: 401 Unauthorized 기대
    val protectedBody = protectedResponse.bodyAsText()
    logger.info("Response status=${protectedResponse.status}, body=$protectedBody")

    assertEquals(
      HttpStatusCode.Unauthorized,
      protectedResponse.status,
      "Token created with past time should be expired immediately"
    )

    // 3) 오류 메시지 검증
    assertTrue(
      protectedBody.contains("Unauthorized") || protectedBody.contains("expired") || protectedBody.contains("invalid"),
      "Response should contain Unauthorized, expiration or invalid token message. Actual body: $protectedBody"
    )

    logger.info("✅ Mock time (past time) test completed successfully without any waiting time")
  }
}
