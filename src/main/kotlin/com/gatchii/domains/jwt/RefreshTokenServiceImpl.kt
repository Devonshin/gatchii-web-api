package com.gatchii.domains.jwt

import com.auth0.jwt.exceptions.InvalidClaimException
import com.gatchii.domains.jwk.JwkService
import com.gatchii.plugins.JwtConfig
import com.gatchii.utils.JwtHandler
import com.gatchii.utils.JwtHandler.Companion.newJwtModel
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.jwt Created: Devonshin Date: 26/09/2024 */

class RefreshTokenServiceImpl(
    private val jwtConfig: JwtConfig,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwkService: JwkService,
    private val jwtService: JwtService,
) : RefreshTokenService {
    //유효한 토큰일 경우 새로운 토큰을 반환
    override suspend fun renewal(oldRefreshToken: String): JwtModel {

        val convert = JwtHandler.convert(oldRefreshToken)
        val id = convert.id
        val jwk = jwkService.findJwk(UUID.fromString(id))
        val provider = jwkService.getProvider(jwk)
        val algorithm = jwkService.convertAlgorithm(provider)

        //verify
        JwtHandler.verify(oldRefreshToken, algorithm, jwtConfig)

        val claim =
            JwtHandler.getClaim(convert).mapValues { it.value.toString() }
        val userUid = claim["userUid"]?.let {
            UUID.fromString(it)
        } ?: throw InvalidClaimException("uuid is null")

        invalidateToken(
            RefreshTokenModel(
                isValid = false,
                id = UUID.fromString(id),
                userUid = userUid
            )
        )
        //generate new one
        val jwtConfig = jwtService.config()
        val refreshJwtConfig = config()
        return newJwtModel(
            accessToken = jwtService.generate(claim),
            jwtConfig = jwtConfig,
            refreshToken = generate(claim),
            refreshJwtConfig = refreshJwtConfig
        )
    }

    override suspend fun generate(
        claim: Map<String, String>,
    ): String {
        val userUid = claim["userUid"] ?: throw InvalidClaimException("userUid is null in claim [$claim]")
        val now = OffsetDateTime.now()
        val registerToken = registerToken(
            RefreshTokenModel(
                userUid = UUID.fromString(userUid.toString()),
                isValid = true,
                expireAt = now.plusSeconds(jwtConfig.expireSec),
                createdAt = now
            )
        )
        val jwk = jwkService.findRandomJwk()
        val provider = jwkService.getProvider(jwk)
        val algorithm = jwkService.convertAlgorithm(provider)
        return JwtHandler.generate(registerToken.id.toString(), claim, algorithm, jwtConfig)
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

    override suspend fun config(): JwtConfig = jwtConfig
}