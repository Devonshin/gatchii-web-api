package com.gatchii.domain.jwt

import com.gatchii.common.repository.ExposedCrudRepository
import com.gatchii.domain.jwt.RefreshTokenTable.createdAt
import com.gatchii.domain.jwt.RefreshTokenTable.expireAt
import com.gatchii.domain.jwt.RefreshTokenTable.id
import com.gatchii.domain.jwt.RefreshTokenTable.isValid
import com.gatchii.domain.jwt.RefreshTokenTable.userUid
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.OffsetDateTime
import java.util.*

interface RefreshTokenRepository: ExposedCrudRepository<RefreshTokenTable, RefreshTokenModel, UUID> {

    override fun toBatchRow(): BatchInsertStatement.(RefreshTokenModel) -> Unit = throw NotImplementedError()

    override fun toRow(domain: RefreshTokenModel): RefreshTokenTable.(InsertStatement<EntityID<UUID>>) -> Unit = {
        if (domain.id != null) {
            it[id] = domain.id!!
        }
        it[userUid] = domain.userUid
        if(domain.expireAt != null) {
            it[expireAt] = domain.expireAt
        }
        it[isValid] = domain.isValid
        if(domain.deletedAt != null) {
            it[deletedAt] = domain.deletedAt
        }
    }

    override fun toDomain(row: ResultRow): RefreshTokenModel {
        return RefreshTokenModel(
            id = row[id].value,
            isValid = row[isValid],
            userUid = row[userUid],
            expireAt = row[expireAt],
            createdAt = row[createdAt],
            deletedAt = row.getOrNull(RefreshTokenTable.deletedAt)
        )
    }

    override fun updateRow(domain: RefreshTokenModel): RefreshTokenTable.(UpdateStatement) -> Unit = {
        it[isValid] = domain.isValid
        if(domain.expireAt != null) {
            it[expireAt] = domain.expireAt
        }
    }

    override suspend fun findAll(): List<RefreshTokenModel> = dbQuery {
        table.selectAll()
            .where { RefreshTokenTable.deletedAt.isNull() }
            .orderBy(id to SortOrder.DESC)
            .map { toDomain(it) }
    }

    override suspend fun delete(id: UUID?) = dbQuery {
        table.update(
            where = { RefreshTokenTable.id eq id }
        ) {
            it[RefreshTokenTable.deletedAt] = OffsetDateTime.now()
        }
        return@dbQuery
    }

}
