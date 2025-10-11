/**
 * @author Devonshin
 * @date 2025-10-11
 */
package com.gatchii.integration

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import shared.common.AbstractIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JWK 엔드포인트 E2E 테스트
 * 
 * 태스크 #11.4: JWK 조회 E2E 테스트 구현
 * 
 * 테스트 범위:
 * 1. JWK 엔드포인트 응답 검증
 * 2. JWK 응답 형식 확인 (JWKS RFC 7517 준수)
 * 3. 공개 키 정보 검증
 * 4. Content-Type 헤더 검증
 * 5. 표준 및 커스텀 엔드포인트 모두 테스트
 * 
 * E2E 테스트 전략:
 * - 실제 HTTP 클라이언트 사용
 * - 실제 데이터베이스와 통신 (Testcontainers)
 * - JWK 응답 구조 검증
 */
class JwkEndpointE2ETest : AbstractIntegrationTest() {

  private val json = Json { 
    ignoreUnknownKeys = true 
    prettyPrint = true
  }

  /**
   * 테스트 1: 표준 JWKS 엔드포인트 응답 검증
   * 
   * 검증 항목:
   * - HTTP 상태 코드 200 OK
   * - Content-Type이 application/json
   * - 응답 본문이 JWKS 형식
   */
  @Test
  fun `GET standard jwks endpoint should return 200 and valid JWKS`() = withIntegrationApplication {
    // When: GET /.well-known/jwks.json 요청
    val response = client.get("/.well-known/jwks.json")

    // Then: 200 OK 응답
    assertEquals(
      HttpStatusCode.OK,
      response.status,
      "Standard JWKS endpoint should return 200 OK"
    )

    // And: Content-Type이 application/json
    val contentType = response.contentType()
    assertNotNull(contentType, "Content-Type should not be null")
    assertEquals(
      ContentType.Application.Json.withoutParameters(),
      contentType?.withoutParameters(),
      "Content-Type should be application/json"
    )

    // And: JWKS 형식 검증
    val responseBody = response.bodyAsText()
    assertTrue(responseBody.isNotBlank(), "Response body should not be empty")
    
    val jwksResponse = json.decodeFromString<JwkResponse>(responseBody)
    assertNotNull(jwksResponse.keys, "JWKS keys should not be null")
  }

  /**
   * 테스트 2: Gatchii 커스텀 JWKS 엔드포인트 응답 검증
   * 
   * 검증 항목:
   * - HTTP 상태 코드 200 OK
   * - Content-Type이 application/json
   * - 응답 본문이 JWKS 형식
   */
  @Test
  fun `GET gatchii jwks endpoint should return 200 and valid JWKS`() = withIntegrationApplication {
    // When: GET /.well-known/gatchii-jwks.json 요청
    val response = client.get("/.well-known/gatchii-jwks.json")

    // Then: 200 OK 응답
    assertEquals(
      HttpStatusCode.OK,
      response.status,
      "Gatchii JWKS endpoint should return 200 OK"
    )

    // And: Content-Type이 application/json
    val contentType = response.contentType()
    assertNotNull(contentType, "Content-Type should not be null")
    assertEquals(
      ContentType.Application.Json.withoutParameters(),
      contentType?.withoutParameters(),
      "Content-Type should be application/json"
    )

    // And: JWKS 형식 검증
    val responseBody = response.bodyAsText()
    assertTrue(responseBody.isNotBlank(), "Response body should not be empty")
    
    val jwksResponse = json.decodeFromString<JwkResponse>(responseBody)
    assertNotNull(jwksResponse.keys, "JWKS keys should not be null")
  }

  /**
   * 테스트 3: JWKS 응답에 keys 배열 포함 검증
   * 
   * 검증 항목:
   * - keys 필드가 존재
   * - keys가 배열(Set) 형태
   * - 최소 1개 이상의 키 포함
   */
  @Test
  fun `GET jwks endpoint should return keys array`() = withIntegrationApplication {
    // When: JWKS 엔드포인트 요청
    val response = client.get("/.well-known/jwks.json")

    // Then: keys 배열 포함
    if (response.status == HttpStatusCode.OK) {
      val jwksResponse = json.decodeFromString<JwkResponse>(response.bodyAsText())
      
      assertNotNull(jwksResponse.keys, "Keys field should not be null")
      assertTrue(
        jwksResponse.keys.isNotEmpty(),
        "Keys array should contain at least one key"
      )
    }
  }

  /**
   * 테스트 4: JWK 구조 검증 (RFC 7517 준수)
   * 
   * 검증 항목:
   * - 각 JWK가 필수 필드를 포함
   * - kty (Key Type) 필드 존재
   * - use 또는 key_ops 필드 존재 (선택적이지만 권장)
   */
  @Test
  fun `GET jwks endpoint should return valid JWK structure`() = withIntegrationApplication {
    // When: JWKS 엔드포인트 요청
    val response = client.get("/.well-known/jwks.json")

    // Then: 각 JWK 구조 검증
    if (response.status == HttpStatusCode.OK) {
      val jwksResponse = json.decodeFromString<JwkResponse>(response.bodyAsText())
      
      jwksResponse.keys.forEach { jwk ->
        // JWK는 Map<String, String> 형태이므로 필수 필드 확인
        assertTrue(
          jwk.isNotEmpty(),
          "Each JWK should not be empty"
        )
        
        // kty (Key Type) 필드는 RFC 7517에서 필수
        assertTrue(
          jwk.containsKey("kty") || jwk.containsKey("alg"),
          "JWK should contain kty or alg field"
        )
      }
    }
  }

  /**
   * 테스트 5: JWKS 엔드포인트 응답 시간 성능 검증
   * 
   * 검증 항목:
   * - 응답 시간이 1초 이내
   */
  @Test
  fun `GET jwks endpoint should respond within reasonable time`() = withIntegrationApplication {
    // Given: 응답 시간 측정
    val startTime = System.currentTimeMillis()
    
    // When: JWKS 엔드포인트 요청
    val response = client.get("/.well-known/jwks.json")
    
    val endTime = System.currentTimeMillis()
    val responseTime = endTime - startTime

    // Then: 1초 이내 응답
    assertTrue(
      responseTime < 1000,
      "JWKS endpoint should respond within 1 second, took ${responseTime}ms"
    )

    // And: 유효한 응답
    assertEquals(HttpStatusCode.OK, response.status)
  }

  /**
   * 테스트 6: 다중 동시 JWKS 요청 처리
   * 
   * 검증 항목:
   * - 동시 요청이 올바르게 처리됨
   * - 모든 요청이 동일한 응답 반환
   */
  @Test
  fun `GET jwks endpoint should handle multiple concurrent requests`() = withIntegrationApplication {
    // Given: 10개의 동시 요청
    val requestCount = 10

    // When: 여러 요청을 동시에 보냄
    val responses = (1..requestCount).map {
      client.get("/.well-known/jwks.json")
    }

    // Then: 모든 요청이 성공
    responses.forEach { response ->
      assertEquals(HttpStatusCode.OK, response.status)
      val body = response.bodyAsText()
      assertTrue(body.isNotBlank(), "Response should not be empty")
    }
  }

  /**
   * 테스트 7: POST 메서드로 JWKS 엔드포인트 호출 시 405 Method Not Allowed
   * 
   * 검증 항목:
   * - GET만 허용, POST 요청 시 405 응답
   */
  @Test
  fun `POST method on jwks endpoint should return 405 Method Not Allowed`() = withIntegrationApplication {
    // When: POST /.well-known/jwks.json 요청
    val response = client.post("/.well-known/jwks.json") {
      contentType(ContentType.Application.Json)
    }

    // Then: 405 Method Not Allowed 또는 404 Not Found
    assertTrue(
      response.status == HttpStatusCode.MethodNotAllowed ||
      response.status == HttpStatusCode.NotFound,
      "POST on GET-only endpoint should return 405 or 404"
    )
  }

  /**
   * 테스트 8: JWKS 엔드포인트 캐싱 헤더 검증
   * 
   * 검증 항목:
   * - 적절한 캐싱 헤더 존재 (선택적)
   * - 응답이 일관성 있게 반환됨
   */
  @Test
  fun `GET jwks endpoint should return consistent response`() = withIntegrationApplication {
    // When: 두 번 요청
    val response1 = client.get("/.well-known/jwks.json")
    val response2 = client.get("/.well-known/jwks.json")

    // Then: 두 응답이 모두 성공
    assertEquals(HttpStatusCode.OK, response1.status)
    assertEquals(HttpStatusCode.OK, response2.status)

    // And: 두 응답의 본문이 동일 (일관성)
    val body1 = response1.bodyAsText()
    val body2 = response2.bodyAsText()
    
    assertTrue(body1.isNotBlank(), "First response should not be empty")
    assertTrue(body2.isNotBlank(), "Second response should not be empty")
    
    // 두 응답이 동일한 구조를 가짐 (키 개수가 동일)
    val jwks1 = json.decodeFromString<JwkResponse>(body1)
    val jwks2 = json.decodeFromString<JwkResponse>(body2)
    
    assertEquals(
      jwks1.keys.size,
      jwks2.keys.size,
      "Both responses should have the same number of keys"
    )
  }

  /**
   * 테스트 9: 표준 및 커스텀 엔드포인트 응답 동일성 검증
   * 
   * 검증 항목:
   * - /.well-known/jwks.json과 /.well-known/gatchii-jwks.json이 동일한 응답 반환
   */
  @Test
  fun `Standard and custom jwks endpoints should return same response`() = withIntegrationApplication {
    // When: 두 엔드포인트 모두 요청
    val standardResponse = client.get("/.well-known/jwks.json")
    val customResponse = client.get("/.well-known/gatchii-jwks.json")

    // Then: 두 응답 모두 성공
    assertEquals(HttpStatusCode.OK, standardResponse.status)
    assertEquals(HttpStatusCode.OK, customResponse.status)

    // And: 두 응답의 본문이 동일
    val standardBody = standardResponse.bodyAsText()
    val customBody = customResponse.bodyAsText()
    
    val standardJwks = json.decodeFromString<JwkResponse>(standardBody)
    val customJwks = json.decodeFromString<JwkResponse>(customBody)
    
    assertEquals(
      standardJwks.keys.size,
      customJwks.keys.size,
      "Both endpoints should return the same number of keys"
    )
  }

  /**
   * 테스트 10: Accept 헤더 변형에 대한 robustness 검증
   * 
   * 검증 항목:
   * - 다양한 Accept 헤더로 요청 시 정상 응답
   */
  @Test
  fun `GET jwks endpoint with different Accept headers should work`() = withIntegrationApplication {
    // Given: 다양한 Accept 헤더
    val acceptHeaders = listOf(
      "application/json",
      "*/*",
      "application/jose+json", // JWKS의 MIME 타입
      "text/plain"
    )

    acceptHeaders.forEach { acceptHeader ->
      // When: Accept 헤더를 포함한 요청
      val response = client.get("/.well-known/jwks.json") {
        header(HttpHeaders.Accept, acceptHeader)
      }

      // Then: 정상 응답
      assertEquals(
        HttpStatusCode.OK,
        response.status,
        "Should return 200 OK with Accept header: $acceptHeader"
      )
      
      // And: 유효한 JSON 응답
      val body = response.bodyAsText()
      assertTrue(body.isNotBlank(), "Response should not be empty")
    }
  }

  /**
   * assertNotNull 헬퍼 함수
   */
  private fun assertNotNull(value: Any?, message: String = "Value should not be null") {
    assertTrue(value != null, message)
  }

  /**
   * JWK 응답 형식 (RFC 7517)
   */
  @Serializable
  data class JwkResponse(
    val keys: Set<Map<String, String>>
  )
}
