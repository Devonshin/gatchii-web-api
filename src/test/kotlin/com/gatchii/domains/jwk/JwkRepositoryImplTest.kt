package com.gatchii.domains.jwk

import com.gatchii.shared.repository.DatabaseFactoryForTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.BeforeTest

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 18/12/2024 */

class JwkRepositoryImplTest {

    private lateinit var jwkRepository: JwkRepositoryImpl

    companion object {
        private val databaseFactory: DatabaseFactoryForTest = DatabaseFactoryForTest()

        @BeforeAll
        @JvmStatic
        fun init() {
            println("init..")
            databaseFactory.connect()
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(JwkTable)
                SchemaUtils.createMissingTablesAndColumns(JwkTable)
                //execInBatch(dummyLoginQueryList)
            }
        }

        @AfterAll
        @JvmStatic
        fun destroy() {
            databaseFactory.close()
        }
    }

    @BeforeTest
    fun setUp() {
        jwkRepository = JwkRepositoryImpl(JwkTable)
    }

    @Test
    fun toRow() {
        //given
        val jwkModel = JwkModel(
            privateKey = "privateKey",
            publicKey = "publicKey",
            createdAt = OffsetDateTime.now(),
            deletedAt = OffsetDateTime.now(),
            id = UUID.randomUUID(),
        )
        val insertStatement = object: InsertStatement<EntityID<UUID>>(JwkTable) {
            val fields = mutableMapOf<Any, Any>()
            override fun <S> set(column: org.jetbrains.exposed.sql.Column<S>, value: S) {
                if (value != null) {
                    fields[column] = value
                }
                super.set(column, value)
            }
        }
        //when
        jwkRepository.toRow(jwkModel)(JwkTable, insertStatement)
        //then
        assertThat(insertStatement.fields[JwkTable.id]).isEqualTo(jwkModel.id)
        assertThat(insertStatement.fields[JwkTable.privateKey]).isEqualTo(jwkModel.privateKey)
        assertThat(insertStatement.fields[JwkTable.publicKey]).isEqualTo(jwkModel.publicKey)
        assertThat(insertStatement.fields[JwkTable.createdAt]).isEqualTo(jwkModel.createdAt)
        assertThat(insertStatement.fields[JwkTable.deletedAt]).isEqualTo(jwkModel.deletedAt)

    }

    @Test
    fun toBatchRow() {
    }

    @Test
    fun toDomain() {
    }

    @Test
    fun updateRow() {
    }

    @Test
    fun create() {
    }

    @Test
    fun batchCreate() {
    }

    @Test
    fun findAll() {
    }

    @Test
    fun read() {
    }

    @Test
    fun update() {
    }

    @Test
    fun delete() {
    }

    @Test
    fun testDelete() {
    }

    @Test
    fun getUsableOne() {
    }

    @Test
    fun getTable() {
    }

}