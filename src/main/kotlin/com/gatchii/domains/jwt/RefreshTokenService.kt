package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 26/09/2024
 */

interface RefreshTokenService {

    suspend fun generate(claim: MutableMap<String, Any>): String

    //verify and refresh
    suspend fun renewal(refreshToken: String): String

    suspend fun registerToken(refreshTokenModel: RefreshTokenModel): RefreshTokenModel

    suspend fun invalidateToken(refreshTokenModel: RefreshTokenModel): RefreshTokenModel

}