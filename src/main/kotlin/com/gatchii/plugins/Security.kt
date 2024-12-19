package com.gatchii.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond
import java.util.concurrent.TimeUnit

fun Application.configureSecurity() {
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwkProvider = JwkProviderBuilder(jwtIssuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    val userId = "userUid"
    // Please read the jwt property from the config file if you are using EngineMain
    install(Authentication) {

        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(jwkProvider, jwtIssuer) {
                acceptLeeway(10)
                withAudience(jwtAudience)
                withIssuer(jwtIssuer)
                withClaimPresence(userId)
            }
            validate { credential ->
                if (credential.payload.getClaim(userId).asString()?.isNotBlank() == true) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}