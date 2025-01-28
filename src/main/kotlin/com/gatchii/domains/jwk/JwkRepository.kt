package com.gatchii.domains.jwk

import com.gatchii.domains.jwk.JwkTable.createdAt
import com.gatchii.domains.jwk.JwkTable.deletedAt
import com.gatchii.domains.jwk.JwkTable.id
import com.gatchii.domains.jwk.JwkTable.privateKey
import com.gatchii.domains.jwk.JwkTable.publicKey
import com.gatchii.shared.exception.NotSupportMethodException
import com.gatchii.shared.model.ResultData
import com.gatchii.shared.repository.ExposedCrudRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 16/09/2024 */

interface JwkRepository : ExposedCrudRepository<JwkTable, JwkModel, UUID> {

    fun getUsableOne(idx: Int = 0): JwkModel?

    fun getAllUsable(id: UUID?, forward: Boolean = true, limit: Int = 10, withDeleted: Boolean = false): ResultData<JwkModel>

    override fun toRow(domain: JwkModel): JwkTable.(InsertStatement<EntityID<UUID>>) -> Unit = {
        if (domain.id != null) it[id] = domain.id!!
        it[publicKey] = domain.publicKey
        it[privateKey] = domain.privateKey
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

}