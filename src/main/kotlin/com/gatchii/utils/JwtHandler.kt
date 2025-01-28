package com.gatchii.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.gatchii.domains.jwt.AccessToken
import com.gatchii.domains.jwt.JwtModel
import com.gatchii.domains.jwt.RefreshToken
import com.gatchii.plugins.JwtConfig
import io.ktor.util.logging.*
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 17/11/2024
 */

class JwtHandler {

    companion object {
        private val logger = KtorSimpleLogger("com.gatchii.utils.JwtHandler")
        private const val JWT_CLAIM_NAME = "claim"
        private const val TOKEN_REFRESH_THRESHOLD_MINUTES = 30L
        fun generate(
            jwtId: String? = UUID.randomUUID().toString(),
            claim: Map<String, Any>,
            algorithm: Algorithm,
            jwtConfig: JwtConfig
        ): String {
            val now = OffsetDateTime.now()
            val sign = JWT.create()
                .withAudience(jwtConfig.audience)
                .withIssuer(jwtConfig.issuer)
                .withKeyId(algorithm.signingKeyId)
                .withIssuedAt(now.toInstant())
                .withClaim(JWT_CLAIM_NAME, claim)
                .withJWTId(jwtId)
                .withExpiresAt(expiresAt(now, jwtConfig.expireSec.toLong()))
                .sign(algorithm)
            return sign
        }

        fun expiresAt(now: OffsetDateTime, plusSec: Long): Instant {
            return now.plusSeconds(plusSec).toInstant()
        }

        fun convert(token: String): DecodedJWT {
            return JWT.decode(token)
        }

        fun getClaim(decodedJWT: DecodedJWT): MutableMap<String, Any> {
            return decodedJWT.getClaim("claim")?.asMap()?: error("claim is null")
        }

        fun verify(token: String, algorithm: Algorithm, jwtConfig: JwtConfig): Boolean {
            return try {
                val convert = convert(token)
                val expiresAt = convert.expiresAt
                val now = OffsetDateTime.now()
                if (expiresAt.toInstant().isAfter(now.plusMinutes(TOKEN_REFRESH_THRESHOLD_MINUTES).toInstant())) { // if remain more than 30 minutes
                    logger.error("Too early request refresh token : expiresAt: $expiresAt, now: $now")
                    throw JWTVerificationException("Too early request refresh token")
                }
                val verifier: JWTVerifier = JWT.require(algorithm)
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .withJWTId(convert.id)
                    .build()
                verifier.verify(token)
                true
            } catch (e: JWTVerificationException) {
                println("Invalid JWT: ${e.message}")
                throw e
            }
        }

        fun newJwtModel(accessToken: String, jwtConfig: JwtConfig, refreshToken: String, refreshJwtConfig: JwtConfig): JwtModel {
            val now = OffsetDateTime.now()
            return JwtModel(
                accessToken = AccessToken(
                    token = accessToken,
                    expiresIn = now.plusSeconds(jwtConfig.expireSec).toEpochSecond(),
                ),
                refreshToken = RefreshToken(
                    token = refreshToken,
                    expiresIn = now.plusSeconds(refreshJwtConfig.expireSec).toEpochSecond(),
                ),
            )
        }
    }
}