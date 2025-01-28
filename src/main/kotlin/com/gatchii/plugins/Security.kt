package com.gatchii.plugins

import com.auth0.jwk.JwkProviderBuilder
import com.gatchii.shared.common.Constants.Companion.USER_UID
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.logging.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

val logger: Logger = KtorSimpleLogger("com.gatchii.plugins.Security")

fun Application.configureSecurity() {

    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(BouncyCastleProvider())
    }
    // Please read the jwt property from the config file if you are using EngineMain
    val env = environment.config

    install(Authentication) {
        jwt("auth-jwt") {
            securitySetup(
                "auth-jwt",
                this@jwt,
                JwtConfig(
                    audience = env.config("jwt").property("audience").getString(),
                    issuer = env.config("jwt").property("issuer").getString(),
                    realm = env.config("jwt").property("realm").getString(),
                    jwkIssuer = env.config("jwt").property("jwkIssuer").getString(),
                    expireSec = env.config("jwt").property("expireSec").getString().toLong()
                )
            )
        }
        jwt("refresh-jwt") {
            securitySetup(
                "refresh-jwt",
                this@jwt,
                JwtConfig(
                    audience = env.config("rfrst").property("audience").getString(),
                    issuer = env.config("rfrst").property("issuer").getString(),
                    realm = env.config("rfrst").property("realm").getString(),
                    jwkIssuer = env.config("rfrst").property("jwkIssuer").getString(),
                    expireSec = env.config("rfrst").property("expireSec").getString().toLong()
                )
            )
        }
    }
    println("Security installed")
}

fun securitySetup(name: String, config: JWTAuthenticationProvider.Config, jwtConfig: JwtConfig) {

    val jwtAudience = jwtConfig.audience
    val jwtRealm = jwtConfig.realm
    val jwtIssuer = jwtConfig.issuer
    val jwkIssuer = jwtConfig.jwkIssuer
    val authFailureKey = AttributeKey<String>("authFailure")
    val jwkProvider = JwkProviderBuilder(jwkIssuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    logger.info("$name JWT Config: realm=$jwtRealm, issuer=$jwtIssuer, audience=$jwtAudience, jwkIssuer=$jwkIssuer")
    config.realm = jwtRealm
    config.verifier(jwkProvider, jwtIssuer) {
        acceptLeeway(30)
        withAudience(jwtAudience)
        withIssuer(jwtIssuer)
    }

    config.validate { credential ->
        val payload = credential.payload
        logger.info(" is valid ?  = ${payload.expiresAt} : " +
                "\npayload.expiresAt?.time : ${payload.expiresAt?.time}" +
                "\nOffsetDateTime.now().toEpochSecond() : ${OffsetDateTime.now().toEpochSecond()}" +
                "\nminusTime : ${payload.expiresAt?.time?.minus(OffsetDateTime.now().toEpochSecond() * 1000)}"
        )

        if (payload.expiresAt?.time?.minus(OffsetDateTime.now().toEpochSecond() * 1000).let { it!! < 0 }) {
            attributes.put(authFailureKey, "Token has expired")
            logger.info(" is valid ? = Token has expired")
            return@validate null
        }
        val userUid = payload.getClaim(USER_UID).asString()
        if (userUid == null) {
            attributes.put(authFailureKey, "userUid is null")
            logger.info(" is valid ? = userUid is null")
            return@validate null
        }

        userUid.takeIf { it.isNotBlank() }?.let { JWTPrincipal(credential.payload) }
    }

    config.challenge { defaultScheme, realm ->
        val failureReason = call.attributes.getOrNull(authFailureKey)
        logger.error("$name : Invalid or expired token failureReason=[$failureReason], realm=$realm, issuer=$jwtIssuer")
        call.respond(
            ErrorResponse(
                message = failureReason ?: "Token is not valid or has expired",
                code = HttpStatusCode.Unauthorized.value,
                path = call.request.uri
            )
        )
    }
}

data class JwtConfig (
    val audience: String,
    val issuer: String,
    val realm: String = "GatchiiWebApp",
    val jwkIssuer: String = issuer,
    val expireSec: Long = 3600,
)