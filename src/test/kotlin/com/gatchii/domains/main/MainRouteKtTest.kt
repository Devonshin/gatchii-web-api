package com.gatchii.domains.main

import com.gatchii.plugins.ErrorResponse
import com.typesafe.config.ConfigFactory
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test

class MainRouteKtTest {

    @Test()
    fun `when get main should return Hello World! This is main page`() = testApplication {
        environment {
            config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
        }
        val response = client.get("http://localhost/").body<String>()
        assert(response == "Hello World! This is main page")

    }


    @Test()
    fun `when get authenticated should return 401 Unauthorized`() = testApplication {
        environment {
            config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
        }

        val responseText = client.get("http://localhost/authenticated") {
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }.bodyAsText()
        
        val response = kotlinx.serialization.json.Json.decodeFromString<ErrorResponse>(responseText)
        
        assert(response.message == "Unauthorized")
        assert(response.code == HttpStatusCode.Unauthorized.value)
        assert(response.path == "/authenticated")

    }

}