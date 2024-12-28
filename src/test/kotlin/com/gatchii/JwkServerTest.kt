package com.gatchii

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.logging.*
import io.mockk.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.UnitTest
import java.util.*
import kotlin.test.Test

@UnitTest
class RefreshTokenRouteTest {
    companion object {
        val logger = KtorSimpleLogger("com.gatchii.domains.jwt.RefreshTokenRouteTest")
        val jwkServer = TestJwkServer() // Start temporary JWK server

        @BeforeAll
        @JvmStatic
        fun init() {
            jwkServer.start()
        }

        @AfterAll
        @JvmStatic
        fun destroy() {
            jwkServer.stop()
        }
    }

    @Test
    fun jwkServerTest() = testApplication {
        environment {
            config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
        }
        val client = HttpClient()
        logger.debug("jwkServerTest: {}", jwkServer.url)
        client.get { url(jwkServer.url) }.apply {
            logger.debug("jwkServerTest: {}", this)
            assert(status == HttpStatusCode.OK)
        }
    }
}