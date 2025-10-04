package com.gatchii.plugins

import com.gatchii.domain.jwk.jwkRoute
import com.gatchii.domain.jwt.JwtModel
import com.gatchii.domain.jwt.refreshTokenRoute
import com.gatchii.domain.login.loginRoute
import com.gatchii.domain.main.mainRoute
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get

fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(DoubleReceive)

    routing {
      configureFaviconRoute()
        route("") {
            mainRoute()
        }
        route("/") {
            jwkRoute(get())
        }
        route("/login") {
            loginRoute(get())
        }
        route("/refresh-token") {
            refreshTokenRoute(get())
        }
    }
}

/**
 * Favicon 요청을 처리합니다.
 * 현재는 빈 응답을 반환하여 브라우저의 불필요한 favicon 요청을 무시합니다.
 */
private fun Route.configureFaviconRoute() {
  get("/favicon.ico") {
    call.respond(HttpStatusCode.NoContent)
  }
}

@Serializable
data class JwtResponse(
    val message: String,
    val code: Int,
    val jwt: JwtModel
)

