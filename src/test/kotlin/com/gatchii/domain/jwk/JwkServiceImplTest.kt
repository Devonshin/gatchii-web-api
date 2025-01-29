package com.gatchii.domain.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.gatchii.utils.ECKeyPairHandler
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPrivateKey
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPublicKey
import io.ktor.util.*
import io.ktor.util.logging.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.util.*

class JwkServiceImplTest {

    val logger : Logger = KtorSimpleLogger(this::class.simpleName?:"JwkServiceImplTest")
    val jwkRepository = mockk<JwkRepository>()
    val jwkService = JwkServiceImpl(jwkRepository)

    @Test
    fun `test findRandomJwk throws error when no usable jwks found`() = runTest {
        // given
        coEvery { jwkRepository.getUsableOne(any()) } returns null
        // when & then
        assertThrows<NoSuchElementException> {
            jwkService.findRandomJwk()
        }
    }

    @Test
    fun `test findRandomJwk returns jwk when usable jwks are found`() = runTest {
        // given
        val jwk = mockk<JwkModel>()
        val jwkRepository = mockk<JwkRepository>()
        coEvery { jwkRepository.getUsableOne(any()) } returns jwk
        val jwkService = JwkServiceImpl(jwkRepository)

        // when
        val result = jwkService.findRandomJwk()
        // then
        assert(jwk == result)
    }

    @Test
    fun `test findJwk throws error when jwk not found`() = runTest {
        // given
        val id = UUID.randomUUID()
        val jwkRepository = mockk<JwkRepository>()
        coEvery { jwkRepository.read(id) } returns null
        val jwkService = JwkServiceImpl(jwkRepository)

        // when & then
        assertThrows<NoSuchElementException> {
            jwkService.findJwk(id)
        }
    }

    @Test
    fun `test findJwk returns jwk when found`() =runTest {
        // given
        val id = UUID.randomUUID()
        val jwk = mockk<JwkModel>()
        val jwkRepository = mockk<JwkRepository>()
        coEvery { jwkRepository.read(id) } returns jwk
        val jwkService = JwkServiceImpl(jwkRepository)

        // when
        val result = jwkService.findJwk(id)

        // then
        assert(jwk == result)
    }

    @Test
    fun `test convertAlgorithm creates Algorithm successfully`() = runTest {
        // given
        val provider = mockk<ECDSAKeyProvider>()
        // when
        val result = jwkService.convertAlgorithm(provider)
        // then
        assert(result is Algorithm)
    }

    @Test
    fun `test getProvider returns ECDSAKeyProvider`() = runTest {
        // given
        val jwkModel = JwkModel(
            privateKey = "mock-private-key",
            publicKey = "mock-public-key",
            createdAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        mockkObject(ECKeyPairHandler)

        coEvery { convertPrivateKey(any()) } returns mockk()
        coEvery { convertPublicKey(any()) } returns mockk()

        // when
        val provider = jwkService.getProvider(jwkModel)

        // then
        assert(jwkModel.id.toString() == provider.privateKeyId)
        unmockkObject(ECKeyPairHandler)
    }

    @Test
    fun `test findAllJwk returns empty set when no jwks present`() =runTest {
        // given
        coEvery { jwkRepository.findAll() } returns emptyList()
        // when
        val result = jwkService.findAllJwk()
        // then
        assert(result.isEmpty())
    }

    @Test
    fun `test findAllJwk filters out deleted jwks`() = runTest{
        // given
        val date=  OffsetDateTime.now()
        val generateKeyPair = ECKeyPairHandler.generateKeyPair()

        val activated = JwkModel(
            privateKey = generateKeyPair.private.encoded.encodeBase64(),
            publicKey =generateKeyPair.public.encoded.encodeBase64(),
            createdAt = date,
        )
        val deleted = JwkModel(
            privateKey = generateKeyPair.private.encoded.encodeBase64(),
            publicKey =generateKeyPair.public.encoded.encodeBase64(),
            createdAt = date,
            deletedAt = date
        )

        coEvery { jwkRepository.findAll() } returns listOf(activated, deleted)
        // when
        val result = jwkService.findAllJwk()

        assert(result.size == 1)
    }

    @Test
    fun `test deleteJwk successfully deletes a jwk`() = runTest {
        // given
        val id = UUID.randomUUID()
        coEvery { jwkRepository.delete(id) } returns Unit
        // when
        jwkService.deleteJwk(id)
        // then
        coVerify(exactly = 1) { jwkRepository.delete(id) }
    }

}