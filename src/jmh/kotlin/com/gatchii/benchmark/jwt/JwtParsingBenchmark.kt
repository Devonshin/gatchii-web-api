/**
 * @author Devonshin
 * @date 2025-01-19
 */
package com.gatchii.benchmark.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.gatchii.plugins.JwtConfig
import com.gatchii.utils.JwtHandler
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.openjdk.jmh.annotations.*
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * JWT 토큰 파싱 성능 벤치마크 테스트
 * 
 * 목적:
 * - JWT 토큰 파싱(디코딩) 및 클레임 추출 성능 측정
 * - 다양한 페이로드 크기에 따른 파싱 성능 영향 분석
 * - 메모리 사용량과 GC 영향 분석
 * 
 * 참고:
 * - 파싱은 서명 검증 없이 토큰을 디코딩하는 과정
 * - 실제 운영 환경에서는 반드시 서명 검증 후 파싱해야 함
 * 
 * 실행 방법:
 * ./gradlew jmh
 * 
 * 측정 지표:
 * - Throughput (ops/sec): 초당 처리 가능한 토큰 파싱 횟수
 * - Average Time (ms/op): 토큰 파싱 평균 소요 시간
 * - GC 횟수 및 GC 시간
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class JwtParsingBenchmark {

    // ES256 알고리즘용 키 쌍
    private lateinit var ecKeyPair: KeyPair
    private lateinit var ecAlgorithm: Algorithm
    
    // HS256 알고리즘용 시크릿
    private lateinit var hmacSecret: ByteArray
    private lateinit var hmacAlgorithm: Algorithm
    
    // JWT 설정
    private lateinit var jwtConfig: JwtConfig
    
    // 사전 생성된 토큰들 (파싱 대상)
    private lateinit var tokenES256Small: String
    private lateinit var tokenES256Medium: String
    private lateinit var tokenES256Large: String
    private lateinit var tokenHS256Small: String
    private lateinit var tokenHS256Medium: String
    private lateinit var tokenHS256Large: String

    @Setup
    fun setup() {
        // BouncyCastle Provider 추가
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        // ES256 (ECDSA with P-256 curve) 키 쌍 생성
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        ecKeyPair = keyPairGenerator.generateKeyPair()
        
        ecAlgorithm = Algorithm.ECDSA256(
            ecKeyPair.public as ECPublicKey,
            ecKeyPair.private as ECPrivateKey
        )

        // HS256 (HMAC with SHA-256) 시크릿 생성
        hmacSecret = ByteArray(64)
        SecureRandom().nextBytes(hmacSecret)
        hmacAlgorithm = Algorithm.HMAC256(hmacSecret)

        // JWT 설정 초기화
        jwtConfig = JwtConfig(
            audience = "test-audience",
            issuer = "test-issuer",
            realm = "test-realm",
            jwkIssuer = "test-jwk-issuer",
            expireSec = 3600
        )

        // Small payload: 기본 사용자 정보 (~100 bytes)
        val smallPayload = mapOf(
            "userId" to "user123",
            "username" to "testuser",
            "role" to "USER"
        )

        // Medium payload: 추가 메타데이터 포함 (~500 bytes)
        val mediumPayload = smallPayload + mapOf(
            "email" to "[email protected]",
            "firstName" to "Test",
            "lastName" to "User",
            "department" to "Engineering",
            "location" to "Seoul",
            "phoneNumber" to "+82-10-1234-5678",
            "metadata" to "Additional metadata information for testing purposes"
        )

        // Large payload: 상세한 권한 및 설정 정보 포함 (~2KB)
        val largePayload = mediumPayload + mapOf(
            "permissions" to "read,write,delete,admin,manage_users,manage_roles,view_analytics,export_data",
            "preferences" to """{"theme":"dark","language":"ko","timezone":"Asia/Seoul","notifications":true}""",
            "sessionData" to "x".repeat(1000),
            "customField1" to "value1",
            "customField2" to "value2",
            "customField3" to "value3"
        )

        // 파싱용 토큰 사전 생성
        tokenES256Small = JwtHandler.generate(
            jwtId = UUID.randomUUID().toString(),
            claim = smallPayload,
            algorithm = ecAlgorithm,
            jwtConfig = jwtConfig
        )

        tokenES256Medium = JwtHandler.generate(
            jwtId = UUID.randomUUID().toString(),
            claim = mediumPayload,
            algorithm = ecAlgorithm,
            jwtConfig = jwtConfig
        )

        tokenES256Large = JwtHandler.generate(
            jwtId = UUID.randomUUID().toString(),
            claim = largePayload,
            algorithm = ecAlgorithm,
            jwtConfig = jwtConfig
        )

        tokenHS256Small = JwtHandler.generate(
            jwtId = UUID.randomUUID().toString(),
            claim = smallPayload,
            algorithm = hmacAlgorithm,
            jwtConfig = jwtConfig
        )

        tokenHS256Medium = JwtHandler.generate(
            jwtId = UUID.randomUUID().toString(),
            claim = mediumPayload,
            algorithm = hmacAlgorithm,
            jwtConfig = jwtConfig
        )

        tokenHS256Large = JwtHandler.generate(
            jwtId = UUID.randomUUID().toString(),
            claim = largePayload,
            algorithm = hmacAlgorithm,
            jwtConfig = jwtConfig
        )
    }

    // ES256 - Small Payload 파싱
    @Benchmark
    fun parseTokenES256Small(): DecodedJWT {
        return JwtHandler.convert(tokenES256Small)
    }

    // ES256 - Medium Payload 파싱
    @Benchmark
    fun parseTokenES256Medium(): DecodedJWT {
        return JwtHandler.convert(tokenES256Medium)
    }

    // ES256 - Large Payload 파싱
    @Benchmark
    fun parseTokenES256Large(): DecodedJWT {
        return JwtHandler.convert(tokenES256Large)
    }

    // HS256 - Small Payload 파싱
    @Benchmark
    fun parseTokenHS256Small(): DecodedJWT {
        return JwtHandler.convert(tokenHS256Small)
    }

    // HS256 - Medium Payload 파싱
    @Benchmark
    fun parseTokenHS256Medium(): DecodedJWT {
        return JwtHandler.convert(tokenHS256Medium)
    }

    // HS256 - Large Payload 파싱
    @Benchmark
    fun parseTokenHS256Large(): DecodedJWT {
        return JwtHandler.convert(tokenHS256Large)
    }

    // 파싱 후 클레임 추출 - ES256 Small
    @Benchmark
    fun parseAndExtractClaimsES256Small(): MutableMap<String, Any> {
        val decoded = JwtHandler.convert(tokenES256Small)
        return JwtHandler.getClaim(decoded)
    }

    // 파싱 후 클레임 추출 - ES256 Medium
    @Benchmark
    fun parseAndExtractClaimsES256Medium(): MutableMap<String, Any> {
        val decoded = JwtHandler.convert(tokenES256Medium)
        return JwtHandler.getClaim(decoded)
    }

    // 파싱 후 클레임 추출 - ES256 Large
    @Benchmark
    fun parseAndExtractClaimsES256Large(): MutableMap<String, Any> {
        val decoded = JwtHandler.convert(tokenES256Large)
        return JwtHandler.getClaim(decoded)
    }

    // 파싱 후 클레임 추출 - HS256 Small
    @Benchmark
    fun parseAndExtractClaimsHS256Small(): MutableMap<String, Any> {
        val decoded = JwtHandler.convert(tokenHS256Small)
        return JwtHandler.getClaim(decoded)
    }

    // 파싱 후 클레임 추출 - HS256 Medium
    @Benchmark
    fun parseAndExtractClaimsHS256Medium(): MutableMap<String, Any> {
        val decoded = JwtHandler.convert(tokenHS256Medium)
        return JwtHandler.getClaim(decoded)
    }

    // 파싱 후 클레임 추출 - HS256 Large
    @Benchmark
    fun parseAndExtractClaimsHS256Large(): MutableMap<String, Any> {
        val decoded = JwtHandler.convert(tokenHS256Large)
        return JwtHandler.getClaim(decoded)
    }
}
