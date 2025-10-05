package com.gatchii.domain.rsa

import com.gatchii.common.exception.NotSupportMethodException
import com.gatchii.common.repository.ExposedCrudRepository
import com.gatchii.domain.rsa.RsaTable.createdAt
import com.gatchii.domain.rsa.RsaTable.deletedAt
import com.gatchii.domain.rsa.RsaTable.exponent
import com.gatchii.domain.rsa.RsaTable.id
import com.gatchii.domain.rsa.RsaTable.modulus
import com.gatchii.domain.rsa.RsaTable.privateKey
import com.gatchii.domain.rsa.RsaTable.publicKey
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 16/09/2024
 */

interface RsaRepository : ExposedCrudRepository<RsaTable, RsaModel, UUID> {

  override fun toRow(domain: RsaModel): RsaTable.(InsertStatement<EntityID<UUID>>) -> Unit = {
    if (domain.id != null) it[id] = domain.id!!
    it[publicKey] = domain.publicKey
    it[privateKey] = domain.privateKey
    it[exponent] = domain.exponent
    it[modulus] = domain.modulus
    if (domain.createdAt != null) {
      it[createdAt] = domain.createdAt
    }
    if (domain.deletedAt != null) {
      it[deletedAt] = domain.deletedAt
    }
    it[deletedAt] = domain.deletedAt?.let {
      domain.deletedAt
    }
  }

  override fun toDomain(row: ResultRow): RsaModel {
    return RsaModel(
      id = row[id].value,
      publicKey = row[publicKey],
      privateKey = row[privateKey],
      exponent = row[exponent],
      modulus = row[modulus],
      createdAt = row[createdAt],
      deletedAt = row.getOrNull(deletedAt)
    )
  }

  override fun updateRow(domain: RsaModel): RsaTable.(UpdateStatement) -> Unit {
    throw NotSupportMethodException("Rsa data can't be update.")
  }

  override fun toBatchRow(): BatchInsertStatement.(RsaModel) -> Unit = {
    if (it.id != null) this[id] = it.id!!
    this[publicKey] = it.publicKey
    this[privateKey] = it.privateKey
    this[exponent] = it.exponent
    this[modulus] = it.modulus
    if (it.createdAt != null) {
      this[createdAt] = it.createdAt
    }
    this[deletedAt] = it.deletedAt
  }

  override suspend fun delete(id: UUID?) = dbQuery {
    table.update(
      where = { RsaTable.id eq id }
    ) {
      it[deletedAt] = OffsetDateTime.now()
    }
    return@dbQuery
  }
}
