package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.InvalidClaimException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.gatchii.utils.JwtHandler
import io.ktor.server.config.*
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.jwt Created: Devonshin Date: 26/09/2024 */

class RefreshTokenServiceImpl(
    rfrstConfig: ApplicationConfig,
    private val refreshTokenRepository: RefreshTokenRepository
) : RefreshTokenService {

    private val rfrstAudience = rfrstConfig.property("audience").getString()
    private val rfrstIssuer = rfrstConfig.property("issuer").getString()
    private val expireSec = rfrstConfig.property("expireSec").getString().toInt()

    //리프레시 토큰의 유효성을 확인하고 유효할 경우 새로운 리프레시 토큰을 반환
    override suspend fun renewal(refreshToken: String, algorithm: Algorithm): String {
        //verify
        val verified = JwtHandler.verify(refreshToken, algorithm, rfrstIssuer, rfrstAudience)
        if (!verified) {
            throw JWTVerificationException("Invalid refresh token")
        }
        val convert = JwtHandler.convert(refreshToken)
        val claim = JwtHandler.getClaim(convert!!)
        val userId = claim["uuid"].let{
            UUID.fromString(it as String?)
        }?: throw InvalidClaimException("uuid is null")
        invalidateRefreshToken(
            RefreshTokenModel(
                isValid = false,
                id = UUID.fromString(convert.keyId),
                userId = userId
            )
        )
        //generate new one
        return generateRefreshToken(claim, algorithm)
    }

    
    /**
     *
     */
    override suspend fun generateRefreshToken(
        claim: MutableMap<String, Any>,
        algorithm: Algorithm
    ): String {
        val userId = claim["uuid"]!!
        val now = OffsetDateTime.now()
        val registerRefreshToken = registerRefreshToken(
            RefreshTokenModel(
                userId = UUID.fromString(userId.toString()),
                isValid = true,
                expireAt = now.plusSeconds(expireSec.toLong()),
                createdAt = now
            )
        )
        return JwtHandler.generate(
            registerRefreshToken.id.toString(), claim, algorithm, JwtHandler.JwtConfig(
                rfrstAudience, rfrstIssuer, expireSec
            )
        )
    }

    /**
     *
     */
    override suspend fun registerRefreshToken(
        refreshTokenModel: RefreshTokenModel
    ): RefreshTokenModel = refreshTokenRepository.create(refreshTokenModel)

    /**
     *
     */
    override suspend fun invalidateRefreshToken(
        refreshTokenModel: RefreshTokenModel
    ): RefreshTokenModel = refreshTokenRepository.update(refreshTokenModel)

}