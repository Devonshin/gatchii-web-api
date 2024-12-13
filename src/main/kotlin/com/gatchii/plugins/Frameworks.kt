package com.gatchii.plugins

import com.gatchii.domains.jwt.RefreshTokenTable
import com.gatchii.domains.jwt.JwtServiceImpl
import com.gatchii.domains.jwt.RefreshTokenRepositoryImpl
import com.gatchii.domains.jwt.RefreshTokenServiceImpl
import com.gatchii.domains.login.LoginServiceImpl
import com.gatchii.utils.BCryptPasswordEncoder
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {

    val appModule = module {

        single { BCryptPasswordEncoder() }

        /*repositories*/
        single { RefreshTokenRepositoryImpl(RefreshTokenTable) }

        /*services*/
        single {
            JwtServiceImpl(
                jwtConfig = environment.config.config("jwt"),
            )
        }
        single { RefreshTokenServiceImpl(environment.config.config("rfrst"), get()) }
        single { LoginServiceImpl(get(), get(), get(), get()) }

    }

    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
