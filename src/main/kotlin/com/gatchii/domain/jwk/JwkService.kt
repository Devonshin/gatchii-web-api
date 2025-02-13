package com.gatchii.domain.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 16/09/2024
 */

interface JwkService {
    suspend fun initializeJwk()
    suspend fun getRandomJwk(): JwkModel
    suspend fun findJwk(id: UUID): JwkModel
    suspend fun convertAlgorithm(provider: ECDSAKeyProvider): Algorithm
    suspend fun getProvider(jwk: JwkModel): ECDSAKeyProvider
    suspend fun findAllJwk(): List<Map<String, String>>
    suspend fun deleteJwk(id: UUID)
    suspend fun deleteJwks(jwks: List<JwkModel>)
    suspend fun createJwk(): JwkModel
    suspend fun taskProcessing()
    suspend fun createJwks(size: Int): List<JwkModel>
    suspend fun findAllUsableJwk(): List<JwkModel>
}

