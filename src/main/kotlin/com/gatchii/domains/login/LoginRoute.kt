package com.gatchii.domains.login

import io.ktor.server.auth.*
import io.ktor.server.routing.*

/**
 * Package: com.gatchii.domains.login
 * Created: Devonshin
 * Date: 23/09/2024
 */

fun Route.loginRoute() {

    post("/login") {

    }
    authenticate("auth-jwt") {

        get("/logout") {

        }
    }

}