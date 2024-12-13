package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.utils.ECKeyPairHandler
import com.gatchii.utils.JwtHandler
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.jwt Created: Devonshin Date: 16/11/2024 */

@UnitTest
class RefreshTokenServiceImplTest {

    private val config = HoconApplicationConfig(
        ConfigFactory.parseString(
            """
                rfrst {
                    audience = "TestAudience"
                    issuer = "TestIssuer"
                    property = "TestProperty"
                    expireSec = 600
                }
            """
        )
    )

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var refreshTokenService: RefreshTokenServiceImpl
    private val generateKeyPair = ECKeyPairHandler.generateKeyPair()
    private val ecdsA256 = Algorithm.ECDSA256(generateKeyPair.public as ECPublicKey, generateKeyPair.private as ECPrivateKey)
    private val claim: MutableMap<String, Any> = mutableMapOf(
        "uuid" to UUID.randomUUID().toString(),
        "suffixIdx" to "suffixIdx",
        "role" to "user"
    )

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mockk<RefreshTokenRepository>()
        refreshTokenService = RefreshTokenServiceImpl(config.config("rfrst"), refreshTokenRepository)
    }

    @Test
    fun `generateRefreshToken test`() = runTest {
        //given
        val userId = UUID.randomUUID()
        val refreshTokenModel = RefreshTokenModel(
            true, userId, OffsetDateTime.now(), OffsetDateTime.now(), id = UUID.randomUUID()
        )
        coEvery { refreshTokenRepository.create(any()) } returns refreshTokenModel

        //when
        val refreshToken = refreshTokenService.generateRefreshToken(claim, ecdsA256)

        //then
        assertThat(refreshToken).isNotNull()
        assertThat(refreshToken).isNotEmpty()
        val verify = JwtHandler.verify(
            refreshToken, ecdsA256,
            config.config("rfrst").property("issuer").getString(),
            config.config("rfrst").property("audience").getString()
        )
        val convert = JwtHandler.convert(refreshToken)

        assert(verify)
        assert(convert != null && convert.token == refreshToken)

        coVerify(exactly = 1) {
            refreshTokenRepository.create(any())
        }
    }

    @Test
    fun registerRefreshToken() = runTest {
        //given
        val userId = UUID.randomUUID()
        val refreshTokenModel = RefreshTokenModel(
            true, userId, OffsetDateTime.now(), OffsetDateTime.now(), id = UUID.randomUUID()
        )

        coEvery { refreshTokenRepository.create(any()) } returns refreshTokenModel
        //when
        val registerRefreshToken = refreshTokenService.registerRefreshToken(refreshTokenModel)
        //then
        assertThat(registerRefreshToken).isNotNull
        assertThat(registerRefreshToken.id).isNotNull
        assertThat(registerRefreshToken.createdAt).isNotNull
        assertThat(registerRefreshToken.isValid).isTrue
        coVerify(exactly = 1) {
            refreshTokenRepository.create(any())
        }
    }

    @Test
    fun invalidateRefreshToken() = runTest {

        //given
        val userId = UUID.randomUUID()
        val refreshTokenModel = RefreshTokenModel(
            false, userId, OffsetDateTime.now(), OffsetDateTime.now(), id = UUID.randomUUID()
        )

        coEvery { refreshTokenRepository.update(any()) } returns refreshTokenModel
        //when
        val registerRefreshToken = refreshTokenService.invalidateRefreshToken(refreshTokenModel)
        //then
        assertThat(registerRefreshToken).isNotNull
        assertThat(registerRefreshToken.id).isNotNull
        assertThat(registerRefreshToken.createdAt).isNotNull
        assertThat(registerRefreshToken.isValid).isFalse

        coVerify(exactly = 1) {
            refreshTokenRepository.update(any())
        }
    }

    @Test
    fun renewalRefreshToken() = runTest {
        // Given
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val oldRefreshTokenModel = RefreshTokenModel(
            false, userId, now.minusDays(1), now.minusDays(1), id = UUID.randomUUID()
        )
        val newRefreshTokenModel = RefreshTokenModel(
            true, userId, now.plusMonths(1), now, id = UUID.randomUUID()
        )

        coEvery { refreshTokenRepository.update(any()) } returns oldRefreshTokenModel
        coEvery { refreshTokenRepository.create(any()) } returns newRefreshTokenModel

        // When
        val refreshToken = refreshTokenService.generateRefreshToken(claim, ecdsA256)
        val renewedRefreshToken = refreshTokenService.renewal(refreshToken, ecdsA256)

        // Then
        val verify = JwtHandler.verify(
            renewedRefreshToken, ecdsA256,
            config.config("rfrst").property("issuer").getString(),
            config.config("rfrst").property("audience").getString()
        )
        val convert = JwtHandler.convert(renewedRefreshToken)

        assertThat(renewedRefreshToken).isNotNull
        assertThat(verify).isTrue
        assertThat(convert?.token).isEqualTo(renewedRefreshToken)

        coVerify(exactly = 1) {
            refreshTokenRepository.update(any())
        }
        coVerify(exactly = 2) {
            refreshTokenRepository.create(any())
        }
    }


}