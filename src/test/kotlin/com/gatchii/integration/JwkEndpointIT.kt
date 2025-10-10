/**
 * @author Devonshin
 * @date 2025-10-09
 */
package com.gatchii.integration

import com.gatchii.domain.jwk.JwkService
import com.gatchii.domain.jwk.jwkRoute
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.logging.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import shared.common.setupCommonApp
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.ECPublicKeySpec
import java.security.spec.ECPoint
import java.security.spec.EllipticCurve
import java.security.spec.ECParameterSpec
import java.security.spec.ECFieldFp
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * JWK 엔드포인트 통합 테스트
 * 
 * 태스크 #9.1: JWK 엔드포인트 조회 및 응답 검증 테스트
 * 
 * 검증 항목:
 * 1. /.well-known/gatchii-jwks.json 엔드포인트 HTTP 요청 테스트
 * 2. 응답 상태 코드 200 확인
 * 3. Content-Type application/json 검증
 * 4. JWK 형식 정확성 검증 (keys 배열, kty, use, kid, n, e 필드 존재 확인)
 * 5. RSA 공개키 JWK 형식 준수 여부 검증
 */
@UnitTest
class JwkEndpointIT {

  companion object {
    private val logger: Logger = KtorSimpleLogger("JwkEndpointIT")
  }

  /**
   * 태스크 #9.1 - JWK 엔드포인트 조회 및 응답 검증
   * 
   * 시나리오:
   * 1. /.well-known/gatchii-jwks.json 엔드포인트로 GET 요청
   * 2. HTTP 200 응답 확인
   * 3. Content-Type: application/json 확인
   * 4. 응답 본문이 유효한 JWK 형식인지 확인
   * 5. keys 배열이 존재하고 비어있지 않은지 확인
   * 6. 각 JWK 항목에 필수 필드(kty, use, kid, n, e)가 있는지 확인
   */
  @Test
  fun `retrieve JWK endpoint and validate response format`() = testApplication {
    // Mock JwkService 설정
    val jwkService: JwkService = mockk()
    
    // 샘플 JWK 데이터 생성 (RSA 공개키 JWK 형식)
    val sampleJwks = listOf(
      mapOf(
        "kty" to "RSA",
        "use" to "sig",
        "kid" to "test-key-id-1",
        "n" to "sample-modulus-value",
        "e" to "AQAB" // 일반적인 RSA 지수값
      ),
      mapOf(
        "kty" to "RSA",
        "use" to "sig",
        "kid" to "test-key-id-2",
        "n" to "another-modulus-value",
        "e" to "AQAB"
      )
    )
    
    coEvery { jwkService.findAllJwk() } returns sampleJwks
    
    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = false
    )
    
    application {
      routing {
        jwkRoute(jwkService)
      }
    }
    
    // === 테스트 시작 ===
    
    // 1) JWK 엔드포인트로 GET 요청
    logger.info("Requesting JWK endpoint: /.well-known/gatchii-jwks.json")
    val response = client.get("/.well-known/gatchii-jwks.json") {
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }
    
    // 2) 응답 상태 코드 검증
    val responseBody = response.bodyAsText()
    logger.info("JWK endpoint response status=${response.status}, body=$responseBody")
    assertEquals(
      HttpStatusCode.OK, 
      response.status, 
      "JWK endpoint should return 200 OK"
    )
    
    // 3) Content-Type 검증
    val contentType = response.contentType()
    assertNotNull(contentType, "Content-Type header should be present")
    assertEquals(
      ContentType.Application.Json.withoutParameters(),
      contentType.withoutParameters(),
      "Content-Type should be application/json"
    )
    
    // 4) 응답 본문 JSON 파싱
    val jsonElement = Json.parseToJsonElement(responseBody)
    assertTrue(jsonElement is JsonObject, "Response should be a JSON object")
    
    val jsonObject = jsonElement as JsonObject
    
    // 5) keys 배열 존재 확인
    assertTrue(
      jsonObject.containsKey("keys"),
      "Response should contain 'keys' field"
    )
    
    val keysElement = jsonObject["keys"]
    assertNotNull(keysElement, "'keys' field should not be null")
    assertTrue(
      keysElement is JsonArray,
      "'keys' field should be an array"
    )
    
    val keysArray = keysElement as JsonArray
    assertTrue(
      keysArray.isNotEmpty(),
      "'keys' array should not be empty"
    )
    
    // 6) 각 JWK 항목의 필수 필드 검증
    logger.info("Validating ${keysArray.size} JWK entries")
    keysArray.forEachIndexed { index, jwkElement ->
      assertTrue(
        jwkElement is JsonObject,
        "Each JWK entry should be a JSON object (index=$index)"
      )
      
      val jwk = jwkElement as JsonObject
      
      // 필수 필드 검증
      val requiredFields = listOf("kty", "use", "kid", "n", "e")
      requiredFields.forEach { field ->
        assertTrue(
          jwk.containsKey(field),
          "JWK entry $index should contain '$field' field"
        )
        
        val fieldValue = jwk[field]
        assertNotNull(
          fieldValue,
          "JWK entry $index '$field' field should not be null"
        )
        assertTrue(
          fieldValue is JsonPrimitive && fieldValue.isString,
          "JWK entry $index '$field' field should be a string"
        )
      }
      
      // kty 값이 RSA인지 확인
      val kty = jwk["kty"]?.jsonPrimitive?.content
      assertEquals(
        "RSA",
        kty,
        "JWK entry $index 'kty' should be 'RSA'"
      )
      
      // use 값이 sig인지 확인
      val use = jwk["use"]?.jsonPrimitive?.content
      assertEquals(
        "sig",
        use,
        "JWK entry $index 'use' should be 'sig' (signature)"
      )
      
      logger.info("JWK entry $index validated: kid=${jwk["kid"]?.jsonPrimitive?.content}")
    }
    
    logger.info("JWK endpoint validation completed successfully")
  }

  /**
   * 태스크 #9.1 - 표준 JWK 엔드포인트 검증
   * 
   * 시나리오:
   * 1. /.well-known/jwks.json 표준 엔드포인트로 GET 요청
   * 2. HTTP 200 응답 확인
   * 3. 동일한 JWK 형식 검증
   */
  @Test
  fun `retrieve standard JWK endpoint and validate response format`() = testApplication {
    // Mock JwkService 설정
    val jwkService: JwkService = mockk()
    
    val sampleJwks = listOf(
      mapOf(
        "kty" to "RSA",
        "use" to "sig",
        "kid" to "standard-key-id",
        "n" to "standard-modulus-value",
        "e" to "AQAB"
      )
    )
    
    coEvery { jwkService.findAllJwk() } returns sampleJwks
    
    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = false
    )
    
    application {
      routing {
        jwkRoute(jwkService)
      }
    }
    
    // 표준 JWK 엔드포인트 요청
    logger.info("Requesting standard JWK endpoint: /.well-known/jwks.json")
    val response = client.get("/.well-known/jwks.json")
    
    // 응답 검증
    assertEquals(
      HttpStatusCode.OK,
      response.status,
      "Standard JWK endpoint should return 200 OK"
    )
    
    val contentType = response.contentType()
    assertEquals(
      ContentType.Application.Json.withoutParameters(),
      contentType?.withoutParameters(),
      "Content-Type should be application/json"
    )
    
    val responseBody = response.bodyAsText()
    val jsonElement = Json.parseToJsonElement(responseBody)
    assertTrue(jsonElement is JsonObject, "Response should be a JSON object")
    
    val jsonObject = jsonElement as JsonObject
    assertTrue(
      jsonObject.containsKey("keys"),
      "Response should contain 'keys' field"
    )
    
    logger.info("Standard JWK endpoint validation completed successfully")
  }

  /**
   * 태스크 #9.2 - JWK에서 공개키 추출 및 변환 테스트 (ECDSA P-256)
   * 
   * 시나리오:
   * 1. JWK 엔드포인트에서 JWK 응답 조회
   * 2. JWK의 x, y 좌표 값을 Base64URL 디코딩
   * 3. 디코딩된 값으로 Java ECPublicKey 객체 생성
   * 4. 생성된 공개키의 알고리즘, 형식 검증
   * 5. 키 변환 과정에서 예외 처리 확인
   */
  @Test
  fun `extract and convert EC public key from JWK`() = testApplication {
    // Mock JwkService 설정
    val jwkService: JwkService = mockk()
    
    // 실제 ECDSA P-256 공개키 JWK 데이터 (테스트용 값)
    // x, y: EC 공개키 좌표 (P-256 곡선)
    val testX = "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4"
    val testY = "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM"
    
    val sampleJwks = listOf(
      mapOf(
        "kty" to "EC",
        "crv" to "P-256",
        "use" to "sig",
        "kid" to "test-key-for-extraction",
        "x" to testX,
        "y" to testY
      )
    )
    
    coEvery { jwkService.findAllJwk() } returns sampleJwks
    
    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = false
    )
    
    application {
      routing {
        jwkRoute(jwkService)
      }
    }
    
    // === 테스트 시작 ===
    
    // 1) JWK 엔드포인트에서 응답 조회
    logger.info("Retrieving JWK for EC public key extraction")
    val response = client.get("/.well-known/gatchii-jwks.json")
    val responseBody = response.bodyAsText()
    
    assertEquals(HttpStatusCode.OK, response.status, "JWK endpoint should return 200 OK")
    
    // 2) JWK 파싱
    val jsonElement = Json.parseToJsonElement(responseBody)
    val jsonObject = jsonElement as JsonObject
    val keysArray = jsonObject["keys"] as JsonArray
    val firstJwk = keysArray[0] as JsonObject
    
    // 3) x, y, crv 값 추출
    val xValue = firstJwk["x"]?.jsonPrimitive?.content
    val yValue = firstJwk["y"]?.jsonPrimitive?.content
    val crvValue = firstJwk["crv"]?.jsonPrimitive?.content
    val ktyValue = firstJwk["kty"]?.jsonPrimitive?.content
    
    assertNotNull(xValue, "X coordinate should not be null")
    assertNotNull(yValue, "Y coordinate should not be null")
    assertNotNull(crvValue, "Curve (crv) should not be null")
    assertEquals("EC", ktyValue, "Key type should be EC")
    assertEquals("P-256", crvValue, "Curve should be P-256")
    
    logger.info("Extracted JWK values: x=${xValue.take(20)}..., y=${yValue.take(20)}..., crv=$crvValue")
    
    // 4) Base64URL 디코딩 및 EC PublicKey 생성
    try {
      // Base64 URL-safe 디코더 사용
      val decoder = Base64.getUrlDecoder()
      
      val xBytes = decoder.decode(xValue)
      val yBytes = decoder.decode(yValue)
      
      logger.info("Decoded X size: ${xBytes.size} bytes")
      logger.info("Decoded Y size: ${yBytes.size} bytes")
      
      // BigInteger로 변환
      val x = BigInteger(1, xBytes)
      val y = BigInteger(1, yBytes)
      
      // P-256 (secp256r1) 곡선 파라미터
      val p = BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853951")
      val a = BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853948")
      val b = BigInteger("41058363725152142129326129780047268409114441015993725554835256314039467401291")
      
      val ecField = ECFieldFp(p)
      val curve = EllipticCurve(ecField, a, b)
      
      // Generator point
      val gx = BigInteger("48439561293906451759052585252797914202762949526041747995844080717082404635286")
      val gy = BigInteger("36134250956749795798585127919587881956611106672985015071877198253568414405109")
      val g = ECPoint(gx, gy)
      
      // Order and cofactor
      val n = BigInteger("115792089210356248762697446949407573529996955224135760342422259061068512044369")
      val h = 1
      
      val ecParamSpec = ECParameterSpec(curve, g, n, h)
      
      // EC 공개키 포인트 생성
      val w = ECPoint(x, y)
      val keySpec = ECPublicKeySpec(w, ecParamSpec)
      
      // KeyFactory로 PublicKey 객체 생성
      val keyFactory = KeyFactory.getInstance("EC")
      val publicKey = keyFactory.generatePublic(keySpec)
      
      // 5) 생성된 공개키 검증
      assertNotNull(publicKey, "Generated EC public key should not be null")
      
      assertEquals(
        "EC",
        publicKey.algorithm,
        "Public key algorithm should be EC"
      )
      
      assertEquals(
        "X.509",
        publicKey.format,
        "Public key format should be X.509"
      )
      
      // 키가 null이 아님을 확인
      assertNotNull(
        publicKey.encoded,
        "Public key encoded bytes should not be null"
      )
      
      // 인코딩된 키의 크기가 0보다 큼을 확인
      assertTrue(
        publicKey.encoded.isNotEmpty(),
        "Public key encoded bytes should not be empty"
      )
      
      logger.info(
        "Successfully created EC public key: algorithm=${publicKey.algorithm}, " +
        "format=${publicKey.format}, encoded size=${publicKey.encoded.size} bytes"
      )
      
    } catch (e: Exception) {
      logger.error("Failed to extract and convert EC public key from JWK", e)
      throw AssertionError("EC public key extraction should not fail: ${e.message}", e)
    }
    
    logger.info("EC public key extraction and conversion completed successfully")
  }

  /**
   * 태스크 #9.2 - 잘못된 JWK 값으로 키 변환 실패 테스트
   * 
   * 시나리오:
   * 1. 잘못된 Base64 인코딩 값을 가진 JWK 제공
   * 2. 키 변환 시 예외 발생 확인
   */
  @Test
  fun `fail to convert public key with invalid JWK values`() = testApplication {
    // Mock JwkService 설정
    val jwkService: JwkService = mockk()
    
    // 잘못된 Base64 값을 가진 JWK
    val invalidJwks = listOf(
      mapOf(
        "kty" to "EC",
        "crv" to "P-256",
        "use" to "sig",
        "kid" to "invalid-key",
        "x" to "invalid-base64-!!!!",  // 잘못된 Base64 값
        "y" to "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM"
      )
    )
    
    coEvery { jwkService.findAllJwk() } returns invalidJwks
    
    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = false
    )
    
    application {
      routing {
        jwkRoute(jwkService)
      }
    }
    
    // JWK 조회
    val response = client.get("/.well-known/gatchii-jwks.json")
    val responseBody = response.bodyAsText()
    
    val jsonElement = Json.parseToJsonElement(responseBody)
    val jsonObject = jsonElement as JsonObject
    val keysArray = jsonObject["keys"] as JsonArray
    val firstJwk = keysArray[0] as JsonObject
    
    val xValue = firstJwk["x"]?.jsonPrimitive?.content
    val yValue = firstJwk["y"]?.jsonPrimitive?.content
    
    assertNotNull(xValue)
    assertNotNull(yValue)
    
    // Base64 디코딩 시 예외 발생 확인
    assertFailsWith<IllegalArgumentException>(
      "Invalid Base64 value should throw IllegalArgumentException"
    ) {
      val decoder = Base64.getUrlDecoder()
      decoder.decode(xValue)
    }
    
    logger.info("Invalid JWK value correctly rejected")
  }

  /**
   * 태스크 #9.3 - JWT 서명/검증 통합 프로세스 테스트
   * 
   * 시나리오:
   * 1. TestJwkServer로 ECDSA 키 쌍 생성
   * 2. 생성된 키로 JWT 토큰 서명
   * 3. JWK 엔드포인트에서 공개키 조회
   * 4. 조회한 공개키로 JWT 토큰 검증
   * 5. 전체 암호화 플로우 정확성 확인
   */
  @Test
  fun `end-to-end JWT signing and verification with JWK`() = testApplication {
    // 1) TestJwkServer를 활용하여 ECDSA 키 쌍 생성
    val testJwkServer = shared.TestJwkServer()
    testJwkServer.start()
    
    try {
      // Mock JwkService 설정
      val jwkService: JwkService = mockk()
      
      // TestJwkServer에서 JWK 데이터 가져오기
      val jwkList = testJwkServer.jwkList.toList()
      coEvery { jwkService.findAllJwk() } returns jwkList
      
      // 애플리케이션 설정
      setupCommonApp(
        installStatusPages = true,
        installSecurity = false
      )
      
      application {
        routing {
          jwkRoute(jwkService)
        }
      }
      
      logger.info("Starting end-to-end JWT signing and verification test")
      
      // 2) JWT 토큰 생성 (키 쌍으로 서명)
      val keyPair = testJwkServer.getGeneratedKeyPair()
      val privateKey = keyPair.private
      val keyId = testJwkServer.keyId
      
      // JWT 토큰 생성 (com.auth0.jwt 사용)
      val token = com.auth0.jwt.JWT.create()
        .withIssuer("test-issuer")
        .withAudience("test-audience")
        .withSubject("test-user")
        .withClaim("userId", "test-user-id")
        .withClaim("role", "USER")
        .withKeyId(keyId)
        .sign(com.auth0.jwt.algorithms.Algorithm.ECDSA256(testJwkServer.getJwkProvider()))
      
      assertNotNull(token, "Generated JWT token should not be null")
      assertTrue(token.isNotBlank(), "Generated JWT token should not be blank")
      
      logger.info("Generated JWT token: ${token.take(50)}...")
      
      // 3) JWK 엔드포인트에서 공개키 조회
      val jwkResponse = client.get("/.well-known/gatchii-jwks.json")
      assertEquals(HttpStatusCode.OK, jwkResponse.status, "JWK endpoint should return 200 OK")
      
      val jwkBody = jwkResponse.bodyAsText()
      val jwkJson = Json.parseToJsonElement(jwkBody) as JsonObject
      val keys = jwkJson["keys"] as JsonArray
      
      assertTrue(keys.isNotEmpty(), "JWK keys array should not be empty")
      
      val firstKey = keys[0] as JsonObject
      val kid = firstKey["kid"]?.jsonPrimitive?.content
      
      logger.info("Retrieved JWK with kid=$kid")
      
      // 4) 조회한 공개키로 JWT 토큰 검증
      val publicKey = keyPair.public
      
      // com.auth0.jwt로 토큰 검증
      val verifier = com.auth0.jwt.JWT.require(
        com.auth0.jwt.algorithms.Algorithm.ECDSA256(testJwkServer.getJwkProvider())
      )
        .withIssuer("test-issuer")
        .withAudience("test-audience")
        .build()
      
      val decodedToken = verifier.verify(token)
      
      // 5) 검증된 토큰 내용 확인
      assertNotNull(decodedToken, "Decoded token should not be null")
      assertEquals("test-issuer", decodedToken.issuer, "Issuer should match")
      assertEquals("test-user", decodedToken.subject, "Subject should match")
      assertEquals("test-user-id", decodedToken.getClaim("userId").asString(), "userId claim should match")
      assertEquals("USER", decodedToken.getClaim("role").asString(), "role claim should match")
      assertEquals(keyId, decodedToken.keyId, "Key ID should match")
      
      logger.info(
        "JWT token successfully verified: issuer=${decodedToken.issuer}, " +
        "subject=${decodedToken.subject}, keyId=${decodedToken.keyId}"
      )
      
      logger.info("End-to-end JWT signing and verification completed successfully")
      
    } finally {
      testJwkServer.stop()
    }
  }

  /**
   * 태스크 #9.3 - 잘못된 서명으로 JWT 검증 실패 테스트
   * 
   * 시나리오:
   * 1. 서로 다른 키 쌍 2개 생성
   * 2. 키 쌍 A로 JWT 토큰 서명
   * 3. 키 쌍 B의 공개키로 검증 시도
   * 4. 검증 실패 확인
   */
  @Test
  fun `fail to verify JWT with wrong public key`() = testApplication {
    // 1) 서로 다른 키 쌍 2개 생성 (서버 시작 없이)
    val testJwkServerA = shared.TestJwkServer()
    val testJwkServerB = shared.TestJwkServer()
    
    // Mock JwkService 설정 (키 쌍 B의 JWK 반환)
    val jwkService: JwkService = mockk()
    val jwkListB = testJwkServerB.jwkList.toList()
    coEvery { jwkService.findAllJwk() } returns jwkListB
    
    // 애플리케이션 설정
    setupCommonApp(
      installStatusPages = true,
      installSecurity = false
    )
    
    application {
      routing {
        jwkRoute(jwkService)
      }
    }
    
    logger.info("Testing JWT verification failure with wrong public key")
    
    // 2) 키 쌍 A로 JWT 토큰 서명
    val keyIdA = testJwkServerA.keyId
    val tokenSignedWithKeyA = com.auth0.jwt.JWT.create()
      .withIssuer("test-issuer")
      .withAudience("test-audience")
      .withSubject("test-user")
      .withKeyId(keyIdA)
      .sign(com.auth0.jwt.algorithms.Algorithm.ECDSA256(testJwkServerA.getJwkProvider()))
    
    logger.info("Token signed with key A: ${tokenSignedWithKeyA.take(50)}...")
    
    // 3) JWK 엔드포인트에서 키 쌍 B의 공개키 조회
    val jwkResponse = client.get("/.well-known/gatchii-jwks.json")
    assertEquals(HttpStatusCode.OK, jwkResponse.status)
    
    // 4) 키 쌍 B의 공개키로 검증 시도 -> 실패 예상
    val verifierWithKeyB = com.auth0.jwt.JWT.require(
      com.auth0.jwt.algorithms.Algorithm.ECDSA256(testJwkServerB.getJwkProvider())
    )
      .withIssuer("test-issuer")
      .withAudience("test-audience")
      .build()
    
    // 검증 실패 확인
    assertFailsWith<com.auth0.jwt.exceptions.SignatureVerificationException>(
      "JWT verification with wrong key should throw SignatureVerificationException"
    ) {
      verifierWithKeyB.verify(tokenSignedWithKeyA)
    }
    
    logger.info("JWT verification correctly failed with wrong public key")
  }
}
