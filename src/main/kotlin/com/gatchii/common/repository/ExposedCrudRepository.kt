package com.gatchii.common.repository

import com.gatchii.common.model.BaseModel
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Package: com.gatchii.shared.repository
 * Created: Devonshin
 * Date: 16/09/2024
 */

interface ExposedCrudRepository<
    TABLE : IdTable<T>,
    DOMAIN : BaseModel<T>,
    T : Comparable<T>
    > : CrudRepository<DOMAIN, T> {
  val table: TABLE

  override suspend fun create(domain: DOMAIN): DOMAIN = dbQuery {
    val id = table.insertAndGetId(toRow(domain))
    domain.id = id.value
    domain
  }

  override suspend fun batchCreate(
    domains: List<DOMAIN>,
    ignore: Boolean,
    shouldReturnGeneratedValues: Boolean
  ): List<DOMAIN> = dbQuery {
    val batchInsert =
      table.batchInsert(
        domains,
        ignore = ignore,
        shouldReturnGeneratedValues = shouldReturnGeneratedValues,
        toBatchRow()
      )
    List(batchInsert.size) {
      toDomain(batchInsert[it])
    }
  }

  override suspend fun findAll(): List<DOMAIN> = dbQuery {
    table.selectAll()
      .orderBy(
        table.id to SortOrder.DESC
      ).map { toDomain(it) }
  }

  override suspend fun read(id: T): DOMAIN? = dbQuery {
    table.selectAll()
      .where { table.id eq id }
      .limit(1)
      .map { toDomain(it) }
      .singleOrNull()
  }

  override suspend fun update(domain: DOMAIN): DOMAIN = dbQuery {
    table.update(
      where = { table.id eq domain.id },
      body = updateRow(domain)
    )
    domain
  }

  private fun delete(deleteCondition: () -> Op<Boolean>) = dbQuery {
    table.deleteWhere { deleteCondition() }
    return@dbQuery
  }

  override suspend fun delete(domain: DOMAIN) = delete(domain.id)

  override suspend fun delete(id: T?) = delete { table.id eq id }

  fun <T> dbQuery(block: () -> T): T = transaction {
    addLogger(StdOutSqlLogger)
    block()
  }

  fun toBatchRow(): BatchInsertStatement.(DOMAIN) -> Unit
  fun toRow(domain: DOMAIN): TABLE.(InsertStatement<EntityID<T>>) -> Unit
  fun toDomain(row: ResultRow): DOMAIN
  fun updateRow(domain: DOMAIN): TABLE.(UpdateStatement) -> Unit

}