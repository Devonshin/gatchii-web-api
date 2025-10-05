package com.gatchii.domain.jwt

import kotlinx.serialization.Serializable

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 26/09/2024
 */

@Serializable
data class JwtModel(
  val accessToken: AccessToken,
  val refreshToken: RefreshToken,
)

@Serializable
data class AccessToken(
  val token: String,
  val expiresIn: Long,
)

@Serializable
data class RefreshToken(
  val token: String,
  val expiresIn: Long,
)