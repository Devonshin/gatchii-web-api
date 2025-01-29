package com.gatchii.domain.rsa;

import com.gatchii.shared.exception.NotSupportMethodException
import shared.repository.DatabaseFactoryForTest
import shared.repository.dummyRsaQueryList
import com.gatchii.utils.RsaPairHandler
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.collections.set
import kotlin.test.assertNotNull

/**
 * Package: com.gatchii.domains.rsa
 * Created: Devonshin
 * Date: 11/11/2024
 */

@UnitTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RsaRepositoryImplTest {

    private val rsaRepository: RsaRepositoryImpl = RsaRepositoryImpl(RsaTable)

    companion object {
        private var totalRsaCount = 0
        private val databaseFactory: DatabaseFactoryForTest = DatabaseFactoryForTest()

        @BeforeAll
        @JvmStatic
        fun init() {
            println("init..")
            databaseFactory.connect()
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.drop(RsaTable)
                SchemaUtils.create(RsaTable)
                SchemaUtils.createMissingTablesAndColumns(RsaTable)
                execInBatch(dummyRsaQueryList)
                totalRsaCount = 5
            }
        }

        @AfterAll
        @JvmStatic
        fun destroy() {
            databaseFactory.close()
        }
    }


    @Test
    fun `toRow should populate InsertStatement with correct values from RsaModel`() {
        val rsaModel = rsaModel()
        val insertStatement = object : InsertStatement<EntityID<UUID>>(RsaTable) {
            val fields = mutableMapOf<Any, Any>()

            // id는 오버라이드 불가..
            override fun <S> set(column: Column<S>, value: S) {
                if (value != null) {
                    fields[column] = value
                }
                super.set(column, value)
            }
        }

        rsaRepository.toRow(rsaModel)(RsaTable, insertStatement)

        //assertEquals(rsaModel.id, insertStatement.fields[RsaTable.id]) // id는 오버라이드 불가..
        assertEquals(rsaModel.publicKey, insertStatement.fields[RsaTable.publicKey])
        assertEquals(rsaModel.privateKey, insertStatement.fields[RsaTable.privateKey])
        assertEquals(rsaModel.exponent, insertStatement.fields[RsaTable.exponent])
        assertEquals(rsaModel.modulus, insertStatement.fields[RsaTable.modulus])
        assertEquals(rsaModel.createdAt, insertStatement.fields[RsaTable.createdAt])
        assertNull(insertStatement.fields[RsaTable.deletedAt])
    }

    @Test
    fun `toDomain should convert ResultRow to RsaModel`() {
        // Given
        val id: EntityID<UUID> = EntityID(UUID.randomUUID(), RsaTable)
        val row = ResultRow.createAndFillDefaults(RsaTable.columns)
        val now = OffsetDateTime.now()
        row[RsaTable.id] = id
        row[RsaTable.publicKey] = "publicKey"
        row[RsaTable.privateKey] = "privateKey"
        row[RsaTable.exponent] = "exponent"
        row[RsaTable.modulus] = "modulus"
        row[RsaTable.createdAt] = now
        row[RsaTable.deletedAt] = null

        // When
        val result = rsaRepository.toDomain(row)

        // Then
        assertEquals(
            RsaModel(
                id = id.value,
                publicKey = "publicKey",
                privateKey = "privateKey",
                exponent = "exponent",
                modulus = "modulus",
                createdAt = now,
                deletedAt = null
            ), result
        )
    }

    @Test
    fun updateRow() = runTest {
        //	given
        val rsaModel = rsaModel()
        //when
        //then
        assertThrows<NotSupportMethodException> {
            rsaRepository.updateRow(rsaModel)
        }
    }

    @Test
    fun `Should correctly transform RsaModel to BatchInsertStatement when toBatchRow is invoked`() {
        //given
        val batchInsertStatement = object : BatchInsertStatement(RsaTable) {
            val fields = mutableMapOf<Any, Any>()
            override fun <S> set(column: Column<S>, value: S) {
                if (value != null) {
                    fields[column] = value
                }
                super.set(column, value)
            }
        }

        val rsaModel = RsaModel(
            publicKey = "publicKey",
            privateKey = "privateKey",
            exponent = "exponent",
            modulus = "modulus",
            createdAt = OffsetDateTime.now(),
            deletedAt = OffsetDateTime.now(),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        )
        //when
        rsaRepository.toBatchRow()(batchInsertStatement, rsaModel)
        //then
        assertEquals(rsaModel.publicKey, batchInsertStatement.fields[RsaTable.publicKey])
        assertEquals(rsaModel.privateKey, batchInsertStatement.fields[RsaTable.privateKey])
        assertEquals(rsaModel.exponent, batchInsertStatement.fields[RsaTable.exponent])
        assertEquals(rsaModel.modulus, batchInsertStatement.fields[RsaTable.modulus])
        assertEquals(rsaModel.createdAt, batchInsertStatement.fields[RsaTable.createdAt])
        assertEquals(rsaModel.deletedAt, batchInsertStatement.fields[RsaTable.deletedAt])
    }

    @Test
    fun `create test`() = runTest {
        //given
        val rsaKeyPair = RsaPairHandler.generateRsaDataPair()
        val privateKey = rsaKeyPair.privateKey
        val publicKey = rsaKeyPair.publicKey
        val now = OffsetDateTime.now()
        val randomUUID = UUID.randomUUID()
        val rsaModel = realRsaModel(randomUUID)
        //when
        val created = rsaRepository.create(rsaModel)
        //then
        assertEquals(rsaModel.privateKey, created.privateKey)
        assertEquals(rsaModel.publicKey, created.publicKey)
        assertEquals(rsaModel.exponent, created.exponent)
        assertEquals(rsaModel.modulus, created.modulus)
        assertEquals(rsaModel.createdAt, created.createdAt)
        assertEquals(rsaModel.id, created.id)
        totalRsaCount++
    }

    @Test
    fun `create test without id`() = runTest {
        //given
        val rsaModel = realRsaModel(null)
        //when
        val created = rsaRepository.create(rsaModel)
        //then
        assertEquals(rsaModel.privateKey, created.privateKey)
        assertEquals(rsaModel.publicKey, created.publicKey)
        assertEquals(rsaModel.exponent, created.exponent)
        assertEquals(rsaModel.modulus, created.modulus)
        assertEquals(rsaModel.createdAt, created.createdAt)
        assertNotNull(created.id)
        totalRsaCount++
    }

    @Test
    fun batchCreate() = runTest {
        //given
        val rsaModels = List(5) {
            realRsaModel(null)
        }
        //when
        val batchCreateds = rsaRepository.batchCreate(rsaModels)
        //then
        assertEquals(rsaModels.size, batchCreateds.size)
        val count = batchCreateds.filter {
            it.id != null
        }.count()
        assertEquals(rsaModels.size, count)
        repeat(rsaModels.size) {
            val rsa = rsaModels[it]
            val batchRsa = batchCreateds[it]
            assertEquals(rsa.privateKey, batchRsa.privateKey)
            assertEquals(rsa.publicKey, batchRsa.publicKey)
            assertEquals(rsa.exponent, batchRsa.exponent)
            assertEquals(rsa.modulus, batchRsa.modulus)
            assertEquals(rsa.createdAt, batchRsa.createdAt)
            totalRsaCount++
        }
    }

    @Test
    @Order(Int.MAX_VALUE)
    fun `findAll test`() = runTest{
        //given
        //when
        val findAll = rsaRepository.findAll()
        //then
        assertEquals(totalRsaCount, findAll.size)
    }

    @Test
    @Order(Int.MAX_VALUE)
    fun `read test`() = runTest {

        //given
        val uuid = UUID.fromString("01922d5e-9721-77f0-8093-55f799339491")
        //when
        val read = rsaRepository.read(uuid)
        //then
        assertNotNull(read)
        assertEquals(read.privateKey, "privatekey")
        assertEquals(read.publicKey, "publicKey")
        assertEquals(read.exponent, "exponent")
        assertEquals(read.modulus, "modulus")
    }

    @Test
    @Order(Int.MAX_VALUE)
    fun `update test should throw NotSupportMethodException`() = runTest{
        //given
        //when
        //then
        assertThrows<NotSupportMethodException>() {
            rsaRepository.update(realRsaModel(UUID.fromString("01922d5e-9721-77f0-8093-55f799339491")))
        }
    }

    @Test
    @Order(Int.MAX_VALUE - 100)
    fun `delete test`() = runTest {
        //given
        val uuid = UUID.fromString("01922d5e-9721-77f0-8093-55f799339492")
        //when
        rsaRepository.delete(uuid)
        totalRsaCount--
        //then
        val read = rsaRepository.read(uuid)
        assertThat(read).isNull()
    }

    private fun rsaModel(now: OffsetDateTime = OffsetDateTime.now()): RsaModel {
        val rsaModel = RsaModel(
            publicKey = "publicKey",
            privateKey = "privateKey",
            exponent = "exponent",
            modulus = "modulus",
            createdAt = now,
            id = UUID.randomUUID()
        )
        return rsaModel
    }

    private fun realRsaModel(randomUUID: UUID?): RsaModel {
        val rsaKeyPair = RsaPairHandler.generateRsaDataPair()
        val privateKey = rsaKeyPair.privateKey
        val publicKey = rsaKeyPair.publicKey
        val now = OffsetDateTime.now()
        val rsaModel = RsaModel(
            publicKey = publicKey.publicKey,
            privateKey = privateKey.privateKey,
            exponent = publicKey.e,
            modulus = publicKey.n,
            createdAt = now,
            id = randomUUID
        )
        return rsaModel
    }

}

//Generated with love by TestMe :) Please raise issues & feature requests at: https://weirddev.com/forum#!/testme