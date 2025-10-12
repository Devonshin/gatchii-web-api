/**
 * @author Devonshin
 * @date 2025-01-19
 */
package com.gatchii.benchmark.database

import com.gatchii.domain.jwk.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.openjdk.jmh.annotations.*
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * JWK 조회 쿼리 성능 벤치마크 테스트
 * 
 * 목적:
 * - JWK(JSON Web Key) 조회 쿼리 성능 측정
 * - 페이지네이션 쿼리 효율성 검증
 * - 랜덤 JWK 선택 쿼리 성능 분석
 * - Active/Deleted 상태별 쿼리 성능 비교
 * 
 * 실행 방법:
 * ./gradlew jmh --includes=".*JwkQueryBenchmark.*"
 * 
 * 측정 지표:
 * - Throughput (ops/sec): 초당 처리 가능한 쿼리 수
 * - Average Time (ms/op): 쿼리 평균 소요 시간
 * - GC 영향: 메모리 사용 패턴
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class JwkQueryBenchmark {

    companion object {
        const val DB_URL = "jdbc:h2:mem:jwk_benchmark;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        const val SMALL_DATASET_SIZE = 20    // 실제 운영 환경 JWK 수와 유사
        const val MEDIUM_DATASET_SIZE = 100
        const val LARGE_DATASET_SIZE = 500
    }

    private lateinit var dataSource: HikariDataSource
    private lateinit var repository: JwkRepository
    
    // 테스트용 JWK ID 목록
    private lateinit var activeJwkIds: List<UUID>
    private lateinit var deletedJwkIds: List<UUID>
    private lateinit var firstJwkId: UUID
    private lateinit var lastJwkId: UUID

    @Setup(Level.Trial)
    fun setupDatabase() {
        // HikariCP 설정
        val config = HikariConfig().apply {
            jdbcUrl = DB_URL
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
        }
        dataSource = HikariDataSource(config)
        
        // Exposed Database 연결
        Database.connect(dataSource)
        
        // Repository 초기화
        repository = JwkRepositoryImpl(JwkTable)
        
        // 테이블 생성
        transaction {
            SchemaUtils.create(JwkTable)
        }
    }

    @Setup(Level.Iteration)
    fun setupSmallDataset() {
        transaction {
            // 기존 데이터 삭제
            SchemaUtils.drop(JwkTable)
            SchemaUtils.create(JwkTable)
            
            val activeIds = mutableListOf<UUID>()
            val deletedIds = mutableListOf<UUID>()
            
            // Small 데이터셋 생성 (20개 Active, 5개 Deleted)
            repeat(SMALL_DATASET_SIZE) { i ->
                val jwk = createTestJwk(JwkStatus.ACTIVE)
                val created = runBlocking { repository.create(jwk) }
                activeIds.add(created.id!!)
            }
            
            // Deleted JWK 생성
            repeat(5) { i ->
                val jwk = createTestJwk(JwkStatus.DELETED)
                val created = runBlocking { repository.create(jwk) }
                deletedIds.add(created.id!!)
            }
            
            activeJwkIds = activeIds.sorted()
            deletedJwkIds = deletedIds
            firstJwkId = activeIds.last()  // DESC 정렬이므로 가장 최근 ID
            lastJwkId = activeIds.first()   // 가장 오래된 ID
        }
    }

    @TearDown(Level.Trial)
    fun teardown() {
        transaction {
            SchemaUtils.drop(JwkTable)
        }
        dataSource.close()
    }

    // 테스트 JWK 생성 헬퍼 함수
    private fun createTestJwk(status: JwkStatus = JwkStatus.ACTIVE): JwkModel {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGenerator.generateKeyPair()
        
        return JwkModel(
            id = UUID.randomUUID(),
            privateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded),
            publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded),
            status = status,
            createdAt = OffsetDateTime.now(),
            deletedAt = if (status == JwkStatus.DELETED) OffsetDateTime.now() else null
        )
    }

    /**
     * 랜덤 JWK 조회 (가장 빈번한 케이스)
     */
    @Benchmark
    fun getUsableOneRandom(): JwkModel? {
        return transaction {
            // 랜덤 인덱스 선택 (0-19)
            val randomIdx = (0 until SMALL_DATASET_SIZE).random()
            repository.getUsableOne(randomIdx)
        }
    }

    /**
     * 첫 번째 JWK 조회
     */
    @Benchmark
    fun getUsableFirst(): JwkModel? {
        return transaction {
            repository.getUsableOne(0)
        }
    }

    /**
     * 모든 Active JWK 조회
     */
    @Benchmark
    fun findAllActiveJwks(): List<JwkModel> {
        return runBlocking {
            repository.findAll()
        }
    }

    /**
     * ID로 JWK 조회
     */
    @Benchmark
    fun findJwkById(): JwkModel? {
        return runBlocking {
            repository.read(firstJwkId)
        }
    }

    /**
     * 페이지네이션 - 첫 페이지 조회 (limit 10)
     */
    @Benchmark
    fun getAllUsableFirstPage(): com.gatchii.common.model.ResultData<JwkModel> {
        return transaction {
            repository.getAllUsable(
                id = null,
                forward = true,
                limit = 10,
                withDeleted = false
            )
        }
    }

    /**
     * 페이지네이션 - 다음 페이지 조회
     */
    @Benchmark
    fun getAllUsableNextPage(): com.gatchii.common.model.ResultData<JwkModel> {
        return transaction {
            repository.getAllUsable(
                id = firstJwkId,
                forward = true,
                limit = 10,
                withDeleted = false
            )
        }
    }

    /**
     * 페이지네이션 - Deleted 포함 조회
     */
    @Benchmark
    fun getAllUsableWithDeleted(): com.gatchii.common.model.ResultData<JwkModel> {
        return transaction {
            repository.getAllUsable(
                id = null,
                forward = true,
                limit = 10,
                withDeleted = true
            )
        }
    }

    /**
     * JWK 생성
     */
    @Benchmark
    fun createJwk(): JwkModel {
        return runBlocking {
            val newJwk = createTestJwk(JwkStatus.ACTIVE)
            repository.create(newJwk)
        }
    }

    /**
     * JWK 소프트 삭제
     */
    @Benchmark
    fun deleteJwk(): Unit {
        return runBlocking {
            // 매번 새로운 JWK를 생성하고 삭제
            val newJwk = createTestJwk(JwkStatus.ACTIVE)
            val created = repository.create(newJwk)
            repository.delete(created.id)
        }
    }

    /**
     * Baseline - 단순 트랜잭션 오버헤드 측정
     */
    @Benchmark
    fun baselineTransactionOverhead(): Int {
        return transaction {
            1 + 1
        }
    }
}
