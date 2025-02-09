package com.gatchii.common.repository

import com.gatchii.plugins.DatabaseConfig

/**
 * Package: com.gatchii.shared.repository
 * Created: Devonshin
 * Date: 19/09/2024
 */

class TestDatabaseConfig {
    companion object {
        fun testDatabaseConfing(): DatabaseConfig {
            return DatabaseConfig(
                driverClass = "org.h2.Driver",
                url = "jdbc:h2:mem:gatchii-db;DATABASE_TO_UPPER=false;MODE=POSTGRESQL",
                user = "sa",
                password = "sa",
                maxPoolSize = 2
            )
        }
    }
}