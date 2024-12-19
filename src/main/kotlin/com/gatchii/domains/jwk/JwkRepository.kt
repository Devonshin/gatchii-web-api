package com.gatchii.domains.jwk

import com.gatchii.domains.jwk.JwkTable.createdAt
import com.gatchii.domains.jwk.JwkTable.deletedAt
import com.gatchii.domains.jwk.JwkTable.id
import com.gatchii.domains.jwk.JwkTable.privateKey
import com.gatchii.domains.jwk.JwkTable.publicKey
import com.gatchii.shared.exception.NotSupportMethodException
import com.gatchii.shared.repository.ExposedCrudRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.util.*

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 16/09/2024 */

interface JwkRepository : ExposedCrudRepository<JwkTable, JwkModel, UUID> {

    fun getUsableOne(idx: Int): JwkModel?

    override fun toRow(domain: JwkModel): JwkTable.(InsertStatement<EntityID<UUID>>) -> Unit = {
        throw NotSupportMethodException("Jwk data can't be update.")
    }

    override fun toBatchRow(): BatchInsertStatement.(JwkModel) -> Unit = {
        throw NotSupportMethodException("Jwk data can't be update.")
    }

    override fun toDomain(row: ResultRow): JwkModel {
        return JwkModel(
            id = row[id].value,
            privateKey = row[privateKey],
            publicKey = row[publicKey],
            createdAt = row[createdAt],
            deletedAt = row[deletedAt]
        )
    }

    override fun updateRow(domain: JwkModel): JwkTable.(UpdateStatement) -> Unit = {
        throw NotSupportMethodException("Jwk data can't be update.")
    }
}