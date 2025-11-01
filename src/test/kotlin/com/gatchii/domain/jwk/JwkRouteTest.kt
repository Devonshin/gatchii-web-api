package com.gatchii.domain.jwk

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.DisplayName
import shared.common.IntegrationTest
import shared.common.setupCommonApp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Devonshin
 * @date 2025-09-14
 */
@IntegrationTest
class JwkRouteTest {


  @Test
  @DisplayName("Should return 200 and JSON when request to /.well-known/gatchii-jwks.json")
  fun Should_return_200_and_JSON_when_request_jwks() = testApplication {
    setupCommonApp(installStatusPages = true)
    val fake = mockk<JwkService>()
    coEvery { fake.findAllJwk() } returns listOf(
      mapOf(
        "kty" to "RSA",
        "alg" to "RS256",
        "kid" to "kid-123",
        "use" to "sig",
        "n" to "modulus",
        "e" to "AQAB"
      )
    )
    application {
      routing {
        route("/") {
          jwkRoute(fake)
        }
      }
    }
    val res: HttpResponse = client.get("/.well-known/gatchii-jwks.json")
    assertEquals(HttpStatusCode.OK, res.status)
    // ContentType 비교 시 charset 파라미터 유무로 테스트가 흔들릴 수 있으므로 type/subtype만 비교
    assertEquals(ContentType.Application.Json.contentType, res.contentType()?.contentType)
    assertEquals(ContentType.Application.Json.contentSubtype, res.contentType()?.contentSubtype)

    val json = Json.parseToJsonElement(res.bodyAsText()).jsonObject
    val keys = json["keys"]!!.jsonArray
    assertEquals(1, keys.size)
    val first = keys[0].jsonObject
    assertEquals("RSA", first["kty"]!!.jsonPrimitive.content)
    assertEquals("RS256", first["alg"]!!.jsonPrimitive.content)
    assertEquals("kid-123", first["kid"]!!.jsonPrimitive.content)
  }

  @Test
  @DisplayName("Should return empty keys when no jwk exists")
  fun Should_return_empty_keys_when_no_jwk_exists() = testApplication {
    setupCommonApp(installStatusPages = true)
    val fake = mockk<JwkService>()
    coEvery { fake.findAllJwk() } returns emptyList()
    application {
      routing {
        route("/") { jwkRoute(fake) }
      }
    }
    val res = client.get("/.well-known/gatchii-jwks.json")
    assertEquals(HttpStatusCode.OK, res.status)
    val json = Json.parseToJsonElement(res.bodyAsText()).jsonObject
    val keys = json["keys"]!!.jsonArray
    assertEquals(0, keys.size)
  }
}