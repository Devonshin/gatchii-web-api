package com.gatchii.domain.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.domain.jwk.JwkModel
import com.gatchii.domain.jwk.JwkService
import com.gatchii.plugins.ErrorResponse
import com.gatchii.plugins.JwtConfig
import com.gatchii.plugins.securitySetup
import com.gatchii.shared.common.Constants.Companion.USER_UID
import com.gatchii.utils.JwtHandler
import com.typesafe.config.ConfigFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
import shared.common.UnitTest
import shared.repository.DatabaseFactoryForTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test

@UnitTest
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
        val logger = KtorSimpleLogger(this::class.simpleName?:"RefreshTokenRouteTest")
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
        environment {
            config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
        }
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
        application {
            routing {
                route("/refresh-token") {
                    refreshTokenRoute(refreshTokenService) //
                }
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
        coEvery { jwkService.findRandomJwk() } returns randomJwk
        coEvery { jwkService.convertAlgorithm(any()) } returns algorithm
        coEvery { JwtHandler.expiresAt(any(),any()) } returns expiredAt

        //when
        val oldRefreshToken = refreshTokenService.generate(
            mapOf(
                USER_UID to userUid.toString(),
                "username" to "testname",
                "role" to "USER",
            )
        )
        logger.debug("oldRefreshToken: $oldRefreshToken")
        val bodyAsText = client.post("/refresh-token/renewal") {
            header("Authorization", "Bearer $oldRefreshToken")
            contentType(ContentType.Application.Json)
        }.bodyAsText()
        //then

        val response = Json.decodeFromString<ErrorResponse>(bodyAsText)
        assert(bodyAsText.isNotEmpty())
        assert(response.code == HttpStatusCode.Unauthorized.value)
        assert(response.message == "Token has expired")
        assert(response.path == "/refresh-token/renewal")

        coVerify(exactly = 1) { refreshTokenRepository.create(any()) }
        coVerify(exactly = 1) { jwkService.findRandomJwk() }
        coVerify(exactly = 1) { jwkService.convertAlgorithm(any()) }
        coVerify(exactly = 1) { JwtHandler.expiresAt(any(),any()) }

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
        val bodyAsText = client.post("/refresh-token/renewal") {
            header("Authorization", "Bearer $oldRefreshToken")
            contentType(ContentType.Application.Json)
        }.bodyAsText()
        //then
        val response = Json.decodeFromString<ErrorResponse>(bodyAsText)
        assert(bodyAsText.isNotEmpty())
        assert(response.code == HttpStatusCode.Unauthorized.value)
        assert(response.message == "userUid is null")
        assert(response.path == "/refresh-token/renewal")

        println("bodyAsText: $bodyAsText")
    }

}
