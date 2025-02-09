package shared.repository

import com.gatchii.plugins.DatabaseConfig
import com.gatchii.common.repository.DatabaseFactory
import com.gatchii.common.repository.TestDatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.util.IsolationLevel
import org.jetbrains.exposed.sql.Database

/**
 * Package: com.gatchii.shared.repository
 * Created: Devonshin
 * Date: 17/09/2024
 */

class DatabaseFactoryForTest(
    databaseConfig: DatabaseConfig = TestDatabaseConfig.Companion.testDatabaseConfing()
) : DatabaseFactory {

    private val config: DatabaseConfig = databaseConfig
    private lateinit var database: HikariDataSource

    override fun dataSource(): HikariDataSource {
        val hConfig = HikariConfig()
        hConfig.jdbcUrl = config.url
        hConfig.username = config.user
        hConfig.password = config.password
        hConfig.setDriverClassName(config.driverClass)
        hConfig.maximumPoolSize = config.maxPoolSize
        hConfig.isAutoCommit = true
        hConfig.transactionIsolation = IsolationLevel.TRANSACTION_REPEATABLE_READ.name
        hConfig.validate()
        database = HikariDataSource(hConfig)
        return database
    }

    override fun connect() {
        Database.connect(dataSource())
    }

    override fun close() {
        database.close()
    }
}