package com.gatchii.domain.jwk

import com.gatchii.common.exception.NotSupportMethodException
import com.gatchii.common.model.ResultData
import com.gatchii.common.repository.ExposedCrudRepository
import com.gatchii.domain.jwk.JwkTable.createdAt
import com.gatchii.domain.jwk.JwkTable.deletedAt
import com.gatchii.domain.jwk.JwkTable.id
import com.gatchii.domain.jwk.JwkTable.privateKey
import com.gatchii.domain.jwk.JwkTable.publicKey
import com.gatchii.domain.jwk.JwkTable.status
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 16/09/2024 */

interface JwkRepository : ExposedCrudRepository<JwkTable, JwkModel, UUID> {

  fun getUsableOne(idx: Int = 0): JwkModel?

  fun getAllUsable(
    id: UUID?,
    forward: Boolean = true,
    limit: Int = 10,
    withDeleted: Boolean = false
  ): ResultData<JwkModel>

  override fun toRow(domain: JwkModel): JwkTable.(InsertStatement<EntityID<UUID>>) -> Unit = {
    if (domain.id != null) it[id] = domain.id!!
    it[publicKey] = domain.publicKey
    it[privateKey] = domain.privateKey
    it[status] = domain.status.name
    if (domain.createdAt != null) {
      it[createdAt] = domain.createdAt
    }
    if (domain.deletedAt != null) {
      it[deletedAt] = domain.deletedAt
    }
  }

  override fun toBatchRow(): BatchInsertStatement.(JwkModel) -> Unit = {
    if (it.id != null) this[id] = it.id!!
    this[publicKey] = it.publicKey
    this[privateKey] = it.privateKey
    this[status] = it.status.name
    if (it.createdAt != null) {
      this[createdAt] = it.createdAt
    }
    if (it.deletedAt != null) {
      this[deletedAt] = it.deletedAt
    }
  }

  override fun toDomain(row: ResultRow): JwkModel {
    return JwkModel(
      id = row[id].value,
      privateKey = row[privateKey],
      publicKey = row[publicKey],
      status = JwkStatus.fromValue(row[status]),
      createdAt = row.getOrNull(createdAt),
      deletedAt = row.getOrNull(deletedAt)
    )
  }

  override fun updateRow(domain: JwkModel): JwkTable.(UpdateStatement) -> Unit = {
    throw NotSupportMethodException("Jwk data can't be update.")
  }

  override suspend fun delete(id: UUID?) = dbQuery {
    table.update(
      where = { JwkTable.id eq id }
    ) {
      it[deletedAt] = OffsetDateTime.now()
    }
    return@dbQuery
  }

  override suspend fun findAll(): List<JwkModel> = dbQuery {
    table.selectAll()
      .where { deletedAt.isNull() }
      .orderBy(id to SortOrder.DESC)
      .map { toDomain(it) }
  }

}