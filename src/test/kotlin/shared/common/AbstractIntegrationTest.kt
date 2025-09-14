package shared.common

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.junit.jupiter.TestcontainersExtension
import shared.postgres.PostgresTestContainer

/**
 * @author Devonshin
 * @date 2025-09-14
 */
@IntegrationTest
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(TestcontainersExtension::class)
abstract class AbstractIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgresTestContainer.container
    }

    @BeforeAll
    fun setUpAll() {
        if (!postgres.isRunning) {
            postgres.start()
        }
    }

    @AfterAll
    fun tearDownAll() {
        // @Container 에 의해 자동 정리되므로 별도 처리 불필요
    }

    protected fun integrationConfig(): Config {
        val overrides = mapOf(
            "database.url" to postgres.jdbcUrl,
            "database.username" to postgres.username,
            "database.password" to postgres.password,
            "database.driver" to "org.postgresql.Driver",
            // 테스트에서는 커넥션 수를 낮게 유지
            "database.maxPoolSize" to 2
        )
        return ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load("application-test.conf"))
    }

    protected fun withIntegrationApplication(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        // Ktor 테스트 엔진에 통합 테스트용 설정 주입
        environment { config = HoconApplicationConfig(integrationConfig()) }
        block()
    }
}