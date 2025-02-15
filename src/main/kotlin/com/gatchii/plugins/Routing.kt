package com.gatchii.plugins

import com.gatchii.domain.jwk.jwkRoute
import com.gatchii.domain.jwt.JwtModel
import com.gatchii.domain.jwt.refreshTokenRoute
import com.gatchii.domain.login.loginRoute
import com.gatchii.domain.main.mainRoute
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get

fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(DoubleReceive)

    routing {
        get("/favicon.ico") { call.respondText("") }
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

@Serializable
data class JwtResponse(
    val message: String,
    val code: Int,
    val jwt: JwtModel
)

