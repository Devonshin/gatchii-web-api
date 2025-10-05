package com.gatchii.common.repository

import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Repository-level transaction boundary helper.
 * 여러 Repository 작업을 하나의 트랜잭션으로 묶어 원자성을 보장하기 위한 유틸입니다.
 */
object RepositoryTransactionRunner {
  fun <T> withTransaction(block: () -> T): T = transaction {
    addLogger(StdOutSqlLogger)
    block()
  }
}
