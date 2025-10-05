package com.gatchii.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.gatchii.domain.jwt.AccessToken
import com.gatchii.domain.jwt.JwtModel
import com.gatchii.domain.jwt.RefreshToken
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
    private val logger = KtorSimpleLogger(this::class.simpleName ?: "JwtHandler")
    private const val JWT_CLAIM_NAME = "claim"
    private const val TOKEN_REFRESH_THRESHOLD_MINUTES = 30L
    fun generate(
      jwtId: String? = UUID.randomUUID().toString(),
      claim: Map<String, Any>,
      algorithm: Algorithm,
      jwtConfig: JwtConfig
    ): String {
      val now = OffsetDateTime.now()
      val builder = JWT.create()
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.issuer)
        .withKeyId(algorithm.signingKeyId)
        .withIssuedAt(now.toInstant())
        // 중첩 "claim" 필드 사용 제거: 각 항목을 최상위 클레임으로 설정
        .withJWTId(jwtId)
        .withExpiresAt(expiresAt(now, jwtConfig.expireSec.toLong()))
      // 전달된 claim 맵의 값을 타입에 맞춰 최상위 클레임으로 추가
      claim.forEach { (k, v) ->
        when (v) {
          is String -> builder.withClaim(k, v)
          is Int -> builder.withClaim(k, v)
          is Long -> builder.withClaim(k, v)
          is Boolean -> builder.withClaim(k, v)
          is Double -> builder.withClaim(k, v)
          is Date -> builder.withClaim(k, v)
          else -> builder.withClaim(k, v.toString())
        }
      }
      val sign = builder.sign(algorithm)
      return sign
    }

    fun expiresAt(now: OffsetDateTime, plusSec: Long): Instant {
      return now.plusSeconds(plusSec).toInstant()
    }

    fun convert(token: String): DecodedJWT {
      return JWT.decode(token)
    }

    fun getClaim(decodedJWT: DecodedJWT): MutableMap<String, Any> {
      // 최상위 클레임에서 문자열로 해석 가능한 항목만 수집
      val result = mutableMapOf<String, Any>()
      decodedJWT.claims.forEach { (key, claim) ->
        val str = claim.asString()
        if (str != null) result[key] = str
      }
      return result
    }

    fun verify(token: String, algorithm: Algorithm, jwtConfig: JwtConfig): Boolean {
      return try {
        val convert = convert(token)
        val expiresAt = convert.expiresAt
        val now = OffsetDateTime.now()
        if (expiresAt.toInstant()
            .isAfter(now.plusMinutes(TOKEN_REFRESH_THRESHOLD_MINUTES).toInstant())
        ) { // if remain more than 30 minutes
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
        logger.error("Invalid JWT: ${e.message}")
        throw e
      }
    }

    fun newJwtModel(
      accessToken: String,
      jwtConfig: JwtConfig,
      refreshToken: String,
      refreshJwtConfig: JwtConfig
    ): JwtModel {
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
