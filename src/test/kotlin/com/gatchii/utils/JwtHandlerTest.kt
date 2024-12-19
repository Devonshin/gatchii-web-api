package com.gatchii.utils

import com.auth0.jwt.algorithms.Algorithm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*

/** Package: com.gatchii.utils Created: Devonshin Date: 17/11/2024 */

@UnitTest
class JwtHandlerTest {

    private var kid: String = UUID.randomUUID().toString()
    private val generateKeyPair = ECKeyPairHandler.generateKeyPair()
    private var algorithm: Algorithm = Algorithm.ECDSA256(generateKeyPair.public as ECPublicKey, generateKeyPair.private as ECPrivateKey)
    private val claim = mapOf("username" to "testUser", "role" to "user")
    private val jwtConfig = JwtHandler.JwtConfig(
        "rfrstAudience", "rfrstIssuer", 60
    )

    private var jwtStr: String = JwtHandler.generate("id", claim, algorithm, jwtConfig)

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
        assertThat(convertJwt.keyId).isEqualTo(kid)
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
    fun `verify should return true for valid token`() {
        // Given
        val token = JwtHandler.generate("id", claim, algorithm, jwtConfig)

        // When
        val result = JwtHandler.verify(token, algorithm)

        // Then
        assertTrue(result)
    }

    @Test
    fun `verify should return false for valid token`() {
        // Given
        val token = JwtHandler.generate("id", claim, algorithm, jwtConfig)

        // When
        val result = JwtHandler.verify("..", algorithm)

        // Then
        assertFalse(result)
    }

    @Test
    fun `verify should return false for token with invalid signature`() {
        // Given
        val token = "invalid.token.signature"

        // When
        val result = JwtHandler.verify(token, algorithm)

        // Then
        assertFalse(result)
    }

    @Test
    fun `verify should return false for token with incorrect audience`() {
        // Given
        val incorrectConfig = JwtHandler.JwtConfig("incorrect-audience", jwtConfig.issuer)
        val token = JwtHandler.generate("id", claim, algorithm, incorrectConfig)

        // When
        val result = JwtHandler.verify(token, algorithm)

        // Then
        assertFalse(result)
    }

    @Test
    fun `verify should return false for token with incorrect issuer`() {
        // Given
        val incorrectConfig = JwtHandler.JwtConfig(jwtConfig.audience, "incorrect-issuer")
        val token = JwtHandler.generate("id", claim, algorithm, incorrectConfig)

        // When
        val result = JwtHandler.verify(token, algorithm)

        // Then
        assertFalse(result)
    }

    @Test
    fun `verify should return false for expired token`() {
        // Given
        val expiredConfig = JwtHandler.JwtConfig(jwtConfig.audience, jwtConfig.issuer, -60)
        val token = JwtHandler.generate("id", claim, algorithm, expiredConfig)

        // When
        val result = JwtHandler.verify(token, algorithm)

        // Then
        assertFalse(result)
    }

    @Test
    fun `verify should return false for token with incorrect algorithm`() {
        // Given
        val expiredConfig = JwtHandler.JwtConfig(jwtConfig.audience, jwtConfig.issuer, -60)
        val token = JwtHandler.generate("id", claim, algorithm, expiredConfig)
        val algorithm: Algorithm = Algorithm.HMAC256("incorrect-secret")
        // When
        val result = JwtHandler.verify(token, algorithm)

        // Then
        assertFalse(result)
    }

}