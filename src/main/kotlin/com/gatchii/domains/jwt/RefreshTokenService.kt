package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 26/09/2024
 */

interface RefreshTokenService {

    suspend fun generateRefreshToken(
                             claim: MutableMap<String, Any>,
                             algorithm: Algorithm
    ): String

    //verify and refresh
    suspend fun renewal(refreshToken: String, algorithm: Algorithm): String

    suspend fun registerRefreshToken(refreshTokenModel: RefreshTokenModel): RefreshTokenModel

    suspend fun invalidateRefreshToken(refreshTokenModel: RefreshTokenModel): RefreshTokenModel

}