package com.gatchii.plugins

import io.ktor.client.request.request
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.uri
import io.ktor.server.response.*
import io.ktor.server.util.url
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable

/** Package: com.gatchii.plugins Created: Devonshin Date: 11/12/2024 */

fun Application.configureStatusPages() {
    install(StatusPages) {
        val logger: Logger = KtorSimpleLogger("StatusPages")
        logger.info("StatusPages installed")

        status(HttpStatusCode.NotFound) { status ->
            call.respondText(text = "404: Page Not Found", status = status)
        }

        status(HttpStatusCode.Unauthorized) { status ->
            call.respond(
                HttpStatusCode.Unauthorized, ErrorResponse(
                    message = status.description,
                    code = status.value,
                    path = call.request.uri
                )
            )
        }

        exception<Throwable> { call, cause ->
            logger.error(cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(
                message = HttpStatusCode.InternalServerError.description,
                code = HttpStatusCode.InternalServerError.value,
                path = call.request.uri
            ))
        }
    }
}

@Serializable
data class ErrorResponse(
    val message: String,
    val code: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val path: String = ""
)
 