package com.gatchii.domains.login

import com.gatchii.domains.login.LoginTable.deletedAt
import com.gatchii.domains.login.LoginTable.id
import com.gatchii.domains.login.LoginTable.lastLoginAt
import com.gatchii.domains.login.LoginTable.prefixId
import com.gatchii.domains.login.LoginTable.password
import com.gatchii.domains.login.LoginTable.role
import com.gatchii.domains.login.LoginTable.status
import com.gatchii.domains.login.LoginTable.suffixId
import com.gatchii.shared.common.Constants.Companion.EMPTY_STR
import com.gatchii.shared.repository.ExposedCrudRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.login
 * Created: Devonshin
 * Date: 24/09/2024
 */

interface LoginRepository: ExposedCrudRepository<LoginTable, LoginModel, UUID> {

    override suspend fun batchCreate(domains: List<LoginModel>, ignore: Boolean, shouldReturnGeneratedValues: Boolean): List<LoginModel> {
        throw NotImplementedError()
    }

    override suspend fun findAll(): List<LoginModel> {
        throw NotImplementedError()
    }

    override suspend fun delete(domain: LoginModel) = delete(domain.id!!)

    override suspend fun delete(id: UUID) = dbQuery {
        table.update(
            where = { LoginTable.id eq id }
        ) {
            it[deletedAt] = OffsetDateTime.now()
            //it[prefixId] = EMPTY_STR
            it[suffixId] = EMPTY_STR
            it[password] = EMPTY_STR
            it[role] = UserRole.DELETED
            it[status] = LoginStatus.DELETED
        }
        return@dbQuery
    }

    override fun toDomain(row: ResultRow): LoginModel {
        return LoginModel(
            id = row[id].value,
            prefixId = row[prefixId],
            password = row[password],
            suffixId = row[suffixId],
            lastLoginAt = row[lastLoginAt],
            status = row[status],
            role = row[role],
            deletedAt = row[deletedAt]
        )
    }

    override fun updateRow(domain: LoginModel): LoginTable.(UpdateStatement) -> Unit = {
        //it[prefixId] = domain.prefixId
        //it[suffixId] = domain.suffixId
        it[password] = domain.password
        it[status] = domain.status
        it[role] = domain.role
        it[lastLoginAt] = domain.lastLoginAt
        it[deletedAt] = domain.deletedAt
    }

    override fun toRow(domain: LoginModel): LoginTable.(InsertStatement<EntityID<UUID>>) -> Unit = {
        if(domain.id != null) it[id] = domain.id!!
        it[prefixId] = domain.prefixId
        it[password] = domain.password
        it[suffixId] = domain.suffixId
        it[status] = domain.status
        it[role] = domain.role
        it[lastLoginAt] = domain.lastLoginAt
        it[deletedAt] = domain.deletedAt
    }

    override fun toBatchRow(): BatchInsertStatement.(LoginModel) -> Unit = {
        throw NotImplementedError()
    }

    suspend fun findUser(prefixId: String, suffixId: String): LoginModel?

}