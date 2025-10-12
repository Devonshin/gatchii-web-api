/**
 * @author Devonshin
 * @date 2025-01-19
 */
package com.gatchii.benchmark.concurrency

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
 * 로그인 동시성 및 부하 테스트 벤치마크
 * 
 * 목적:
 * - 여러 사용자의 동시 로그인 시나리오 시뮬레이션
 * - 동시 사용자 수에 따른 성능 변화 측정
 * - 데이터베이스 커넥션 풀 효율성 검증
 * - 응답 시간 분포 및 처리량 분석
 * 
 * 동시성 레벨:
 * - 10 threads: 낮은 부하
 * - 50 threads: 중간 부하
 * - 100 threads: 높은 부하
 * 
 * 실행 방법:
 * ./gradlew jmh --includes=".*LoginConcurrencyBenchmark.*"
 * 
 * 측정 지표:
 * - Throughput (ops/sec): 초당 처리 가능한 동시 로그인 수
 * - Average Time (ms/op): 로그인 평균 소요 시간
 * - 응답 시간 분포: P50, P95, P99
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class LoginConcurrencyBenchmark {

    companion object {
        const val DB_URL = "jdbc:h2:mem:concurrency_benchmark;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        const val USER_COUNT = 1000  // 테스트 사용자 수
    }

    private lateinit var dataSource: HikariDataSource
    private lateinit var repository: LoginRepository
    
    // 테스트용 사용자 목록 (prefix, suffix 쌍)
    private lateinit var testUsers: List<Pair<String, String>>

    @Setup(Level.Trial)
    fun setupDatabase() {
        // HikariCP 설정 - 동시성 테스트를 위해 풀 크기 증가
        val config = HikariConfig().apply {
            jdbcUrl = DB_URL
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 50  // 동시성 테스트를 위해 증가
            minimumIdle = 10
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
        
        // 테스트 사용자 데이터 생성
        transaction {
            val users = mutableListOf<Pair<String, String>>()
            repeat(USER_COUNT) { i ->
                val user = createTestUser("user", i)
                runBlocking {
                    repository.create(user)
                }
                users.add(Pair(user.prefixId, user.suffixId))
            }
            testUsers = users
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

    // 랜덤 사용자 선택
    @State(Scope.Thread)
    open class RandomUserState {
        private val random = Random()
        var userIndex: Int = 0
        
        @Setup(Level.Invocation)
        fun selectRandomUser() {
            userIndex = random.nextInt(USER_COUNT)
        }
    }

    /**
     * 10 스레드 동시 실행 - 낮은 부하
     */
    @Benchmark
    @Threads(10)
    fun concurrentLogin10Threads(state: RandomUserState): LoginModel? {
        val (prefix, suffix) = testUsers[state.userIndex]
        return runBlocking {
            repository.findUser(prefix, suffix)
        }
    }

    /**
     * 50 스레드 동시 실행 - 중간 부하
     */
    @Benchmark
    @Threads(50)
    fun concurrentLogin50Threads(state: RandomUserState): LoginModel? {
        val (prefix, suffix) = testUsers[state.userIndex]
        return runBlocking {
            repository.findUser(prefix, suffix)
        }
    }

    /**
     * 100 스레드 동시 실행 - 높은 부하
     */
    @Benchmark
    @Threads(100)
    fun concurrentLogin100Threads(state: RandomUserState): LoginModel? {
        val (prefix, suffix) = testUsers[state.userIndex]
        return runBlocking {
            repository.findUser(prefix, suffix)
        }
    }

    /**
     * 단일 스레드 - 기준선 (비교용)
     */
    @Benchmark
    @Threads(1)
    fun singleThreadLogin(state: RandomUserState): LoginModel? {
        val (prefix, suffix) = testUsers[state.userIndex]
        return runBlocking {
            repository.findUser(prefix, suffix)
        }
    }

    /**
     * 10 스레드 - 사용자 생성 동시성 테스트
     */
    @Benchmark
    @Threads(10)
    fun concurrentCreateUser10Threads(): LoginModel {
        return runBlocking {
            val newUser = createTestUser("concurrent", System.nanoTime().toInt())
            repository.create(newUser)
        }
    }

    /**
     * 10 스레드 - 사용자 업데이트 동시성 테스트
     */
    @Benchmark
    @Threads(10)
    fun concurrentUpdateUser10Threads(state: RandomUserState): LoginModel {
        return runBlocking {
            val (prefix, suffix) = testUsers[state.userIndex]
            val user = repository.findUser(prefix, suffix)
            val updated = user!!.copy(
                lastLoginAt = java.time.OffsetDateTime.now()
            )
            repository.update(updated)
        }
    }
}
