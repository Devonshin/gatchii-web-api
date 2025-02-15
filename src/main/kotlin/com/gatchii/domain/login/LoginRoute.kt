package com.gatchii.domain.login

import com.gatchii.common.const.Constants.Companion.SUCCESS
import com.gatchii.plugins.JwtResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Package: com.gatchii.domains.login Created: Devonshin Date: 23/09/2024 */

fun Route.loginRoute(
    loginService: LoginService
) {

    val logger: Logger = KtorSimpleLogger(this::class.simpleName ?: "LoginRoute")

    post("/attempt") {
        val receive = call.receive<LoginUserRequest>()
        val result = loginService.loginProcess(receive)
        logger.info("Attempt Authenticate result: $result")
        if (result == null) {
            call.respond(HttpStatusCode.Unauthorized, HttpStatusCode.Unauthorized)
        } else {
            call.respondText(
                Json.encodeToString(
                    JwtResponse(
                        message = SUCCESS,
                        code = HttpStatusCode.OK.value,
                        jwt = result
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    authenticate("auth-jwt") {
        get("/logout") {

        }
    }

}