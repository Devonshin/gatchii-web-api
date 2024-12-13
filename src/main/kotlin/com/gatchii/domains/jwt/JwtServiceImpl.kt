package com.gatchii.domains.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.gatchii.utils.JwtHandler
import io.ktor.server.config.*

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 26/09/2024
 */

class JwtServiceImpl(
    jwtConfig: ApplicationConfig
) : JwtService {

    private val jwtAudience = jwtConfig.property("audience").getString()
    private val jwtIssuer = jwtConfig.property("issuer").getString()
    private val expireSec = jwtConfig.property("expireSec").getString().toInt()

    override fun generate(
        claim: Map<String, Any>,
        kid: String,
        algorithm: Algorithm
    ): String = JwtHandler.generate(kid, claim, algorithm, JwtHandler.JwtConfig(
        jwtAudience, jwtIssuer, expireSec
    ))

    override fun verify(token: String, algorithm: Algorithm): Boolean {
        return JwtHandler.verify(token, algorithm, jwtIssuer, jwtAudience)
    }

    override fun convert(token: String): DecodedJWT? {
        return JwtHandler.convert(token)
    }

}