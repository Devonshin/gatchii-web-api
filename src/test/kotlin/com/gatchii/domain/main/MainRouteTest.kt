package com.gatchii.domain.main

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.common.const.Constants.Companion.USER_UID
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
import io.ktor.util.logging.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import shared.TestJwkServer
import shared.common.IntegrationTest
import shared.common.setupCommonApp

/**
 * @author Devonshin
 * @date 2025-10-05
 */
@IntegrationTest
class MainRouteTest {

  private val logger: Logger = KtorSimpleLogger(this::class.simpleName ?: "MainRouteTest")

  private val config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
  private val jwtConfig: JwtConfig = JwtConfig(
    audience = config.config("jwt").property("audience").getString(),
    issuer = config.config("jwt").property("issuer").getString(),
    realm = "Test Realm",
    jwkIssuer = config.config("jwt").property("jwkIssuer").getString(),
    expireSec = 60,
  )

  companion object {
    private val jwkServer = TestJwkServer()

    @BeforeAll
    @JvmStatic
    fun beforeAll() {
      jwkServer.start()
    }

    @AfterAll
    @JvmStatic
    fun afterAll() {
      jwkServer.stop()
    }
  }

  private inline fun withMainRoute(
    crossinline block: suspend ApplicationTestBuilder.() -> Unit
  ) = testApplication {
    setupCommonApp(
      installStatusPages = true,
      installSecurity = true,
      securityInstall = {
        install(Authentication) {
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
        route("/") { mainRoute() }
      }
    }
    block()
  }

  @Test
  fun `GET root should return 200 and text`() = withMainRoute {
    val res: HttpResponse = client.get("/")
    Assertions.assertEquals(HttpStatusCode.OK, res.status)
    // ContentType이 명시되지 않을 수 있으므로 본문만 확인
    val body = res.bodyAsText()
    Assertions.assertTrue(body.contains("Hello World!"))
  }

  @Test
  fun `GET authenticated without token should return 401`() = withMainRoute {
    val res: HttpResponse = client.get("/authenticated")
    Assertions.assertEquals(HttpStatusCode.Unauthorized, res.status)
  }

  @Test
  fun `GET authenticated with valid token should return 200 and greeting`() = withMainRoute {
    val algorithm: Algorithm = Algorithm.ECDSA256(jwkServer.getJwkProvider())
    val tokenId = "test-token-id"
    // 토큰을 수동 생성하여 top-level claim(userUid, username, role)을 포함
    val now = java.time.OffsetDateTime.now()
    val token = com.auth0.jwt.JWT.create()
      .withAudience(jwtConfig.audience)
      .withIssuer(jwtConfig.issuer)
      .withKeyId(algorithm.signingKeyId)
      .withIssuedAt(now.toInstant())
      .withJWTId(tokenId)
      .withClaim(USER_UID, "5aaa3e8f-1c8f-4d94-a9d2-fd6f4d2d7c9b")
      .withClaim("username", "testuser")
      .withClaim("role", "USER")
      .withExpiresAt(JwtHandler.expiresAt(now, jwtConfig.expireSec))
      .sign(algorithm)

    val res: HttpResponse = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer $token")
    }
    Assertions.assertEquals(HttpStatusCode.OK, res.status)
    val body = res.bodyAsText()
    logger.debug("/authenticated body: $body")
    Assertions.assertTrue(body.contains("Hello, testuser!"))
  }
}