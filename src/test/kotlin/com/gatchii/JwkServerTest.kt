package com.gatchii

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.util.logging.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.TestJwkServer
import shared.common.UnitTest
import kotlin.test.Test

@UnitTest
class JwkServerTest {
    companion object {
        val logger = KtorSimpleLogger("com.gatchii.JwkServerTest")
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