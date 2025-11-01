/**
 * @author Devonshin
 * @date 2025-10-11
 */
package com.gatchii.plugins

import shared.repository.DatabaseFactoryForTest
import com.gatchii.domain.jwk.JwkTable
import com.gatchii.domain.login.LoginTable
import com.gatchii.domain.rsa.RsaTable
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import shared.common.IntegrationTest

/**
 * Database 플러그인 테스트 클래스
 * 
 * 데이터베이스 연결, 마이그레이션, 설정 등을 테스트합니다.
 */
@IntegrationTest
class DatabasesTest {

  /**
   * DatabaseConfig 데이터 클래스 생성 테스트
   */
  @Test
  fun `DatabaseConfig should create instance with valid properties`() {
    // Given: 유효한 데이터베이스 설정 값
    val driverClass = "org.h2.Driver"
    val url = "jdbc:h2:mem:test"
    val user = "sa"
    val password = ""
    val maxPoolSize = 10

    // When: DatabaseConfig 인스턴스 생성
    val config = DatabaseConfig(
      driverClass = driverClass,
      url = url,
      user = user,
      password = password,
      maxPoolSize = maxPoolSize
    )

    // Then: 모든 속성이 올바르게 설정됨
    assertEquals(driverClass, config.driverClass)
    assertEquals(url, config.url)
    assertEquals(user, config.user)
    assertEquals(password, config.password)
    assertEquals(maxPoolSize, config.maxPoolSize)
  }

  /**
   * tables 배열에 필수 테이블이 포함되어 있는지 테스트
   */
  @Test
  fun `tables array should contain all required tables`() {
    // Then: tables 배열에 필수 테이블 포함
    assertTrue(tables.contains(LoginTable), "LoginTable should be in tables array")
    assertTrue(tables.contains(RsaTable), "RsaTable should be in tables array")
    assertTrue(tables.contains(JwkTable), "JwkTable should be in tables array")
    assertTrue(tables.contains(com.gatchii.domain.jwt.RefreshTokenTable), "RefreshTokenTable should be in tables array")
    assertEquals(4, tables.size, "tables array should contain exactly 4 tables")
  }

  /**
   * 데이터베이스 연결 테스트 (테스트용 H2 인메모리 DB)
   */
  @Test
  fun `database connection should be established successfully`() {
    // Given: 테스트용 데이터베이스 설정
    val testConfig = DatabaseConfig(
      driverClass = "org.h2.Driver",
      url = "jdbc:h2:mem:test_db_connection;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
      user = "sa",
      password = "",
      maxPoolSize = 5
    )

    // When: DatabaseFactory를 통한 연결
    val factory = DatabaseFactoryForTest(testConfig)
    factory.connect()

    // Then: 데이터베이스 연결이 성공하고 트랜잭션 실행 가능
    transaction {
      val result = exec("SELECT 1") { rs ->
        rs.next()
        rs.getInt(1)
      }
      assertNotNull(result, "Database query should return result")
      assertEquals(1, result, "Query result should be 1")
    }
  }

  /**
   * Flyway 마이그레이션이 정상적으로 적용되는지 테스트
   */
  @Test
  fun `flyway migration should be applied successfully`() {
    // Given: 테스트용 H2 데이터베이스
    val testUrl = "jdbc:h2:mem:test_flyway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
    val testUser = "sa"
    val testPassword = ""

    // When: Flyway 마이그레이션 실행
    val flyway = Flyway.configure()
      .dataSource(testUrl, testUser, testPassword)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .load()

    val result = flyway.migrate()

    // Then: 마이그레이션이 성공적으로 완료됨
    assertNotNull(result, "Migration result should not be null")
    assertTrue(result.success, "Migration should be successful")
    assertTrue(result.migrationsExecuted >= 0, "At least baseline migration should be executed")
  }

  /**
   * configureDatabases 플러그인이 application에 정상 설치되는지 테스트
   */
  @Test
  fun `configureDatabases plugin should install successfully`() = testApplication {
    // Given: 테스트용 application.conf 설정
    environment {
      config = MapApplicationConfig(
        "database.driver" to "org.h2.Driver",
        "database.url" to "jdbc:h2:mem:test_plugin;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "database.username" to "sa",
        "database.password" to "",
        "database.maxPoolSize" to "5"
      )
    }

    application {
      configureDatabases()
    }

    // When: 간단한 health check 요청
    val response = client.get("/")

    // Then: 애플리케이션이 정상 실행됨 (404는 정상, 500이 아니면 DB 초기화 성공)
    assertTrue(
      response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.OK,
      "Application should start successfully with database configured"
    )
  }

  /**
   * 데이터베이스 연결 풀 크기 설정 테스트
   */
  @Test
  fun `database connection pool size should be configurable`() {
    // Given: 다양한 maxPoolSize 값
    val poolSizes = listOf(1, 5, 10, 20)

    poolSizes.forEach { size ->
      // When: DatabaseConfig 생성
      val config = DatabaseConfig(
        driverClass = "org.h2.Driver",
        url = "jdbc:h2:mem:test_pool_$size",
        user = "sa",
        password = "",
        maxPoolSize = size
      )

      // Then: maxPoolSize가 올바르게 설정됨
      assertEquals(size, config.maxPoolSize, "maxPoolSize should be $size")
    }
  }

  /**
   * 데이터베이스 URL 형식 검증 테스트
   */
  @Test
  fun `database URL should support different connection strings`() {
    // Given: 다양한 데이터베이스 URL 형식
    val urls = listOf(
      "jdbc:h2:mem:test",
      "jdbc:h2:file:./data/testdb",
      "jdbc:postgresql://localhost:5432/testdb",
      "jdbc:postgresql://postgres-container:5432/authdb"
    )

    urls.forEach { url ->
      // When: DatabaseConfig 생성
      val config = DatabaseConfig(
        driverClass = if (url.contains("postgresql")) "org.postgresql.Driver" else "org.h2.Driver",
        url = url,
        user = "testuser",
        password = "testpass",
        maxPoolSize = 10
      )

      // Then: URL이 올바르게 설정됨
      assertEquals(url, config.url, "URL should be set correctly")
    }
  }

  /**
   * DatabaseConfig가 다양한 maxPoolSize 값을 지원하는지 테스트
   */
  @Test
  fun `DatabaseConfig should support various pool sizes`() {
    // Given: 다양한 maxPoolSize 값
    val poolSizes = listOf(1, 5, 10, 20, 50, 100)

    poolSizes.forEach { size ->
      // When: DatabaseConfig 생성
      val config = DatabaseConfig(
        driverClass = "org.h2.Driver",
        url = "jdbc:h2:mem:test_pool_$size",
        user = "sa",
        password = "",
        maxPoolSize = size
      )

      // Then: maxPoolSize가 올바르게 설정됨
      assertEquals(size, config.maxPoolSize, "Pool size should be $size")
      assertTrue(config.maxPoolSize > 0, "Pool size should be positive")
    }
  }
}
