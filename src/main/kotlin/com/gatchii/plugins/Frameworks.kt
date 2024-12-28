package com.gatchii.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.gatchii.domains.jwk.*
import com.gatchii.domains.jwt.*
import com.gatchii.domains.login.*
import com.gatchii.domains.rsa.*
import com.gatchii.utils.BCryptPasswordEncoder
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.util.concurrent.TimeUnit

fun Application.configureFrameworks() {

    val appModule = module {

        single<BCryptPasswordEncoder> { BCryptPasswordEncoder() }
        val config = environment.config.config("jwt")
        val jwtConfig = JwtConfig(
            audience = config.property("audience").getString(),
            issuer = config.property("issuer").getString(),
            realm = config.property("realm").getString(),
            jwkIssuer = config.property("jwkIssuer").getString(),
            expireSec = config.property("expireSec").getString().toString().toLong()
        )
        single<JwkProvider> {
            JwkProviderBuilder(config.property("issuer").getString())

                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()
        }

        /*repositories*/
        single<JwkRepository> { JwkRepositoryImpl(JwkTable) }
        single<LoginRepository> { LoginRepositoryImpl(LoginTable) }
        single<RefreshTokenRepository> { RefreshTokenRepositoryImpl(RefreshTokenTable) }
        single<RsaRepository> { RsaRepositoryImpl(RsaTable) }

        /*services*/
        single<JwkService> {
            JwkServiceImpl(get())
        }
        single<JwtService> {
            JwtServiceImpl(jwtConfig, get())
        }
        single<RsaService> {
            RsaServiceImpl(get())
        }
        single<RefreshTokenService> {
            RefreshTokenServiceImpl(jwtConfig, get(), get())
        }
        single<LoginService> {
            LoginServiceImpl(get(), get(), get(), get(), get())
        }

    }

    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
