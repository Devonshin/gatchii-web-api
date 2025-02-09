package com.gatchii.domain.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.domain.jwk.JwkModel
import com.gatchii.domain.jwk.JwkService
import com.gatchii.plugins.JwtConfig
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.util.*
import io.ktor.util.logging.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Package: com.gatchii.domains.jwt Created: Devonshin Date: 09/11/2024 */

@UnitTest
class JwtServiceImplTest {

    companion object {
        val logger = KtorSimpleLogger(this::class.simpleName?:"JwtServiceImplTest")
        val jwkServer = TestJwkServer() // Start temporary JWK server

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

    private val config = HoconApplicationConfig(
        ConfigFactory.parseString(
            """
                jwt {
                    audience = "TestAudience"
                    issuer = "TestIssuer"
                    property = "TestProperty"
                    expireSec = 60000
                }
            """
        )
    )
    private val jwtConfig = JwtConfig(
        audience = config.config("jwt").property("audience").getString(),
        issuer = config.config("jwt").property("issuer").getString(),
        expireSec = config.config("jwt").property("expireSec").getString().toLong()
    )
    private val jwkService = mockk<JwkService>()
    private val jwtService = JwtServiceImpl(jwtConfig, jwkService)
    val keyPair = jwkServer.getGeneratedKeyPair()
    val randomJwk = JwkModel(
        publicKey = keyPair.public.encoded.encodeBase64(),
        privateKey = keyPair.private.encoded.encodeBase64(),
        createdAt = OffsetDateTime.now(),
        id = UUID.randomUUID()
    )

    private val algorithm = Algorithm.ECDSA256(jwkServer.getJwkProvider())

    @BeforeTest
    fun setup() {
        coEvery {
            jwkService.getRandomJwk()
        } returns randomJwk
        coEvery {
            jwkService.getProvider(any())
        } returns jwkServer.getJwkProvider()
        coEvery {
            jwkService.convertAlgorithm(any())
        } returns algorithm
    }

    @Test
    fun `test generateJwt with valid claim`() = runTest {
        // Given
        val claim = mapOf("username" to "testUser", "role" to "user")

        // When
        val result = jwtService.generate(claim)
        val converted = jwtService.convert(result)
        // Then
        assertTrue(converted?.getClaim("claim")!!.asMap().containsKey("username")) // Confirm if the token contains the expected "kid"
        assertTrue(converted.getClaim("claim")!!.asMap().containsKey("role")) // Confirm if the token contains the expected "role"
    }

    @Test
    fun `test generateJwt with empty claim`() = runTest {
        // Given
        val claim = emptyMap<String, String>()
        // When
        val jwt = jwtService.generate(claim)
        val converted = jwtService.convert(jwt)
        // Then
        val claimMap = converted?.getClaim("claim")!!.asMap()

        assertFalse(claimMap.containsKey("username")) // Confirm if the token contains the expected "username"
        assertFalse(claimMap.containsKey("role")) // Confirm if the token contains the expected "role"
    }


    @Test
    fun `test jwt expired duration is 60,000 seconds `() = runTest {

        // Given
        val claim = emptyMap<String, String>()

        // When
        val jwt = jwtService.generate(claim)
        val converted = jwtService.convert(jwt)
        // Then
        val expectedExpiredDuration = 60_000 * 1000L // 60,000 초
        val actualDuration = converted?.expiresAt?.time?.minus(converted.issuedAt?.time!!)
        assertTrue(expectedExpiredDuration == actualDuration) // 만료 시간이 기대값인지 확인

    }

}
