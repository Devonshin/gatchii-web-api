package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 26/09/2024
 */

interface JwtService {
    fun generate(claim: Map<String, Any>, kid: String, algorithm: Algorithm): String
    fun convert(token: String): DecodedJWT?
    fun verify(token: String, algorithm: Algorithm): Boolean
}