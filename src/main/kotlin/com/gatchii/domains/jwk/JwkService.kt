package com.gatchii.domains.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import java.util.*

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 16/09/2024
 */

interface JwkService {
    suspend fun findRandomJwk(): JwkModel
    suspend fun findJwk(id: UUID): JwkModel
    suspend fun convertAlgorithm(provider: ECDSAKeyProvider): Algorithm
    suspend fun getProvider(jwk: JwkModel): ECDSAKeyProvider
    suspend fun findAllJwk(): Set<Map<String, String>>
    suspend fun deleteJwk(id: UUID): JwkModel
    suspend fun createJwk(domain: JwkModel): JwkModel
}

