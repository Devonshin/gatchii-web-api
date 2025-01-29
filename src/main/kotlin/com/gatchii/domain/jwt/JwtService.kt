package com.gatchii.domain.jwt

import com.auth0.jwt.interfaces.DecodedJWT
import com.gatchii.plugins.JwtConfig

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 26/09/2024
 */

interface JwtService {
    suspend fun generate(claim: Map<String, String>): String
    suspend fun convert(token: String): DecodedJWT?
    suspend fun config(): JwtConfig
}
