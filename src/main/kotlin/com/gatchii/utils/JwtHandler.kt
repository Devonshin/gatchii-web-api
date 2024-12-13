package com.gatchii.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 17/11/2024
 */

class JwtHandler {
    companion object {
        private const val JWT_CLAIM_NAME = "claim"
        fun generate(
            kid: String,
            claim: Map<String, Any>,
            algorithm: Algorithm,
            jwtConfig: JwtConfig
        ): String {
            val now = OffsetDateTime.now()
            val sign = JWT.create()
                .withAudience(jwtConfig.audience)
                .withIssuer(jwtConfig.issuer)
                .withKeyId(kid)
                .withIssuedAt(now.toInstant())
                .withClaim(JWT_CLAIM_NAME, claim)
                .withJWTId(claim.getOrDefault("id", UUID.randomUUID().toString()) as String)
                .withExpiresAt(now.plusSeconds(jwtConfig.expireSec.toLong()).toInstant())
                .sign(algorithm)
            return sign
        }

        fun convert(token: String): DecodedJWT? {
            return JWT.decode(token)
        }

        fun getClaim(decodedJWT: DecodedJWT): MutableMap<String, Any> {
            return decodedJWT.getClaim("claim")?.asMap()?: error("claim is null")
        }

        fun verify(token: String, algorithm: Algorithm, issuer: String, audience: String): Boolean {
            return try {
                val verifier: JWTVerifier = JWT.require(algorithm)
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
                verifier.verify(token)
                true
            } catch (e: JWTVerificationException) {
                println("Invalid JWT signature: ${e.message}")
                false
            }
        }
    }

    data class JwtConfig (
        val audience: String,
        val issuer: String,
        val expireSec: Int = 3600,
    )
}