package com.gatchii.domains.jwk

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 26/01/2025
 */

fun Route.jwkRoute(jwkService: JwkService) {

    val logger: Logger = KtorSimpleLogger("com.gatchii.domains.jwk.JwkRoute")

    get(".well-known/gatchii-jwks.json") {

        val jwkList = jwkService.findAllJwk()
        call.respondText(
            Json.encodeToString(JwkResponse(jwkList)),
            contentType = ContentType.Application.Json
        )

    }

}


@Serializable
data class JwkResponse(
    val keys: Set<Map<String, String>>
)