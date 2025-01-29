package com.gatchii.domain.rsa


import com.gatchii.utils.RsaPairHandler
import io.ktor.util.logging.*
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 16/09/2024
 */

class RsaServiceImpl(
    private val rsaRepository: RsaRepository,
) : RsaService {

    private val logger = KtorSimpleLogger(this::class.simpleName?:"RsaServiceImpl")

    override suspend fun generateRsa(): RsaModel {
        val rsaKeyPair = RsaPairHandler.generateRsaDataPair()
        val privateKey = rsaKeyPair.privateKey
        val publicKey = rsaKeyPair.publicKey
        return rsaRepository.create(
            RsaModel(
                publicKey = publicKey.publicKey,
                privateKey = privateKey.privateKey,
                exponent = publicKey.e,
                modulus = publicKey.n,
                createdAt = OffsetDateTime.now()
            )
        )
    }

    override suspend fun getRsa(id: UUID): RsaModel {
        return findRsa(id)?: throw NoSuchElementException("No RsaModel found for id: $id")
    }

    override suspend fun findRsa(id: UUID): RsaModel? {
        return rsaRepository.read(id)
    }

    override suspend fun deleteRsa(id: UUID) {
        rsaRepository.delete(id)
    }

    override suspend fun deleteRsa(domain: RsaModel) {
        deleteRsa(domain.id?: throw NoSuchElementException("No RsaModel found for id: $domain"))
    }

    override suspend fun encrypt(rsaModel: RsaModel, data: String): String {
        return RsaPairHandler.encrypt(data, rsaModel.publicKey)
    }

    override suspend fun decrypt(rsaModel: RsaModel, data: String): String {
        return RsaPairHandler.decrypt(data, rsaModel.privateKey)
    }

}