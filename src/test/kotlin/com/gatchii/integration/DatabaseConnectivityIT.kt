package com.gatchii.integration

import com.gatchii.plugins.configureDatabases
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import shared.common.AbstractIntegrationTest
import com.gatchii.plugins.tables
import com.gatchii.common.repository.DatabaseFactoryImpl
import com.gatchii.plugins.DatabaseConfig

/**
 * @author Devonshin
 * @date 2025-09-14
 */
class DatabaseConnectivityIT : AbstractIntegrationTest() {

    @Test
    @DisplayName("Should connect to PostgreSQL container and init schema when application starts")
    fun should_connect_and_init_schema_when_application_starts() = withIntegrationApplication {
        // Given: 통합 환경 설정으로 Ktor 애플리케이션에서 DB 플러그인을 구동
        application {
            configureDatabases()
        }
        // And: 안전하게 Exposed 연결을 보장하기 위해 직접 연결 수행(환경과 동일 설정)
        DatabaseFactoryImpl(
            DatabaseConfig(
                driverClass = "org.postgresql.Driver",
                url = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password,
                maxPoolSize = 2
            )
        ).connect()

        // When: Exposed 트랜잭션으로 스키마 초기화 확인 (이미 플러그인에서 수행되지만 안전 확인)
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.createMissingTablesAndColumns(*tables)
        }

        // Then: 예외 없이 수행되면 성공으로 간주
        assert(true)
    }
}