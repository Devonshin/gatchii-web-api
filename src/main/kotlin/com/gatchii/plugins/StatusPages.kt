package com.gatchii.plugins

import com.gatchii.common.exception.NotFoundUserException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable

/** Package: com.gatchii.plugins Created: Devonshin Date: 11/12/2024 */

fun Application.configureStatusPages() {
  install(StatusPages) {
    val logger: Logger = KtorSimpleLogger("com.gatchii.plugins.StatusPages")
    status(HttpStatusCode.NotFound) { status ->
      call.respond(
        HttpStatusCode.NotFound, ErrorResponse(
          message = status.description,
          code = status.value,
          path = call.request.uri
        )
      )
    }

    status(HttpStatusCode.Unauthorized) { status ->
      logger.error("Unauthorized request: call.request.uri: ${call.request.uri}")
      call.respond(
        HttpStatusCode.Unauthorized, ErrorResponse(
          message = status.description,
          code = status.value,
          path = call.request.uri
        )
      )
    }

    exception<NotFoundUserException> { call, cause ->
      logger.error("exception<NotFoundUserException>: ", cause)
      call.respond(
        HttpStatusCode.NotFound, ErrorResponse(
          message = cause.message ?: "User not found",
          code = HttpStatusCode.NotFound.value,
          path = call.request.uri
        )
      )
    }

    exception<RequestValidationException> { call, cause ->
      logger.error("exception<RequestValidationException>: ", cause)
      call.respond(
        HttpStatusCode.BadRequest, ErrorResponse(
          message = cause.reasons[0],
          code = HttpStatusCode.BadRequest.value,
          path = call.request.uri
        )
      )
    }

    exception<Throwable> { call, cause ->
      logger.error("exception<Throwable>: ", cause)
      call.respond(
        HttpStatusCode.InternalServerError, ErrorResponse(
          message = HttpStatusCode.InternalServerError.description,
          code = HttpStatusCode.InternalServerError.value,
          path = call.request.uri
        )
      )
    }
  }
  println("StatusPage installed")
}

@Serializable
data class ErrorResponse(
  val message: String,
  val code: Int,
  val timestamp: Long = System.currentTimeMillis(),
  val path: String = ""
)
