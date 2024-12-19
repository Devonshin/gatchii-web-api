package com.gatchii.domains.jwt

import com.auth0.jwt.exceptions.InvalidClaimException
import com.gatchii.domains.jwk.JwkService
import com.gatchii.utils.JwtHandler
import com.gatchii.utils.JwtHandler.JwtConfig
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.jwt Created: Devonshin Date: 26/09/2024 */

class RefreshTokenServiceImpl(
    private val jwtConfig: JwtConfig,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwkService: JwkService,
) : RefreshTokenService {
    //리프레시 토큰의 유효성을 확인하고 유효할 경우 새로운 리프레시 토큰을 반환
    override suspend fun renewal(oldRefreshToken: String): String {

        val convert = JwtHandler.convert(oldRefreshToken)
        val id = convert.id
        val jwk = jwkService.findJwk(UUID.fromString(id))
        val algorithm = jwkService.convertAlgorithm(jwk)

        //verify
        JwtHandler.verify(oldRefreshToken, algorithm)

        val claim = JwtHandler.getClaim(convert)
        val userId = claim["userUid"].let{
            UUID.fromString(it as String?)
        }?: throw InvalidClaimException("uuid is null")

        invalidateToken(
            RefreshTokenModel(
                isValid = false,
                id = UUID.fromString(id),
                userId = userId
            )
        )
        //generate new one
        return generate(claim)
    }


    /**
     *
     */
    override suspend fun generate(
        claim: MutableMap<String, Any>,
    ): String {
        val userId = claim["userUid"] ?: error("userUid is null in claim [$claim]")
        val now = OffsetDateTime.now()
        registerToken(
            RefreshTokenModel(
                userId = UUID.fromString(userId.toString()),
                isValid = true,
                expireAt = now.plusSeconds(jwtConfig.expireSec),
                createdAt = now
            )
        )
        val jwk = jwkService.findRandomJwk()
        val algorithm = jwkService.convertAlgorithm(jwk)
        return JwtHandler.generate(jwk.id.toString(), claim, algorithm, jwtConfig)
    }

    /**
     *
     */
    override suspend fun registerToken(
        refreshTokenModel: RefreshTokenModel
    ): RefreshTokenModel = refreshTokenRepository.create(refreshTokenModel)

    /**
     *
     */
    override suspend fun invalidateToken(
        refreshTokenModel: RefreshTokenModel
    ): RefreshTokenModel = refreshTokenRepository.update(refreshTokenModel)


}