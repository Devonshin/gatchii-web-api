package com.gatchii.plugins

import com.gatchii.domains.login.LoginUserRequest
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

/** Package: com.gatchii.plugins Created: Devonshin Date: 15/12/2024 */

fun Application.configureValidation() {

    install(RequestValidation) {
        validate<LoginUserRequest> { loginRequest ->
            if (loginRequest.prefixId.isBlank() || loginRequest.suffixId.isBlank() || loginRequest.password.isBlank())
                ValidationResult.Invalid("Invalid login parameter")
            else ValidationResult.Valid
        }
    }

}