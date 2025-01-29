package com.gatchii.domain.jwt

import com.gatchii.domain.jwt.RefreshTokenTable.createdAt
import com.gatchii.domain.jwt.RefreshTokenTable.expireAt
import com.gatchii.domain.jwt.RefreshTokenTable.isValid
import com.gatchii.domain.jwt.RefreshTokenTable.userUid
import shared.repository.DatabaseFactoryForTest
import shared.repository.dummyRefreshTokenQueryList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 15/11/2024
 */

@UnitTest
class RefreshTokenRepositoryImplTest {

    private val refreshRepository: RefreshTokenRepositoryImpl = RefreshTokenRepositoryImpl(RefreshTokenTable)

    companion object {

        private val databaseFactory: DatabaseFactoryForTest = DatabaseFactoryForTest()

        @BeforeAll
        @JvmStatic
        fun init() {
            println("init..")
            databaseFactory.connect()
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.drop(RefreshTokenTable)
                SchemaUtils.create(RefreshTokenTable)
                SchemaUtils.createMissingTablesAndColumns(RefreshTokenTable)
                execInBatch(dummyRefreshTokenQueryList)
            }
        }

        @AfterAll
        @JvmStatic
        fun destroy() {
            databaseFactory.close()
        }

    }

    @Test
    fun toBatchRow() {
        assertThrows<NotImplementedError> {
            refreshRepository.toBatchRow()
        }
    }

    @Test
    fun toRow() {
        //given
        val refreshTokenModel = RefreshTokenModel(
            id = UUID.randomUUID(),
            isValid = true,
            userUid = UUID.randomUUID(),
            expireAt = OffsetDateTime.now().plusDays(1),
            createdAt = OffsetDateTime.now(),
        )

        val insertStatement = object : InsertStatement<EntityID<UUID>>(RefreshTokenTable) {
            val fields = mutableMapOf<Any, Any>()
            override fun <S> set(column: Column<S>, value: S) {
                if (value != null) {
                    fields[column] = value
                }
                super.set(column, value)
            }
        }
        //when
        refreshRepository.toRow(refreshTokenModel)(RefreshTokenTable, insertStatement)
        //then
        //assertThat(insertStatement.fields[RefreshTokenTable.id]).isEqualTo(refreshTokenModel.id)
        assertThat(insertStatement.fields[isValid]).isEqualTo(refreshTokenModel.isValid)
        assertThat(insertStatement.fields[expireAt]).isEqualTo(refreshTokenModel.expireAt)
        assertThat(insertStatement.fields[createdAt]).isNotEqualTo(refreshTokenModel.createdAt)
    }

    @Test
    fun toDomain() {
        //given
        val resultRow = ResultRow.createAndFillDefaults(RefreshTokenTable.columns)
        val now = OffsetDateTime.now()
        val expAt = now.plusDays(1)
        resultRow[isValid] = true
        resultRow[expireAt] = expAt
        resultRow[createdAt] = now
        resultRow[userUid] = "019335d8-52d6-7d40-b434-af29f956faec".let { UUID.fromString(it) }
        //when

        val toDomain = refreshRepository.toDomain(resultRow)
        //then

        assertThat(toDomain.isValid).isEqualTo(resultRow[isValid])
        assertThat(toDomain.expireAt).isEqualTo(resultRow[expireAt])
        assertThat(toDomain.createdAt).isEqualTo(resultRow[createdAt])

    }

    @Test
    fun updateRow() {

        //given
        val now = OffsetDateTime.now()
        val refreshTokenModel = RefreshTokenModel(
            id = UUID.randomUUID(),
            isValid = false,
            userUid = UUID.randomUUID(),
            expireAt = now.plusDays(1),
            createdAt = now
        )
        val updateStateMent = object : UpdateStatement(RefreshTokenTable, null) {
            val fields = mutableMapOf<Any, Any>()
            override fun <S> set(column: Column<S>, value: S) {
                if (value != null) {
                    fields[column] = value
                }
                super.set(column, value)
            }
        }
        //when
        refreshRepository.updateRow(refreshTokenModel)(RefreshTokenTable, updateStateMent)
        //then

        assertThat(refreshTokenModel.isValid).isEqualTo(updateStateMent.fields[isValid])
        assertThat(refreshTokenModel.expireAt).isEqualTo(updateStateMent.fields[expireAt])
        assertThat(refreshTokenModel.createdAt).isNotEqualTo(updateStateMent.fields[createdAt])

    }

    @Test
    fun `create test`() = runTest {
        //given
        val refreshTokenModel = RefreshTokenModel(
            expireAt = OffsetDateTime.now().plusDays(1),
            isValid = true,
            userUid = UUID.randomUUID(),
        )
        //when
        val created = refreshRepository.create(refreshTokenModel)
        //then
        assertThat(created.id).isNotNull
    }

    @Test
    fun batchCreate() = runTest {
        //given
        //when
        //then
        assertThrows<NotImplementedError> {
            refreshRepository.batchCreate(domains = listOf(), ignore = false, shouldReturnGeneratedValues = false)
        }
    }

    @Test
    fun `findAll test`() = runTest {
        //given
        //when
        val findAll = refreshRepository.findAll()
        //then
        assertThat(findAll).isNotNull
        assertThat(findAll.size).isEqualTo(2)
    }

    @Test
    fun `read test`() =runTest {
        //given
        val id = "019335d8-52d6-7d40-b434-af29f956faec".let { UUID.fromString(it) }
        //when
        val read = refreshRepository.read(id)
        //then
        assertThat(read).isNotNull
        assertThat(read?.id).isEqualTo(id)
        assertThat(read?.isValid).isEqualTo(true)
        assertThat(read?.expireAt).isEqualTo(OffsetDateTime.parse("2024-11-17T17:39:49.709344+01:00"))
        assertThat(read?.createdAt).isEqualTo(OffsetDateTime.parse("2024-11-16T17:39:49.718633+01:00"))
    }

    @Test
    fun `update test`() = runTest {
        //given
        val updateModel = RefreshTokenModel(
            id = "019335d8-52d6-7d40-b434-af29f956faec".let { UUID.fromString(it) },
            isValid = false,
            userUid = UUID.randomUUID(),
            expireAt = OffsetDateTime.now(),
        )
        val userUid = "019345d8-52d6-7d40-b434-af29f956faec".let{ UUID.fromString(it) }
        //when
        val update = refreshRepository.update(updateModel)
        val find = refreshRepository.read(updateModel.id!!)!!
        //then
        assertThat(find).isNotNull
        assertThat(find.id).isEqualTo(updateModel.id)
        assertThat(find.isValid).isEqualTo(updateModel.isValid)
        assertThat(find.userUid).isEqualTo(userUid)
        assertThat(find.expireAt).isEqualTo(updateModel.expireAt)

    }

    @Test
    fun `delete with id test`() = runTest {

        //given
        val id = "019335d8-52d6-7d40-b434-af29f956faec".let { UUID.fromString(it) }
        //when
        refreshRepository.delete(id)
        val find = refreshRepository.read(id)
        //then
        assertThat(find).isNull()
    }

    @Test
    fun `delete with domain test`() = runTest {
        //given
        val updateModel = RefreshTokenModel(
            id = "019335d9-52d6-7d40-b434-af29f956faec".let { UUID.fromString(it) },
            isValid = false,
            userUid = UUID.randomUUID(),
            expireAt = OffsetDateTime.now(),
        )
        //when
        refreshRepository.delete(updateModel)
        //then
        val find = refreshRepository.read(updateModel.id!!)
        assertThat(find).isNull()
    }
}