package com.gatchii.domain.jwt

import com.gatchii.domain.jwt.RefreshTokenTable.createdAt
import com.gatchii.domain.jwt.RefreshTokenTable.expireAt
import com.gatchii.domain.jwt.RefreshTokenTable.id
import com.gatchii.domain.jwt.RefreshTokenTable.isValid
import com.gatchii.domain.jwt.RefreshTokenTable.userUid
import com.gatchii.shared.repository.ExposedCrudRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
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
    }

    override fun toDomain(row: ResultRow): RefreshTokenModel {
        return RefreshTokenModel(
            id = row[id].value,
            isValid = row[isValid],
            userUid = row[userUid],
            expireAt = row[expireAt],
            createdAt = row[createdAt],
        )
    }

    override fun updateRow(domain: RefreshTokenModel): RefreshTokenTable.(UpdateStatement) -> Unit = {
        it[isValid] = domain.isValid
        if(domain.expireAt != null) {
            it[expireAt] = domain.expireAt
        }
    }

}
