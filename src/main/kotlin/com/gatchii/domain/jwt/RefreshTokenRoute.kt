package com.gatchii.domain.jwt

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 21/12/2024
 */

fun Route.refreshTokenRoute(
  refreshTokenService: RefreshTokenService
) {
  val logger = KtorSimpleLogger(this::class.simpleName ?: "RefreshTokenRoute")

  route("/refresh-token") {
    authenticate("refresh-jwt") {
      post("/renewal") {
        logger.info("Refresh token requested ${call.request.headers}")
        call.request.header("Authorization")?.let {
          val token = refreshTokenService.renewal(it)
          call.respond(token)
        }
      }
    }
  }
}