package com.gatchii.domain.jwt

import com.gatchii.plugins.JwtConfig


/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 26/09/2024
 */

interface RefreshTokenService {

    suspend fun generate(claim: Map<String, String>): String

    //verify and refresh
    suspend fun renewal(refreshToken: String): JwtModel

    suspend fun registerToken(refreshTokenModel: RefreshTokenModel): RefreshTokenModel

    suspend fun invalidateToken(refreshTokenModel: RefreshTokenModel): RefreshTokenModel

    suspend fun config(): JwtConfig

}