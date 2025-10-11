/**
 * @author Devonshin
 * @date 2025-01-19
 */
package com.gatchii.benchmark.jwt

import com.auth0.jwt.algorithms.Algorithm
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
 * JWT 토큰 검증 성능 벤치마크 테스트
 * 
 * 목적:
 * - JWT 토큰 검증 시 알고리즘별 (ES256, HS256) 성능 차이 측정
 * - 다양한 페이로드 크기에 따른 검증 성능 영향 분석
 * - 메모리 사용량과 GC 영향 분석
 * 
 * 실행 방법:
 * ./gradlew jmh
 * 
 * 측정 지표:
 * - Throughput (ops/sec): 초당 처리 가능한 토큰 검증 횟수
 * - Average Time (ms/op): 토큰 검증 평균 소요 시간
 * - GC 횟수 및 GC 시간
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class JwtVerificationBenchmark {

    // ES256 알고리즘용 키 쌍
    private lateinit var ecKeyPair: KeyPair
    private lateinit var ecAlgorithm: Algorithm
    
    // HS256 알고리즘용 시크릿
    private lateinit var hmacSecret: ByteArray
    private lateinit var hmacAlgorithm: Algorithm
    
    // JWT 설정
    private lateinit var jwtConfig: JwtConfig
    
    // 사전 생성된 토큰들 (검증 대상)
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
            expireSec = 300 // 5분 - 검증 가능하도록 짧게 설정
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

        // 검증용 토큰 사전 생성
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

    // ES256 - Small Payload 검증
    @Benchmark
    fun verifyTokenES256Small(): Boolean {
        return JwtHandler.verify(
            token = tokenES256Small,
            algorithm = ecAlgorithm,
            jwtConfig = jwtConfig
        )
    }

    // ES256 - Medium Payload 검증
    @Benchmark
    fun verifyTokenES256Medium(): Boolean {
        return JwtHandler.verify(
            token = tokenES256Medium,
            algorithm = ecAlgorithm,
            jwtConfig = jwtConfig
        )
    }

    // ES256 - Large Payload 검증
    @Benchmark
    fun verifyTokenES256Large(): Boolean {
        return JwtHandler.verify(
            token = tokenES256Large,
            algorithm = ecAlgorithm,
            jwtConfig = jwtConfig
        )
    }

    // HS256 - Small Payload 검증
    @Benchmark
    fun verifyTokenHS256Small(): Boolean {
        return JwtHandler.verify(
            token = tokenHS256Small,
            algorithm = hmacAlgorithm,
            jwtConfig = jwtConfig
        )
    }

    // HS256 - Medium Payload 검증
    @Benchmark
    fun verifyTokenHS256Medium(): Boolean {
        return JwtHandler.verify(
            token = tokenHS256Medium,
            algorithm = hmacAlgorithm,
            jwtConfig = jwtConfig
        )
    }

    // HS256 - Large Payload 검증
    @Benchmark
    fun verifyTokenHS256Large(): Boolean {
        return JwtHandler.verify(
            token = tokenHS256Large,
            algorithm = hmacAlgorithm,
            jwtConfig = jwtConfig
        )
    }
}
