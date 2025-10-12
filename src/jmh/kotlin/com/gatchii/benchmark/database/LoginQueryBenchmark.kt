/**
 * @author Devonshin
 * @date 2025-01-19
 */
package com.gatchii.benchmark.database

import com.gatchii.domain.login.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.openjdk.jmh.annotations.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 로그인 쿼리 성능 벤치마크 테스트
 * 
 * 목적:
 * - 사용자 로그인 조회 쿼리 성능 측정
 * - prefixId + suffixId 복합 인덱스 효율성 검증
 * - 다양한 데이터 크기에서의 성능 비교
 * - 쿼리 실행 계획 분석을 위한 baseline 제공
 * 
 * 실행 방법:
 * ./gradlew jmh --includes=".*LoginQueryBenchmark.*"
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
open class LoginQueryBenchmark {

    companion object {
        const val DB_URL = "jdbc:h2:mem:benchmark;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        const val SMALL_DATASET_SIZE = 100
        const val MEDIUM_DATASET_SIZE = 1000
        const val LARGE_DATASET_SIZE = 10000
    }

    private lateinit var dataSource: HikariDataSource
    private lateinit var repository: LoginRepository
    
    // 테스트용 사용자 데이터
    private lateinit var testUserSmall: Pair<String, String>  // Small 데이터셋의 사용자
    private lateinit var testUserMedium: Pair<String, String> // Medium 데이터셋의 사용자
    private lateinit var testUserLarge: Pair<String, String>  // Large 데이터셋의 사용자

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
        repository = LoginRepositoryImpl(LoginTable)
        
        // 테이블 생성
        transaction {
            SchemaUtils.create(LoginTable)
        }
    }

    @Setup(Level.Iteration)
    fun setupSmallDataset() {
        transaction {
            // 기존 데이터 삭제
            SchemaUtils.drop(LoginTable)
            SchemaUtils.create(LoginTable)
            
            // Small 데이터셋 생성 (100개)
            val users = mutableListOf<LoginModel>()
            repeat(SMALL_DATASET_SIZE) { i ->
                users.add(createTestUser("small", i))
            }
            
            // 배치 삽입
            users.forEach { user ->
                runBlocking {
                    repository.create(user)
                }
            }
            
            // 테스트용 사용자 선택 (중간 정도 위치)
            testUserSmall = Pair("small_prefix_50", "small_suffix_50")
        }
    }

    @TearDown(Level.Trial)
    fun teardown() {
        transaction {
            SchemaUtils.drop(LoginTable)
        }
        dataSource.close()
    }

    // 테스트 사용자 생성 헬퍼 함수
    private fun createTestUser(prefix: String, index: Int): LoginModel {
        return LoginModel(
            id = UUID.randomUUID(),
            prefixId = "${prefix}_prefix_${index}",
            suffixId = "${prefix}_suffix_${index}",
            password = "hashed_password_${index}",
            rsaUid = UUID.randomUUID(),
            status = LoginStatus.ACTIVE,
            role = UserRole.USER,
            lastLoginAt = java.time.OffsetDateTime.now()
        )
    }

    /**
     * Small 데이터셋 (100개) - 사용자 조회
     */
    @Benchmark
    fun findUserInSmallDataset(): LoginModel? {
        return runBlocking {
            repository.findUser(testUserSmall.first, testUserSmall.second)
        }
    }

    /**
     * Small 데이터셋 - 존재하지 않는 사용자 조회 (Miss 케이스)
     */
    @Benchmark
    fun findNonExistentUserInSmallDataset(): LoginModel? {
        return runBlocking {
            repository.findUser("nonexistent_prefix", "nonexistent_suffix")
        }
    }

    /**
     * Small 데이터셋 - 전체 사용자 조회
     */
    @Benchmark
    fun findAllUsersInSmallDataset(): List<LoginModel> {
        return runBlocking {
            repository.findAll()
        }
    }

    /**
     * Small 데이터셋 - ID로 사용자 조회
     */
    @Benchmark
    fun findByIdInSmallDataset(): LoginModel? {
        return runBlocking {
            val user = repository.findUser(testUserSmall.first, testUserSmall.second)
            user?.id?.let { repository.read(it) }
        }
    }

    /**
     * Small 데이터셋 - 사용자 생성
     */
    @Benchmark
    fun createUserInSmallDataset(): LoginModel {
        return runBlocking {
            val newUser = createTestUser("new", System.nanoTime().toInt())
            repository.create(newUser)
        }
    }

    /**
     * Small 데이터셋 - 사용자 업데이트
     */
    @Benchmark
    fun updateUserInSmallDataset(): LoginModel {
        return runBlocking {
            val user = repository.findUser(testUserSmall.first, testUserSmall.second)
            val updated = user!!.copy(
                password = "new_hashed_password_${System.nanoTime()}",
                lastLoginAt = java.time.OffsetDateTime.now()
            )
            repository.update(updated)
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
