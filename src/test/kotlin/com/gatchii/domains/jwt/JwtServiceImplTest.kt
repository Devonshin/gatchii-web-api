package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.domains.jwk.JwkModel
import com.gatchii.domains.jwk.JwkService
import com.gatchii.utils.ECKeyPairHandler
import com.gatchii.utils.JwtHandler
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import shared.common.UnitTest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Package: com.gatchii.domains.jwt Created: Devonshin Date: 09/11/2024 */

@UnitTest
class JwtServiceImplTest {

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
    private val jwtConfig = JwtHandler.JwtConfig(
        config.config("jwt").property("audience").getString(),
        config.config("jwt").property("issuer").getString(),
        config.config("jwt").property("expireSec").getString().toLong()
    )
    private val jwkService = mockk<JwkService>()
    private val jwtService = JwtServiceImpl(jwtConfig, jwkService)
    private val jwkModel: JwkModel = JwkModel(
        privateKey = "privateKey",
        publicKey = "publicKey",
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
    )
    private val keyPaire = ECKeyPairHandler.generateKeyPair()
    private val algorithm = Algorithm.ECDSA256(keyPaire.public as ECPublicKey?, keyPaire.private as ECPrivateKey?)

    @BeforeTest
    fun setup() {
        coEvery {
            jwkService.findRandomJwk()
        } returns jwkModel
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
        assert(converted?.getClaim("claim")!!.asMap().containsKey("username")) // Confirm if the token contains the expected "kid"
        assert(converted.getClaim("claim")!!.asMap().containsKey("role")) // Confirm if the token contains the expected "role"
        assert(converted.id == jwkModel.id.toString()) // Confirm if the token contains the expected "kid"
    }

    @Test
    fun `test generateJwt with empty claim`() = testApplication {
        // Given
        val claim = emptyMap<String, String>()
        // When
        val jwt = jwtService.generate(claim)
        val converted = jwtService.convert(jwt)
        // Then
        val claimMap = converted?.getClaim("claim")!!.asMap()

        assertFalse(claimMap.containsKey("username")) // Confirm if the token contains the expected "username"
        assertFalse(claimMap.containsKey("role")) // Confirm if the token contains the expected "role"
        assert(converted.id == jwkModel.id.toString()) // Confirm if the token contains the expected "kid"
    }


    @Test
    fun `test jwt expired duration is 60,000 seconds `() = testApplication {

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
