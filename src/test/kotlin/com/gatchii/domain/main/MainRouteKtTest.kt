package com.gatchii.domain.main

import com.gatchii.plugins.ErrorResponse
import com.gatchii.plugins.JwtConfig
import com.gatchii.plugins.securitySetup
import com.typesafe.config.ConfigFactory
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import shared.common.IntegrationTest

@IntegrationTest
class MainRouteKtTest {

  @Test()
  fun `get main page should return Hello World! This is main page`() = testApplication {
    environment {
      config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
      println("config = ${config.config("ktor").property("environment").getString()}")
    }
    val config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val jwtConfig: JwtConfig = JwtConfig(
      audience = config.config("jwt").property("audience").getString(),
      issuer = config.config("jwt").property("issuer").getString(),
      realm = "Test Realm",
      jwkIssuer = config.config("jwt").property("jwkIssuer").getString(),
      expireSec = 60,
    )
    install(ContentNegotiation) {
      json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(Authentication) {
      jwt("auth-jwt") {
        securitySetup(
          "auth-jwt",
          this@jwt,
          jwtConfig
        )
      }
    }
    routing {
      mainRoute()
    }
    val response = client.get("http://localhost/").body<String>()
    println(response)
    assert(response == "Hello World! This is main page")

  }

  @Test()
  fun `when get authenticated should return 401 Unauthorized`() = testApplication {
    environment {
      config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
      println("config = ${config.config("ktor").property("environment").getString()}")
    }
    val config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    val jwtConfig: JwtConfig = JwtConfig(
      audience = config.config("jwt").property("audience").getString(),
      issuer = config.config("jwt").property("issuer").getString(),
      realm = "Test Realm",
      jwkIssuer = config.config("jwt").property("jwkIssuer").getString(),
      expireSec = 60,
    )
    install(ContentNegotiation) {
      json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(Authentication) {
      jwt("auth-jwt") {
        securitySetup(
          "auth-jwt",
          this@jwt,
          jwtConfig
        )
      }
    }
    routing {
      mainRoute()
    }
    val httpRes = client.get("http://localhost/authenticated") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }

    // 상태 코드만 401로 검증
    println("/authenticated status: ${httpRes.status}")
    assert(httpRes.status == HttpStatusCode.Unauthorized)

  }

}