/**
 * @author Devonshin
 * @date 2025-10-05
 */
package com.gatchii.integration

import com.gatchii.domain.login.LoginStatus
import com.gatchii.domain.login.UserRole
import com.gatchii.domain.login.loginRoute
import com.gatchii.domain.main.mainRoute
import com.gatchii.plugins.configureDatabases
import com.gatchii.plugins.configureFrameworks
import com.gatchii.plugins.configureRouting
import com.gatchii.plugins.configureSecurity
import com.gatchii.domain.login.LoginTable
import com.gatchii.domain.rsa.RsaTable
import com.gatchii.plugins.JwtResponse
import com.typesafe.config.ConfigFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.AbstractIntegrationTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginFlowPostgresIT : AbstractIntegrationTest() {

  companion object {
    private val jwkServer = TestJwkServer()

    @BeforeAll
    @JvmStatic
    fun initAll() {
      jwkServer.start()
    }

    @AfterAll
    @JvmStatic
    fun shutdownJwk() {
      jwkServer.stop()
    }
  }

  private fun seedUser(prefixId: String, suffixId: String, rawPassword: String): Pair<UUID, UUID> {
    val userId = UUID.randomUUID()
    val rsaId = UUID.randomUUID()
    val bCrypt = com.gatchii.common.utils.BCryptPasswordEncoder()
    val hashed = bCrypt.encode(rawPassword)

    transaction {
      RsaTable.insert {
        it[id] = rsaId
        it[publicKey] = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEbCOI...mockpub"
        it[privateKey] = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49...mockpriv"
        it[exponent] = "AQAB"
        it[modulus] = "00C1...MOCKMOD"
        it[createdAt] = OffsetDateTime.now()
        it[deletedAt] = null
      }
      LoginTable.insert {
        it[id] = userId
        it[LoginTable.prefixId] = prefixId
        it[LoginTable.suffixId] = suffixId
        it[LoginTable.password] = hashed
        it[LoginTable.rsaUid] = rsaId
        it[LoginTable.status] = LoginStatus.ACTIVE
        it[LoginTable.role] = UserRole.USER
        it[LoginTable.lastLoginAt] = OffsetDateTime.now()
        it[LoginTable.deletedAt] = null
      }
    }
    return userId to rsaId
  }

  @Test
  fun `successful login flow with postgres`() = withIntegrationApplication {
    // Ktor app with real DB + DI + Security + Routes
    application {
      configureDatabases()
      configureFrameworks()
      configureSecurity()
      configureRouting()
    }

    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    // Seed a user
    val prefix = "ituser"
    val suffix = "example"
    val pass = "P@ssw0rd!"
    seedUser(prefix, suffix, pass)

    // 1) Login
    val loginHttp = client.post("/login/attempt") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(com.gatchii.domain.login.LoginUserRequest(prefix, suffix, pass)))
    }
    assertEquals(HttpStatusCode.OK, loginHttp.status)
    val loginRes = Json.decodeFromString<JwtResponse>(loginHttp.bodyAsText())
    assertEquals(HttpStatusCode.OK.value, loginRes.code)
    assertTrue(loginRes.jwt.accessToken.token.isNotBlank())
    assertTrue(loginRes.jwt.refreshToken.token.isNotBlank())

    // 2) Authenticated endpoint
    val authRes = client.get("/authenticated") {
      header(HttpHeaders.Authorization, "Bearer ${loginRes.jwt.accessToken.token}")
      header(HttpHeaders.Accept, "*/*")
    }
    assertEquals(HttpStatusCode.OK, authRes.status)
  }

  @Test
  fun `wrong password should return 404 with postgres`() = withIntegrationApplication {
    application {
      configureDatabases()
      configureFrameworks()
      configureSecurity()
      configureRouting()
    }

    val prefix = "ituser2"
    val suffix = "example"
    val pass = "Correct1!"
    seedUser(prefix, suffix, pass)

    val httpRes = client.post("/login/attempt") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(com.gatchii.domain.login.LoginUserRequest(prefix, suffix, "wrong")))
    }
    assertEquals(HttpStatusCode.NotFound, httpRes.status)
  }
}