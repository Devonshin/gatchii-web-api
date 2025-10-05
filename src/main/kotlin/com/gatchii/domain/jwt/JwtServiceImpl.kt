package com.gatchii.domain.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.gatchii.domain.jwk.JwkService
import com.gatchii.plugins.JwtConfig
import com.gatchii.utils.JwtHandler
import java.util.*

class JwtServiceImpl(
  private val jwtConfig: JwtConfig,
  private val jwkService: JwkService,
) : JwtService {

  override suspend fun generate(claim: Map<String, String>): String {
    val jwk = jwkService.getRandomJwk()
    val provider = jwkService.getProvider(jwk)
    val algorithm = jwkService.convertAlgorithm(provider)
    return JwtHandler.generate(jwtId = UUID.randomUUID().toString(), claim, algorithm, jwtConfig)
  }

  override suspend fun convert(token: String): DecodedJWT? {
    return JWT.decode(token)
  }

  override suspend fun config(): JwtConfig = jwtConfig

}