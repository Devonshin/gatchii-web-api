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
import com.gatchii.plugins.configureSerialization
import com.gatchii.plugins.configureValidation
import com.gatchii.plugins.configureStatusPages
import com.typesafe.config.ConfigFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.util.encodeBase64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.AbstractIntegrationTest
import shared.common.IntegrationTest
import shared.configureTestSecurity
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.gatchii.common.repository.DatabaseFactoryImpl
import com.gatchii.plugins.DatabaseConfig
import com.gatchii.domain.jwk.JwkTable
import com.gatchii.domain.jwk.JwkStatus

@IntegrationTest
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
      val pair = com.gatchii.common.utils.RsaPairHandler.generateRsaDataPair()
      val pub = pair.publicKey
      val priv = pair.privateKey
      RsaTable.insert {
        it[id] = rsaId
        it[publicKey] = pub.publicKey
        it[privateKey] = priv.privateKey
        it[exponent] = pub.e
        it[modulus] = pub.n
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

  private fun seedJwk(testJwkServer: TestJwkServer) {
    transaction {
      // TestJwkServer의 키 페어를 사용하여 JWT 생성/검증 일관성 보장
      val keyPair = testJwkServer.getGeneratedKeyPair()
      val privEnc = com.gatchii.common.utils.RsaPairHandler.encrypt(keyPair.private.encoded.encodeBase64())
      val pubB64 = keyPair.public.encoded.encodeBase64()
      JwkTable.insert {
        it[privateKey] = privEnc
        it[publicKey] = pubB64
        it[status] = JwkStatus.ACTIVE.name
        it[createdAt] = OffsetDateTime.now()
        it[deletedAt] = null
      }
    }
  }

  @Test
  fun `successful login flow with postgres`() = withIntegrationApplication {
    // Flyway 마이그레이션 적용 (테스트 컨테이너 DB에 스키마 보장)
    org.flywaydb.core.Flyway.configure()
      .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .load()
      .migrate()

    // 트랜잭션 기반 시딩 전에 Exposed 연결을 먼저 보장
    val dbFactory = DatabaseFactoryImpl(
      DatabaseConfig(
        driverClass = "org.postgresql.Driver",
        url = postgres.jdbcUrl,
        user = postgres.username,
        password = postgres.password,
        maxPoolSize = 2
      )
    )
    dbFactory.connect()

    // JWT 서명을 위한 JWK 사전 데이터 삽입 (트랜잭션이 커밋되도록 application 블록 밖에서 수행)
    // TestJwkServer의 키 페어를 사용하여 JWT 생성/검증 키가 일치하도록 보장
    this@LoginFlowPostgresIT.seedJwk(jwkServer)

    // Ktor app with real DB + DI + Security + Routes
    application {
      // 테스트에서는 Postgres Testcontainers 연결을 별도로 수행하므로 내부 DB 플러그인은 생략
      configureFrameworks()

      // JSON 직렬화/역직렬화 및 요청 검증 플러그인 설치
      configureSerialization()
      configureValidation()
      configureStatusPages()

      // JWK 핸들러 초기화 (Koin 초기화 이후, Security 설정 전에 수행)
      com.gatchii.domain.jwk.JwkHandler.setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
      com.gatchii.domain.jwk.JwkHandler.clearAll()
      kotlinx.coroutines.runBlocking {
        val koin = org.koin.core.context.GlobalContext.get()
        val svc = koin.get<com.gatchii.domain.jwk.JwkService>()
        svc.initializeJwk()
      }

      // Use test-specific security config that reads JWK from DB
      val jwkService = org.koin.core.context.GlobalContext.get().get<com.gatchii.domain.jwk.JwkService>()
      configureTestSecurity(jwkService)

      configureRouting()
    }

    val cfg = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))

    // Seed a user (마이그레이션 및 연결 이후)
    val prefix = "ituser"
    val suffix = "example"
    val pass = "P@ssw0rd!"
    val (userId, rsaId) = seedUser(prefix, suffix, pass)
    println("DEBUG: Seeded user with id=$userId, rsaId=$rsaId")

    // Verify user was seeded
    val userCount = transaction {
      LoginTable.selectAll().count()
    }
    println("DEBUG: Total users in DB: $userCount")

    // Verify JWK was seeded
    val jwkCount = transaction {
      JwkTable.selectAll().count()
    }
    println("DEBUG: Total JWK entries in DB: $jwkCount")

    // Read last_login_at before login
    val before = transaction {
      LoginTable.selectAll()
        .where { (LoginTable.prefixId eq prefix) and (LoginTable.suffixId eq suffix) }
        .limit(1)
        .single()[LoginTable.lastLoginAt]
    }

    // 1) Login
    val loginHttp = client.post("/login/attempt") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(com.gatchii.domain.login.LoginUserRequest(prefix, suffix, pass)))
    }
    // Debug: print response if not OK
    if (loginHttp.status != HttpStatusCode.OK) {
      println("Login failed with status: ${loginHttp.status}")
      println("Response body: ${loginHttp.bodyAsText()}")
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

    // 3) last_login_at updated check (B안)
    val after = transaction {
      LoginTable.selectAll()
        .where { (LoginTable.prefixId eq prefix) and (LoginTable.suffixId eq suffix) }
        .limit(1)
        .single()[LoginTable.lastLoginAt]
    }
    assertTrue(after.isAfter(before))
  }

  @Test
  fun `wrong password should return 404 with postgres`() = withIntegrationApplication {
    application {
      // 테스트에서는 Postgres Testcontainers 연결을 별도로 수행하므로 내부 DB 플러그인은 생략
      configureFrameworks()
      // JSON 직렬화/역직렬화 및 요청 검증 플러그인 설치
      configureSerialization()
      configureValidation()
      configureStatusPages()
      configureSecurity()
      configureRouting()
    }

    // 안전하게 Exposed 연결을 보장 (Postgres Testcontainers)
    DatabaseFactoryImpl(
      DatabaseConfig(
        driverClass = "org.postgresql.Driver",
        url = postgres.jdbcUrl,
        user = postgres.username,
        password = postgres.password,
        maxPoolSize = 2
      )
    ).connect()

    // Flyway 마이그레이션 적용
    org.flywaydb.core.Flyway.configure()
      .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .load()
      .migrate()

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