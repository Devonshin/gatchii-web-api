package com.gatchii.domains.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.gatchii.domains.jwk.JwkService
import com.gatchii.utils.JwtHandler
import com.gatchii.utils.JwtHandler.JwtConfig

/**
 * A service implementation that handles JWT generation, decoding, and claims extraction.
 *
 * This service relies on a provided JWT configuration and an implementation of the JwkService
 * to retrieve JSON Web Key (JWK) details and perform operations using specific algorithms.
 *
 * @property jwtConfig The configuration properties for JWT generation.
 * @property jwkService The service for handling JSON Web Keys (JWK) and converting algorithms.
 */

class JwtServiceImpl(
    private val jwtConfig: JwtConfig,
    private val jwkService: JwkService,
) : JwtService {

    /**
     * Generates a signed JWT token using the provided claims.
     *
     * @param claim The claims to be included in the token as key-value pairs.
     * @return A signed JWT token as a string.
     */
    override suspend fun generate(claim: Map<String, String>): String {
        val jwk = jwkService.findRandomJwk()
        val algorithm = jwkService.convertAlgorithm(jwk)
        return JwtHandler.generate(jwtId = jwk.id.toString(), claim, algorithm, jwtConfig)
    }

    /**
     * Decodes a JWT token without verifying its signature.
     *
     * @param token The JWT token to be decoded.
     * @return A decoded JWT object, or null if decoding fails.
     */
    override suspend fun convert(token: String): DecodedJWT? {
        return JWT.decode(token)
    }

    /**
     * Extracts claims from a decoded JWT token.
     *
     * @param decodedJWT The decoded JWT object from which claims will be extracted.
     * @return A mutable map containing the claims as key-value pairs.
     * @throws IllegalStateException If the "claim" field is missing or null in the token.
     */
    fun getClaim(decodedJWT: DecodedJWT): MutableMap<String, Any> {
        return decodedJWT.getClaim("claim")?.asMap() ?: error("claim is null")
    }

    override suspend fun config(): JwtConfig = jwtConfig

}