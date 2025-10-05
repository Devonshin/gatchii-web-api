package com.gatchii.plugins

import com.gatchii.common.repository.DatabaseFactoryImpl
import com.gatchii.domain.jwk.JwkTable
import com.gatchii.domain.login.LoginTable
import com.gatchii.domain.rsa.RsaTable
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {

//    val ktorConfig = environment.config.config("ktor")
//    val env = ktorConfig.propertyOrNull("environment")?.getString()
  //    if (arrayOf("test").contains(env)) configureH2()
  val dbConfig = environment.config.config("database")
  val databaseFactory = DatabaseFactoryImpl(
    DatabaseConfig(
      driverClass = dbConfig.property("driver").getString(),
      url = dbConfig.property("url").getString(),
      user = dbConfig.property("username").getString(),
      password = dbConfig.property("password").getString(),
      maxPoolSize = dbConfig.property("maxPoolSize").getString().toInt()
    )
  )
  databaseFactory.connect()
  flywayMigrate(
    url = dbConfig.property("url").getString(),
    user = dbConfig.property("username").getString(),
    password = dbConfig.property("password").getString()
  )
  println("Database connected & migrations applied")
}

//fun Application.configureH2() {
//    val h2Server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092")
//    environment.monitor.subscribe(ApplicationStarted) { application ->
//        h2Server.start()
//        application.environment.log.info("H2 server started. ${h2Server.url}")
//    }
//    environment.monitor.subscribe(ApplicationStopped) { application ->
//        h2Server.stop()
//        application.environment.log.info("H2 server stopped. ${h2Server.url}")
//    }
//}

private fun initData() {
  transaction {
    addLogger(StdOutSqlLogger)
    // For legacy/dev fallback only: keep schema creation if needed
    SchemaUtils.create(*tables)
    SchemaUtils.createMissingTablesAndColumns(*tables)
  }
}

private fun flywayMigrate(url: String, user: String, password: String) {
  val flyway = Flyway.configure()
    .dataSource(url, user, password)
    .locations("classpath:db/migration")
    .baselineOnMigrate(true)
    .load()
  flyway.migrate()
}

val tables = arrayOf(
  LoginTable,
  RsaTable,
  JwkTable,
  com.gatchii.domain.jwt.RefreshTokenTable
)

data class DatabaseConfig(
  val driverClass: String,
  val url: String,
  val user: String,
  val password: String,
  val maxPoolSize: Int
)

