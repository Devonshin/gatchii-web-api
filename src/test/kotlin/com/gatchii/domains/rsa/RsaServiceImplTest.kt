package com.gatchii.domains.rsa

import com.gatchii.utils.PrivateKeyData
import com.gatchii.utils.PublicKeyData
import com.gatchii.utils.RsaKeyDataPair
import com.gatchii.utils.RsaPairHandler
import io.ktor.server.plugins.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import kotlin.NoSuchElementException

/**
 * Package: com.gatchii.domains.rsa
 * Created: Devonshin
 * Date: 12/11/2024
 */

class RsaServiceImplTest {

    private val mockRsaRepository = mockk<RsaRepositoryImpl>()
    private val rsaService = RsaServiceImpl(mockRsaRepository)
    private val mockRsaModel = RsaModel(
        publicKey = "publicKey",
        privateKey = "privateKey",
        exponent = "exponent",
        modulus = "modulus",
        createdAt = OffsetDateTime.now(),
        deletedAt = OffsetDateTime.now(),
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    )

    @Test
    fun `generateRsa test`() = runTest {
        //given
        mockkObject(RsaPairHandler.Companion)
        coEvery { mockRsaRepository.create(any()) } returns mockRsaModel
        coEvery { RsaPairHandler.generateRsaDataPair() } returns
                RsaKeyDataPair(
                    privateKey = PrivateKeyData(UUID.randomUUID().toString(), "", LocalDateTime.now()),
                    publicKey = PublicKeyData(UUID.randomUUID().toString(), "", "", "", LocalDateTime.now())
                )

        //when
        val generateRsa = rsaService.generateRsa()
        assertThat(generateRsa).isNotNull
        assertThat(generateRsa.id).isNotNull
        //then
        coVerify(exactly = 1) { mockRsaRepository.create(any()) }
        coVerify(exactly = 1) { RsaPairHandler.generateRsaDataPair() }
    }

    @Test
    fun `getRsa should return single rsaModel objecttest`() = runTest {
        //given
        coEvery { mockRsaRepository.read(any()) } returns mockRsaModel
        //when
        val rsaModel = rsaService.getRsa(mockRsaModel.id!!)
        //then
        coVerify(exactly = 1) { mockRsaRepository.read(any()) }
        assertThat(rsaModel).isNotNull
        assertThat(rsaModel.id).isEqualTo(mockRsaModel.id)
    }

    @Test
    fun `getRsa should throw NotFoundException when requested by non exist id test`() = runTest {
        //given
        coEvery { mockRsaRepository.read(any()) } returns null
        //when
        //then
        assertThrows<NotFoundException> {
            rsaService.getRsa(UUID.randomUUID())
        }
    }

    @Test
    fun `deleteRsa should delete single rsa and return void test`() = runTest {

        //given
        coEvery { mockRsaRepository.delete(any<UUID>()) } returns Unit
        coEvery { mockRsaRepository.read(any()) } returns null
        //when
        rsaService.deleteRsa(mockRsaModel.id!!)
        //then
        val findRsa = rsaService.findRsa(mockRsaModel.id!!)
        assertThat(findRsa).isNull()
        coVerify(exactly = 1) { mockRsaRepository.delete(any<UUID>()) }
        coVerify(exactly = 1) { mockRsaRepository.read(any()) }
    }

    @Test
    fun `deleteRsa by domain should delete single rsa and return void test`() = runTest {
        //given
        coEvery { mockRsaRepository.delete(any<UUID>()) } returns Unit
        coEvery { mockRsaRepository.read(any()) } returns null
        //when
        rsaService.deleteRsa(mockRsaModel)
        //then
        val findRsa = rsaService.findRsa(mockRsaModel.id!!)
        assertThat(findRsa).isNull()
        coVerify(exactly = 1) { mockRsaRepository.delete(any<UUID>()) }
        coVerify(exactly = 1) { mockRsaRepository.read(any()) }
    }

    @Test
    fun `deleteRsa by domain non id should thrown NoSuchElementException test`() = runTest {
        //given
        //when
        //then
        assertThrows<NoSuchElementException> {
            rsaService.deleteRsa(noIdRsaModel())
        }
    }

    private fun noIdRsaModel(now: OffsetDateTime = OffsetDateTime.now()): RsaModel {
        val rsaModel = RsaModel(
            publicKey = "publicKey",
            privateKey = "privateKey",
            exponent = "exponent",
            modulus = "modulus",
            createdAt = now
        )
        return rsaModel
    }
}