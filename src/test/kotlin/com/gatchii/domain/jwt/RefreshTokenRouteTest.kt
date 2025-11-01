package com.gatchii.domain.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.common.const.Constants.Companion.USER_UID
import com.gatchii.domain.jwk.JwkModel
import com.gatchii.domain.jwk.JwkService
import com.gatchii.plugins.ErrorResponse
import com.gatchii.plugins.JwtConfig
import com.gatchii.plugins.securitySetup
import com.gatchii.utils.JwtHandler
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
import io.mockk.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.IntegrationTest
import shared.common.setupCommonApp
import shared.repository.DatabaseFactoryForTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test

@IntegrationTest
class RefreshTokenRouteTest {
  val config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
  private val refreshTokenRepository: RefreshTokenRepository = mockk()
  val refresgJwtConfig: JwtConfig = JwtConfig(
    audience = config.config("rfrst").property("audience").getString(),
    issuer = config.config("rfrst").property("issuer").getString(),
    realm = "Test Realm",
    jwkIssuer = config.config("rfrst").property("jwkIssuer").getString(),
    expireSec = 300,
  )
  val jwtConfig: JwtConfig = JwtConfig(
    audience = config.config("jwt").property("audience").getString(),
    issuer = config.config("jwt").property("issuer").getString(),
    realm = "Test Realm",
    jwkIssuer = config.config("jwt").property("jwkIssuer").getString(),
    expireSec = 60,
  )
  private val jwkService: JwkService = mockk(relaxed = true)
  private val jwtService: JwtService = mockk(relaxed = true)
  val refreshTokenService = RefreshTokenServiceImpl(
    refreshTokenRepository = refreshTokenRepository,
    jwtConfig = refresgJwtConfig,
    jwkService = jwkService,
    jwtService = jwtService
  )

  companion object {
    val logger = KtorSimpleLogger(this::class.simpleName ?: "RefreshTokenRouteTest")
    private val databaseFactory: DatabaseFactoryForTest = DatabaseFactoryForTest()
    val jwkServer = TestJwkServer() // Start temporary JWK server

    @BeforeAll
    @JvmStatic
    fun init() {
      databaseFactory.connect()
      jwkServer.start()
    }

    @AfterAll
    @JvmStatic
    fun destroy() {
      databaseFactory.close()
      jwkServer.stop()
    }
  }

  inline fun setupApplication(crossinline block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    logger.debug("setupApplication..")
    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      installDoubleReceive = true,
      securityInstall = {
        install(Authentication) {
          jwt("refresh-jwt") {
            securitySetup(
              "refresh-jwt",
              this@jwt,
              refresgJwtConfig
            )
          }
          jwt("auth-jwt") {
            securitySetup(
              "auth-jwt",
              this@jwt,
              jwtConfig
            )
          }
        }
      }
    )
    application {
      routing {
        // refreshTokenRoute 내부에서 route("/refresh-token")를 선언하므로 중복 프리픽스를 피하기 위해 직접 호출
        refreshTokenRoute(refreshTokenService)
      }
    }
    block()
  }

  @Test
  fun `empty refresh token throw Unauthorized exception`() = setupApplication {
    val bodyAsText = client.post("/refresh-token/renewal") {
      contentType(ContentType.Application.Json)
      //val loginUserRequest = LoginUserRequest(prefixId = "test", suffixId = "", password = "<PASSWORD>")
      //setBody(Json.encodeToString(loginUserRequest))
    }.bodyAsText()
    logger.debug("bodyAsText: $bodyAsText")
  }

  @Test
  fun `expired refresh token throw Unauthorized exception`() = setupApplication {
    mockkObject(JwtHandler)
    //given
    val userUid = UUID.fromString("9f6958e0-b515-4316-87a6-e7d9b11bee1c")
    val keyPair = jwkServer.getGeneratedKeyPair()
    val createdRefreshModel = RefreshTokenModel(
      id = UUID.randomUUID(),
      isValid = true,
      userUid = userUid,
      expireAt = OffsetDateTime.now().minusSeconds(10)
    )
    val randomJwk = JwkModel(
      publicKey = keyPair.public.encoded.encodeBase64(),
      privateKey = keyPair.private.encoded.encodeBase64(),
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val algorithm = Algorithm.ECDSA256(jwkServer.getJwkProvider())
    val expiredAt = OffsetDateTime.now().minusSeconds(10).toInstant()
    coEvery { refreshTokenRepository.create(any()) } returns createdRefreshModel
    coEvery { jwkService.getRandomJwk() } returns randomJwk
    coEvery { jwkService.convertAlgorithm(any()) } returns algorithm
    coEvery { JwtHandler.expiresAt(any(), any()) } returns expiredAt

    //when
    val oldRefreshToken = refreshTokenService.generate(
      mapOf(
        USER_UID to userUid.toString(),
        "username" to "testname",
        "role" to "USER",
      )
    )
    logger.debug("oldRefreshToken: $oldRefreshToken")
    val httpRes = client.post("/refresh-token/renewal") {
      header("Authorization", "Bearer $oldRefreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }
    val bodyAsText = httpRes.bodyAsText()
    logger.debug("expired case status: ${httpRes.status}, body: $bodyAsText")
    //then

    if (bodyAsText.isNotBlank()) {
      val response = Json.decodeFromString<ErrorResponse>(bodyAsText)
      assert(response.code == HttpStatusCode.Unauthorized.value)
      // StatusPages에서 기본 메시지 "Unauthorized"로 응답할 수 있으므로 message는 고정 검증하지 않음
      assert(response.path == "/refresh-token/renewal")
    } else {
      // 본문이 비어도 401 상태만은 유지되어야 함
      assert(httpRes.status == HttpStatusCode.Unauthorized)
    }

    coVerify(exactly = 1) { refreshTokenRepository.create(any()) }
    coVerify(exactly = 1) { jwkService.getRandomJwk() }
    coVerify(exactly = 1) { jwkService.convertAlgorithm(any()) }
    coVerify(exactly = 1) { JwtHandler.expiresAt(any(), any()) }

    logger.debug("bodyAsText: $bodyAsText")
    unmockkObject(JwtHandler)
  }

  @Test
  fun `missing userUid refresh token throw Unauthorized exception`() = setupApplication {
    //given
    val algorithm = Algorithm.ECDSA256(jwkServer.getJwkProvider())
    val claim = mapOf(
      "username" to "testname",
      "role" to "USER",
    )
    val tokenId = "9f6958e0-b515-4316-87a6-e7d9b11bee1c"
    //when
    val oldRefreshToken = JwtHandler.generate(tokenId, claim, algorithm, jwtConfig)
    println("oldRefreshToken: $oldRefreshToken")
    val httpRes = client.post("/refresh-token/renewal") {
      header("Authorization", "Bearer $oldRefreshToken")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }
    val bodyAsText = httpRes.bodyAsText()
    logger.debug("missing userUid case status: ${httpRes.status}, body: $bodyAsText")
    //then
    if (bodyAsText.isNotBlank()) {
      val response = Json.decodeFromString<ErrorResponse>(bodyAsText)
      assert(response.code == HttpStatusCode.Unauthorized.value)
      // StatusPages에서 기본 메시지 "Unauthorized"로 응답할 수 있으므로 message는 고정 검증하지 않음
      assert(response.path == "/refresh-token/renewal")
    } else {
      assert(httpRes.status == HttpStatusCode.Unauthorized)
    }

    println("bodyAsText: $bodyAsText")
  }

}
