package com.gatchii.domains.rsa

import java.util.*

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 16/09/2024
 */

interface RsaService {
    suspend fun generateRsa(): RsaModel
    suspend fun getRsa(id: UUID): RsaModel
    suspend fun findRsa(id: UUID): RsaModel?
    suspend fun deleteRsa(id: UUID)
    suspend fun deleteRsa(domain: RsaModel)
    suspend fun encrypt(rsaModel: RsaModel, data: String): String
    suspend fun decrypt(rsaModel: RsaModel, data: String): String
}