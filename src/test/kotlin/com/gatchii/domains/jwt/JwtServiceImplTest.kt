
package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.utils.ECKeyPairHandler
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 09/11/2024
 */

@UnitTest
class JwtServiceImplTest {

    private val config = HoconApplicationConfig(ConfigFactory.parseString("""
                jwt {
                    audience = "TestAudience"
                    issuer = "TestIssuer"
                    property = "TestProperty"
                    expireSec = 60000
                }
            """))
    private val generateKeyPair = ECKeyPairHandler.generateKeyPair()
    private var algorithm: Algorithm = Algorithm.ECDSA256(generateKeyPair.public as ECPublicKey, generateKeyPair.private as ECPrivateKey);
    private var kid: String = UUID.randomUUID().toString()
    private val service = JwtServiceImpl(config.config("jwt"))

    @Test
    fun `test generateJwt with valid claim`() = testApplication {
        // Given

        val claim = mapOf("username" to "testUser", "role" to "user")

        // When
        val result = service.generate(claim, kid, algorithm)
        val converted = service.convert(result)
        // Then
        assert(converted?.getClaim("claim")!!.asMap().containsKey("username")) // Confirm if the token contains the expected "kid"
        assert(converted.getClaim("claim")!!.asMap().containsKey("role")) // Confirm if the token contains the expected "role"
        assert(converted.keyId == kid) // Confirm if the token contains the expected "kid"
    }
    
    @Test
    fun `test generateJwt with empty claim`() = testApplication {

        // Given
        val claim = emptyMap<String, Any>()

        // When
        val result = service.generate(claim, kid, algorithm)
        val converted = service.convert(result)
        // Then
        assert(!converted?.getClaim("claim")!!.asMap().containsKey("username")) // Confirm if the token contains the expected "username"
        assert(!converted.getClaim("claim")!!.asMap().containsKey("role")) // Confirm if the token contains the expected "role"
        assert(converted.keyId == kid) // Confirm if the token contains the expected "kid"
    }


    @Test
    fun `test jwt expired duration is 60,000 seconds `() = testApplication {

        // Given
        val claim = emptyMap<String, Any>()

        // When
        val result = service.generate(claim, kid, algorithm)
        val converted = service.convert(result)
        // Then
        val expectedExpiredDuration = 60_000 * 1000L // 60,000초를 밀리초로 변환
        val actualDuration = converted?.expiresAt?.time?.minus(converted.issuedAt?.time!!)
        assert(expectedExpiredDuration == actualDuration) // 만료 시간이 기대값인지 확인

    }

    @Test
    fun `jwt convert test `() = testApplication {

        // Given
        val claim = emptyMap<String, Any>()

        // When
        val result = service.generate(claim, kid, algorithm)
        val converted = service.convert(result)
        // Then
        val expectedExpiredDuration = 60_000 * 1000L // 60,000초를 밀리초로 변환
        val actualDuration = converted?.expiresAt?.time?.minus(converted.issuedAt?.time!!)
        assert(expectedExpiredDuration == actualDuration) // 만료 시간이 기대값인지 확인

    }



}
