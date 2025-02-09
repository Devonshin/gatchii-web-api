package com.gatchii.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.gatchii.common.task.RoutineScheduleExpression
import com.gatchii.domain.jwk.*
import com.gatchii.domain.jwt.*
import com.gatchii.domain.login.*
import com.gatchii.domain.rsa.*
import com.gatchii.common.task.RoutineTaskHandler
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
            //val tomorrow = OffsetDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            //val taskName = "jwkTask"
            //JwkServiceImpl(get()) { task ->
            //    DailyTaskHandler(
            //        taskName = taskName,
            //        scheduledTime = Date.from(tomorrow.toInstant()),
            //        task = task
            //    )
            //}
            val taskName = "jwkTask"
            JwkServiceImpl(get()) { task: () -> Unit ->
                RoutineTaskHandler(
                    taskName = taskName,
                    scheduleExpression = RoutineScheduleExpression(),
                    task = task
                )
            }
        }
        single<JwtService> {
            JwtServiceImpl(jwtConfig, get())
        }
        single<RsaService> {
            RsaServiceImpl(get())
        }
        single<RefreshTokenService> {
            RefreshTokenServiceImpl(jwtConfig, get(), get(), get())
        }
        single<LoginService> {
            LoginServiceImpl(get(), get(), get(), get(), get())
        }

    }

    install(Koin) {
        slf4jLogger()
        modules(appModule)
        println("Framework installed")
    }
}
