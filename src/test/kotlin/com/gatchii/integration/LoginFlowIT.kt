package com.gatchii.integration

import com.gatchii.common.const.Constants.Companion.USER_UID
import com.gatchii.domain.jwk.JwkModel
import com.gatchii.domain.jwk.JwkService
import com.gatchii.domain.login.*
import com.gatchii.domain.jwt.JwtService
import com.gatchii.domain.jwt.JwtServiceImpl
import com.gatchii.domain.jwt.RefreshTokenService
import com.gatchii.domain.rsa.RsaModel
import com.gatchii.domain.rsa.RsaService
import com.gatchii.plugins.JwtConfig
import com.gatchii.plugins.JwtResponse
import com.gatchii.plugins.securitySetup
import com.gatchii.domain.main.mainRoute
import com.gatchii.domain.jwt.refreshTokenRoute
import com.typesafe.config.ConfigFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.logging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.statuspages.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.gatchii.common.exception.NotFoundUserException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.UnitTest
import shared.common.setupCommonApp
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@UnitTest
class LoginFlowIT {

  companion object {
    private val logger: Logger = KtorSimpleLogger("LoginFlowIT")
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

  private fun HoconApplicationConfig.jwtCfg(prefix: String): JwtConfig = JwtConfig(
    audience = this.config(prefix).property("audience").getString(),
    issuer = this.config(prefix).property("issuer").getString(),
    realm = this.config(prefix).property("realm").getString(),
    jwkIssuer = this.config(prefix).property("jwkIssuer").getString(),
    expireSec = this.config(prefix).property("expireSec").getString().toLong(),
  )

  @Test
  fun `login success then authenticated 200 and refresh renewal 200`() = testApplication {
    // env
    environment {
      config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")
    val refreshJwtConfig = cfg.jwtCfg("rfrst")

    // Dependencies (partial real + mocks)
    val loginRepository: LoginRepository = mockk()
    val bCrypt = com.gatchii.common.utils.BCryptPasswordEncoder()
    val jwkService: JwkService = mockk()
    val jwtService: JwtService = JwtServiceImpl(accessJwtConfig, jwkService)
    val refreshTokenService: RefreshTokenService = mockk()
    val rsaService: RsaService = mockk()

    // Prepare user
    val userId = UUID.randomUUID()
    val rsaUid = UUID.randomUUID()
    val rawPassword = "P@ssw0rd!"
    val hashed = bCrypt.encode(rawPassword)
    val loginModel = LoginModel(
      id = userId,
      prefixId = "user",
      suffixId = "example",
      password = hashed,
      rsaUid = rsaUid,
      status = LoginStatus.ACTIVE,
      role = UserRole.USER,
      lastLoginAt = OffsetDateTime.now(),
      deletedAt = null,
    )

    // Mocks wiring
    coEvery { loginRepository.findUser("user", "example") } returns loginModel
    // rsaService: encrypt는 IDENTITY로 동작시키기 (토큰 클레임을 단순화)
    coEvery { rsaService.getRsa(rsaUid) } returns RsaModel(
      publicKey = "",
      privateKey = "",
      exponent = "",
      modulus = "",
      createdAt = OffsetDateTime.now(),
      id = rsaUid
    )
    coEvery { rsaService.encrypt(any(), any()) } answers { secondArg<String>() }

    // JWK & Algorithm for JwtServiceImpl
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

    // RefreshTokenService: generate/renewal/config 목킹
    coEvery { refreshTokenService.config() } returns refreshJwtConfig
    coEvery { refreshTokenService.generate(any()) } coAnswers {
      val claim = arg<Map<String, String>>(0)
      jwtService.generate(claim)
    }
    coEvery { refreshTokenService.renewal(any()) } coAnswers {
      val claim = mapOf(
        USER_UID to userId.toString(),
        "role" to loginModel.role.name,
        "userId" to loginModel.prefixId
      )
      val access = jwtService.generate(claim)
      val refresh = jwtService.generate(mapOf(USER_UID to userId.toString()))
      com.gatchii.utils.JwtHandler.newJwtModel(access, accessJwtConfig, refresh, refreshJwtConfig)
    }

    val loginService: LoginService = LoginServiceImpl(loginRepository, bCrypt, jwtService, refreshTokenService, rsaService)

    // Install security and routes
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
        loginRoute(loginService)
        mainRoute()
        refreshTokenRoute(refreshTokenService)
      }
    }

    // 1) Login
    val loginReq = LoginUserRequest(prefixId = "user", suffixId = "example", password = rawPassword)
    val loginHttp = client.post("/login/attempt") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(loginReq))
    }
    val loginResText = loginHttp.bodyAsText()
    println("login status=${loginHttp.status}, body=${loginResText}")
    assertEquals(HttpStatusCode.OK, loginHttp.status)
    val loginRes = Json.decodeFromString<JwtResponse>(loginResText)
    assertEquals(HttpStatusCode.OK.value, loginRes.code)
    assertTrue(loginRes.jwt.accessToken.token.isNotBlank())
    assertTrue(loginRes.jwt.refreshToken.token.isNotBlank())

    // 2) Access protected endpoint with access token
    val authRes = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer ${loginRes.jwt.accessToken.token}")
      header(HttpHeaders.Accept, "*/*")
    }
    assertEquals(HttpStatusCode.OK, authRes.status)

    // 3) Refresh renewal
    val renewalHttp = client.post("/refresh-token/renewal") {
      header(HttpHeaders.Authorization, "Bearer ${loginRes.jwt.refreshToken.token}")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
    }
    val renewalResText = renewalHttp.bodyAsText()
    println("renewal status=${renewalHttp.status}, body=${renewalResText}")
    assertEquals(HttpStatusCode.OK, renewalHttp.status)
    val renewed = Json.decodeFromString<com.gatchii.domain.jwt.JwtModel>(renewalResText)
    assertTrue(renewed.accessToken.token.isNotBlank())
    assertTrue(renewed.refreshToken.token.isNotBlank())
  }

  @Test
  fun `login failure with wrong password returns 404`() = testApplication {
    environment { config = HoconApplicationConfig(ConfigFactory.load("application-test.conf")) }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")

    val loginRepository: LoginRepository = mockk()
    val bCrypt = com.gatchii.common.utils.BCryptPasswordEncoder()
    val jwkService: JwkService = mockk(relaxed = true)
    val jwtService: JwtService = mockk(relaxed = true) // 사용 안 됨
    val refreshTokenService: RefreshTokenService = mockk(relaxed = true) // 사용 안 됨
    val rsaService: RsaService = mockk(relaxed = true)

    val hashed = bCrypt.encode("correct")
    val loginModel = LoginModel(
      id = UUID.randomUUID(),
      prefixId = "user",
      suffixId = "example",
      password = hashed,
      rsaUid = UUID.randomUUID(),
      status = LoginStatus.ACTIVE,
      role = UserRole.USER,
      lastLoginAt = OffsetDateTime.now(),
      deletedAt = null,
    )
    coEvery { loginRepository.findUser("user", "example") } returns loginModel

    val loginService: LoginService = LoginServiceImpl(loginRepository, bCrypt, jwtService, refreshTokenService, rsaService)

    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
        }
      }
    )
    application { routing { loginRoute(loginService) } }

    val loginReq = LoginUserRequest(prefixId = "user", suffixId = "example", password = "wrong")
    val httpRes = client.post("/login/attempt") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(loginReq))
    }
    println("wrong password status=${httpRes.status}, body=${httpRes.bodyAsText()}")
    assertEquals(HttpStatusCode.NotFound, httpRes.status)
  }

  @Test
  fun `login failure with nonexistent user returns 404`() = testApplication {
    environment { config = HoconApplicationConfig(ConfigFactory.load("application-test.conf")) }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")

    val loginRepository: LoginRepository = mockk()
    val bCrypt = com.gatchii.common.utils.BCryptPasswordEncoder()
    val jwkService: JwkService = mockk(relaxed = true)
    val jwtService: JwtService = mockk(relaxed = true)
    val refreshTokenService: RefreshTokenService = mockk(relaxed = true)
    val rsaService: RsaService = mockk(relaxed = true)

    coEvery { loginRepository.findUser("nouser","example") } returns null

    val loginService: LoginService = LoginServiceImpl(loginRepository, bCrypt, jwtService, refreshTokenService, rsaService)

    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
        }
      }
    )
    application { routing { loginRoute(loginService) } }

    val loginReq = LoginUserRequest(prefixId="nouser", suffixId="example", password="any")
    val httpRes = client.post("/login/attempt") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(loginReq))
    }
    println("nonexistent user status=${httpRes.status}, body=${httpRes.bodyAsText()}")
    assertEquals(HttpStatusCode.NotFound, httpRes.status)
  }

  @Test
  fun `invalid signature token returns 401 on authenticated`() = testApplication {
    environment { config = HoconApplicationConfig(ConfigFactory.load("application-test.conf")) }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")

    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
        }
      }
    )
    application { routing { mainRoute() } }

    val httpRes = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer invalid.token.signature")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }
    println("invalid signature status=${httpRes.status}")
    assertEquals(HttpStatusCode.Unauthorized, httpRes.status)
  }

  @Test
  fun `missing userUid token returns 401 on authenticated`() = testApplication {
    environment { config = HoconApplicationConfig(ConfigFactory.load("application-test.conf")) }
    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val accessJwtConfig = cfg.jwtCfg("jwt")

    val jwkService: JwkService = mockk()
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
    val jwtService: JwtService = JwtServiceImpl(accessJwtConfig, jwkService)

    // Generate token without userUid
    val token = jwtService.generate(mapOf("role" to "USER", "userId" to "user"))

    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
          jwt("auth-jwt") { securitySetup("auth-jwt", this@jwt, accessJwtConfig) }
        }
      }
    )
    application { routing { mainRoute() } }

    val httpRes = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $token")
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }
    println("missing userUid status=${httpRes.status}")
    assertEquals(HttpStatusCode.Unauthorized, httpRes.status)
  }
}
