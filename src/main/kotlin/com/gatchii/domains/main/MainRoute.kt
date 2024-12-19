package com.gatchii.domains.main

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger

/**
 * Package: com.gatchii.domains.main
 * Created: Devonshin
 * Date: 10/09/2024
 */

fun Route.mainRoute() {

    val logger: Logger = KtorSimpleLogger("com.gatchii.domains.main.MainRoute")

    get(Regex("/?")) {
        logger.info("Main")
        call.respond("Hello World! This is main page")
    }

    authenticate("auth-jwt") {
        get("/authenticated") {
            logger.info("Authenticated")
            val principal = call.principal<JWTPrincipal>()
            val username = principal!!.payload.getClaim("username").asString()
            val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
            call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
        }
    }

}