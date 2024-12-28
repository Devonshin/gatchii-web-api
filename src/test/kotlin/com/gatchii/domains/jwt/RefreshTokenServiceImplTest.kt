package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.gatchii.domains.jwk.JwkModel
import com.gatchii.domains.jwk.JwkService
import com.gatchii.plugins.JwtConfig
import com.gatchii.utils.ECKeyPairHandler
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPrivateKey
import com.gatchii.utils.ECKeyPairHandler.Companion.generatePublicKeyFromPrivateKey
import com.gatchii.utils.JwtHandler
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    private val claim: MutableMap<String, String> = mutableMapOf(
        "userUid" to UUID.randomUUID().toString(),
        "suffixIdx" to "suffixIdx",
        "role" to "user"
    )
    private val jwtConfig = JwtConfig(
        audience = config.config("rfrst").property("audience").getString(),
        issuer = config.config("rfrst").property("issuer").getString(),
        expireSec = config.config("rfrst").property("expireSec").getString().toLong()
    )

    private val jwkService = mockk<JwkService>()
    private val jwkModel: JwkModel = JwkModel(
        privateKey = "privateKey",
        publicKey = "publicKey",
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
    )
    private val keyPaire = ECKeyPairHandler.generateKeyPair()
    private val algorithm = Algorithm.ECDSA256(keyPaire.public as ECPublicKey?, keyPaire.private as ECPrivateKey?)


    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mockk<RefreshTokenRepository>()
        refreshTokenService = RefreshTokenServiceImpl(jwtConfig, refreshTokenRepository, jwkService)
    }

    @Test
    fun `generateRefreshToken test`() = runTest {
        //given
        val userId = UUID.randomUUID()
        val refreshTokenModel = RefreshTokenModel(
            true, userId, OffsetDateTime.now(), OffsetDateTime.now(), id = UUID.randomUUID()
        )
        coEvery { refreshTokenRepository.create(any()) } returns refreshTokenModel
        coEvery { jwkService.findRandomJwk() } returns jwkModel
        coEvery { jwkService.convertAlgorithm(any()) } returns algorithm

        //when
        val refreshToken = refreshTokenService.generate(claim)

        //then
        assertThat(refreshToken).isNotNull()
        assertThat(refreshToken).isNotEmpty()
        val verify = JwtHandler.verify(refreshToken, algorithm, jwtConfig)
        val convert = JwtHandler.convert(refreshToken)

        assert(verify)
        assert(convert.token == refreshToken)

        coVerify(exactly = 1) {
            refreshTokenRepository.create(any())
        }
        coVerify(exactly = 1) {
            jwkService.findRandomJwk()
        }
        coVerify(exactly = 1) {
            jwkService.convertAlgorithm(any())
        }
    }

    @Test
    fun `generate with non userUuid claim throw IllegalStateException `() = runTest {
        //given
        //when
        assertThrows<IllegalStateException> {
            refreshTokenService.generate(
                mutableMapOf(
                    "suffixIdx" to "suffixIdx",
                    "role" to "user"
                )
            )
        }
        //then
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
        val registerRefreshToken = refreshTokenService.registerToken(refreshTokenModel)
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
    fun `invalidateRefreshToken test`() = runTest {

        //given
        val userId = UUID.randomUUID()
        val refreshTokenModel = RefreshTokenModel(
            false, userId, OffsetDateTime.now(), OffsetDateTime.now(), id = UUID.randomUUID()
        )

        coEvery { refreshTokenRepository.update(any()) } returns refreshTokenModel
        //when
        val registerRefreshToken = refreshTokenService.invalidateToken(refreshTokenModel)
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
        val jwtConfig = JwtConfig(
            audience = config.config("rfrst").property("audience").getString(),
            issuer = config.config("rfrst").property("issuer").getString(),
            expireSec = config.config("rfrst").property("expireSec").getString().toLong()
        )

        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val oldRefreshTokenModel = RefreshTokenModel(
            false, userId, now.minusDays(1), now.minusDays(1), id = UUID.randomUUID()
        )
        val newRefreshTokenModel = RefreshTokenModel(
            true, userId, now.plusMonths(1), now, id = UUID.randomUUID()
        )
        coEvery { jwkService.findRandomJwk() } returns jwkModel
        coEvery { jwkService.convertAlgorithm(any()) } returns algorithm
        coEvery { jwkService.findJwk(any()) } returns jwkModel
        coEvery { refreshTokenRepository.update(any()) } returns oldRefreshTokenModel
        coEvery { refreshTokenRepository.create(any()) } returns newRefreshTokenModel

        // When
        val refreshToken = refreshTokenService.generate(claim)
        val renewedRefreshToken = refreshTokenService.renewal(refreshToken)

        // Then
        val verify = JwtHandler.verify(renewedRefreshToken, algorithm, jwtConfig)
        val convert = JwtHandler.convert(renewedRefreshToken)

        assertThat(renewedRefreshToken).isNotNull
        assertThat(verify).isTrue
        assertThat(convert.token).isEqualTo(renewedRefreshToken)

        coVerify(exactly = 1) {
            jwkService.findJwk(any())
        }
        coVerify(exactly = 1) {
            refreshTokenRepository.update(any())
        }
        coVerify(exactly = 2) {
            refreshTokenRepository.create(any())
        }
        coVerify(exactly = 2) {
            jwkService.findRandomJwk()
        }
        coVerify(exactly = 3) {
            jwkService.convertAlgorithm(any())
        }
    }


    @Test
    fun `unverified renewal token should thrown JWTVerificationException`() = runTest {
        // Given
        mockkObject(JwtHandler.Companion)
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val oldRefreshTokenModel = RefreshTokenModel(
            false, userId, now.minusDays(1), now.minusDays(1), id = UUID.randomUUID()
        )
        val newRefreshTokenModel = RefreshTokenModel(
            true, userId, now.plusMonths(1), now, id = UUID.randomUUID()
        )
        coEvery { refreshTokenRepository.create(any()) } returns newRefreshTokenModel
        coEvery { JwtHandler.verify(any(), any(), any()) } answers {
            throw JWTVerificationException("Invalid token")
        }
        coEvery { jwkService.findJwk(any()) } returns jwkModel
        coEvery { jwkService.findRandomJwk() } returns jwkModel
        coEvery { jwkService.convertAlgorithm(any()) } returns algorithm

        val refreshToken = refreshTokenService.generate(claim)
        // When
        assertThrows<JWTVerificationException> {
            refreshTokenService.renewal(refreshToken)
        }

        // Then
        coVerify(exactly = 1) {
            refreshTokenRepository.create(any())
        }
        coVerify(exactly = 1) {
            JwtHandler.verify(any(), any(), any())
        }
        coVerify(exactly = 1) {
            jwkService.findJwk(any())
        }
        coVerify(exactly = 1) {
            jwkService.findRandomJwk()
        }
        coVerify(exactly = 2) {
            jwkService.convertAlgorithm(any())
        }
        unmockkObject(JwtHandler.Companion)
    }

    @Test
    fun `too early renewal token should thrown JWTVerificationException`() = runTest {
        // Given
        val privateKeyStr = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDQ+B6qEzr/M2sql4X+09X9YlYt8BKAHX8Q7/6s4KC3qQ=="
        val privateKey = convertPrivateKey(privateKeyStr)
        val publicKey = generatePublicKeyFromPrivateKey(privateKey)
        val ecdsA256 = Algorithm.ECDSA256(publicKey as ECPublicKey, privateKey as ECPrivateKey?)
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val jwt = JwtHandler.generate(
            UUID.randomUUID().toString(), claim, ecdsA256, JwtConfig(
                audience = config.config("rfrst").property("audience").getString(),
                issuer = config.config("rfrst").property("issuer").getString(),
                expireSec = 60 * 60
            )
        )
        val oldRefreshTokenModel = RefreshTokenModel(
            false, userId, now.minusDays(1), now.minusDays(1), id = UUID.randomUUID()
        )

        coEvery { jwkService.findJwk(any()) } returns jwkModel
        coEvery { jwkService.convertAlgorithm(any()) } returns ecdsA256

        // When
        assertThrows<JWTVerificationException> {
            refreshTokenService.renewal(jwt)
        }.also {
            assertThat(it.message).isEqualTo("Too early request refresh token")
        }
        // Then
        coVerify(exactly = 1) {
            jwkService.findJwk(any())
        }
        coVerify(exactly = 1) {
            jwkService.convertAlgorithm(any())
        }
    }


}