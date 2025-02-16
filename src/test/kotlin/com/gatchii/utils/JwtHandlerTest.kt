package com.gatchii.utils

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.*
import com.gatchii.common.utils.ECKeyPairHandler
import com.gatchii.plugins.JwtConfig
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.common.UnitTest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*

/** Package: com.gatchii.utils Created: Devonshin Date: 17/11/2024 */

@UnitTest
class JwtHandlerTest {

    private val generateKeyPair = ECKeyPairHandler.generateKeyPair()
    private var algorithm: Algorithm = Algorithm.ECDSA256(generateKeyPair.public as ECPublicKey, generateKeyPair.private as ECPrivateKey)
    private val claim = mapOf("username" to "testUser", "role" to "user")
    private val jwtConfig = JwtConfig(
        audience = "rfrstAudience",
        issuer = "rfrstIssuer",
        expireSec = 60
    )

    private var jwtStr: String = JwtHandler.generate("id", claim, algorithm, jwtConfig)

    @BeforeEach
    fun setUp() {
        unmockkAll()
        unmockkObject(JwtHandler)
    }

    @Test
    fun `generate test`() {
        //given
        //when
        val generate = JwtHandler.generate("id", claim, algorithm, jwtConfig)
        println("generate = $generate")
        //then
        assertThat(generate).isNotBlank()
    }

    @Test
    fun `convert test`() {
        //given
        //when
        val convertJwt = JwtHandler.convert(jwtStr)
        //then
        assertThat(convertJwt).isNotNull()
        assertThat(convertJwt.issuer).isEqualTo(jwtConfig.issuer)
        assertThat(convertJwt.audience).contains(jwtConfig.audience)
        assertThat(convertJwt.expiresAt?.time).isEqualTo(convertJwt?.issuedAt?.time?.plus(jwtConfig.expireSec * 1000L))
    }


    @Test
    fun `getClaim test`() {

        //given
        //when
        val convertJwt = JwtHandler.convert(jwtStr)
        val claim1 = JwtHandler.getClaim(convertJwt!!)
        //then
        assertThat(claim1).isNotNull()
        assertThat(claim1["username"]).isEqualTo("testUser")
        assertThat(claim1["role"]).isEqualTo("user")

    }

    @Test
    fun `getClaim empty test`() {

        //given
        val emptyJwtStr = JwtHandler.generate("id", mapOf(), algorithm, jwtConfig)
        //when
        val convertJwt = JwtHandler.convert(emptyJwtStr)
        val claim1 = JwtHandler.getClaim(convertJwt)
        //then
        assertThat(claim1).isNotNull()
        assertThat(claim1["username"]).isNull()
        assertThat(claim1["role"]).isNull()

    }

    @Test
    fun `verify should return true for valid token`() = runTest {

        // Given
        val token = JwtHandler.generate("id", claim, algorithm, jwtConfig)

        // When
        val result = JwtHandler.verify(token, algorithm, jwtConfig)

        // Then
        assertTrue(result)
    }

    @Test
    fun `too early verify should throw JWTVerificationException`()  = runTest {
        // Given
        val token = JwtHandler.generate("id", claim, algorithm, JwtConfig(
            audience = "rfrstAudience",
            issuer = "rfrstIssuer",
            expireSec = 60 * 60
        ))
        // When
        assertThrows<JWTVerificationException> {
            JwtHandler.verify(token, algorithm, jwtConfig)
        }
    }

    @Test
    fun `verify should throw JWTDecodeException for token with invalid signature`()  = runTest {
        // Given
        val token = "invalid.token.signature"
        // When
        assertThrows<JWTDecodeException> {
            JwtHandler.verify(token, algorithm, jwtConfig)
        }
    }

    @Test
    fun `verify should throw IncorrectClaimException for token with incorrect audience`()  = runTest {
        // Given
        val incorrectConfig = JwtConfig(
            audience = "incorrect-audience",
            issuer = jwtConfig.issuer,
            expireSec = 60)
        val token = JwtHandler.generate("id", claim, algorithm, incorrectConfig)

        // When
        assertThrows<IncorrectClaimException> {
            JwtHandler.verify(token, algorithm, jwtConfig)
        }
    }

    @Test
    fun `verify should throw IncorrectClaimException for token with incorrect issuer`()  = runTest {
        // Given
        val incorrectConfig = JwtConfig(
            audience = jwtConfig.audience,
            issuer = "incorrect-issuer",
            expireSec = 60)
        val token = JwtHandler.generate("id", claim, algorithm, incorrectConfig)

        // When
        assertThrows<JWTVerificationException> {
            JwtHandler.verify(token, algorithm, jwtConfig)
        }
        // Then
    }

    @Test
    fun `verify should throw TokenExpiredException for expired token`()  = runTest {
        // Given
        val expiredConfig = JwtConfig(
            audience = jwtConfig.audience,
            issuer = jwtConfig.issuer,
            expireSec = -60)
        val token = JwtHandler.generate("id", claim, algorithm, expiredConfig)

        // When
        assertThrows<TokenExpiredException> {
            JwtHandler.verify(token, algorithm, jwtConfig)
        }
        // Then
    }

    @Test
    fun `verify should throw AlgorithmMismatchException for token with incorrect algorithm`()  = runTest {
        // Given
        val expiredConfig = JwtConfig(
            audience = jwtConfig.audience,
            issuer = jwtConfig.issuer,
            expireSec = -60)
        val token = JwtHandler.generate("id", claim, algorithm, expiredConfig)
        val algorithm: Algorithm = Algorithm.HMAC256("incorrect-secret")
        // When
        assertThrows<AlgorithmMismatchException> {
            JwtHandler.verify(token, algorithm, jwtConfig)
        }
        // Then
    }

}