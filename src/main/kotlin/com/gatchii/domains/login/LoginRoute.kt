package com.gatchii.domains.login

import com.gatchii.domains.jwt.JwtModel
import com.gatchii.plugins.JwtResponse
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

/** Package: com.gatchii.domains.login Created: Devonshin Date: 23/09/2024 */

fun Route.loginRoute(
    loginService: LoginService
) {

    val logger: Logger = KtorSimpleLogger("com.gatchii.domains.main.LoginRoute")
    fun response(jwt: JwtModel): JwtResponse = JwtResponse(
        message = "Success",
        code = HttpStatusCode.OK.value,
        jwt = jwt
    )

    post("/attempt") {
        val receive = call.receive<LoginUserRequest>()
        val attemptAuthenticate = loginService.loginProcess(receive)
        logger.info("Attempt Authenticate: $attemptAuthenticate")
        if (attemptAuthenticate == null) {
            call.respond(HttpStatusCode.Unauthorized, HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.OK, response(attemptAuthenticate)
            )
        }
    }

    authenticate("auth-jwt") {
        get("/logout") {

        }
    }

}