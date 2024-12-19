package com.gatchii.domains.jwk

import com.auth0.jwt.algorithms.Algorithm
import java.util.UUID

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 16/09/2024
 */

interface JwkService {
    suspend fun findRandomJwk(): JwkModel
    suspend fun findJwk(id: UUID): JwkModel
    suspend fun convertAlgorithm(jwk: JwkModel): Algorithm
}