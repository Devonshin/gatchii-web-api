package com.gatchii.domain.login

import com.gatchii.common.const.Constants.Companion.EMPTY_STR
import shared.repository.DatabaseFactoryForTest
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import shared.repository.dummyLoginQueryList
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.BeforeTest

/**
 * Package: com.gatchii.unit.domains.login
 * Created: Devonshin
 * Date: 24/09/2024
 */
@UnitTest
class LoginRepositoryImplUnitTest {

    private lateinit var loginRepository: LoginRepository
    companion object {

        private val databaseFactory: DatabaseFactoryForTest = DatabaseFactoryForTest()

        @BeforeAll
        @JvmStatic
        fun init() {
            println("init..")
            databaseFactory.connect()
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(LoginTable)
                SchemaUtils.createMissingTablesAndColumns(LoginTable)
                execInBatch(dummyLoginQueryList)
            }
        }

        @AfterAll
        @JvmStatic
        fun destroy() {
            databaseFactory.close()
        }
    }

    @BeforeTest
    fun setup() {
        loginRepository = LoginRepositoryImpl(LoginTable)
    }

    private val loginAt = OffsetDateTime.now()
    private val id = UuidCreator.getTimeOrderedEpoch()
    private val loginData = LoginModel(
        id = id,
        prefixId = "prefixId",
        suffixId = "gmail.com",
        password = "laudem",
        rsaUid = UUID.randomUUID(),
        status = LoginStatus.ACTIVE,
        role = UserRole.USER,
        lastLoginAt = loginAt,
        deletedAt = null,
    )

    @Test
    fun `login findUser test`() = runTest {
        //given
        val prefixId = "loginId"
        val suffixId = "laudem"
        //when
        val loginModel = loginRepository.findUser(prefixId, suffixId)
        //then
        assertThat(loginModel).isNotNull
        assertThat(loginModel?.prefixId).isEqualTo(prefixId)
        assertThat(loginModel?.status).isEqualTo(LoginStatus.ACTIVE)
        assertThat(loginModel?.role).isEqualTo(UserRole.USER)
    }

    @Test
    fun `login create test`() = runTest {
        //given
        //when
        val create = loginRepository.create(loginData)
        //then
        assertThat(create).isNotNull
        assertThat(create).isEqualTo(loginData)
    }

    @Test
    fun `login update test`() = runTest {
        //given
        loginRepository.create(loginData)
        val lastLoginAt = OffsetDateTime.now()
        val loginModel = LoginModel(
            id = id,
            prefixId = "prefixId",
            suffixId = "gmail.com",
            password = "laudem",
            rsaUid = UUID.randomUUID(),
            status = LoginStatus.ACTIVE,
            role = UserRole.USER,
            lastLoginAt = lastLoginAt,
            deletedAt = null,
        )
        //when
        loginRepository.update(loginModel)
        //then
        val updated = loginRepository.read(id)
        assertThat(loginModel).isEqualTo(updated)
    }

    @Test
    fun `login delete test`() = runTest {
        //given
        val deleteId = UuidCreator.fromString("01922d5e-9721-77f0-8093-55f799339495")
        //when
        loginRepository.delete(deleteId)
        //then
        val read = loginRepository.read(deleteId)
        assertThat(read?.deletedAt).isNotNull
        assertThat(read?.prefixId).isEqualTo("testI2")
        assertThat(read?.suffixId).isEqualTo("laudem")
        assertThat(read?.password).isEqualTo(EMPTY_STR)
        assertThat(read?.status).isEqualTo(LoginStatus.DELETED)
        assertThat(read?.role).isEqualTo(UserRole.DELETED)
    }

    @Test
    fun `login findAll should return only non-deleted users`() = runTest {
        // when
        val list = loginRepository.findAll()
        // then
        assertThat(list.all { it.deletedAt == null }).isTrue()
    }

    @Test
    fun `login batchCreate should throw an exception test`() = runTest {
        //given
        var isThrown = false
        //when
        try {
            loginRepository.batchCreate(domains = listOf(), ignore = false, shouldReturnGeneratedValues = false)
        } catch (e: NotImplementedError) {
            isThrown = true
        }
        //then
        assertThat(isThrown).isTrue()
    }

    @Test
    fun `read returns null for non-existent id`() = runTest {
        // when
        val read = loginRepository.read(UUID.randomUUID())
        // then
        assertThat(read).isNull()
    }

    @Test
    fun `create with too long prefixId should throw`() = runTest {
        // given
        val tooLongPrefix = "a".repeat(51) // LoginTable.prefixId length = 50
        val model = LoginModel(
            id = UuidCreator.getTimeOrderedEpoch(),
            prefixId = tooLongPrefix,
            suffixId = "gmail.com",
            password = "pw",
            rsaUid = UUID.randomUUID(),
            status = LoginStatus.ACTIVE,
            role = UserRole.USER,
            lastLoginAt = OffsetDateTime.now(),
            deletedAt = null
        )
        // then
        var thrown = false
        try {
            loginRepository.create(model)
        } catch (e: Exception) {
            thrown = true
        }
        assertThat(thrown).isTrue()
    }

    @Test
    fun `withTransaction rolls back when second create fails`() = runTest {
        // given
        val prefixOk = "okUser"
        val valid = LoginModel(
            id = UuidCreator.getTimeOrderedEpoch(),
            prefixId = prefixOk,
            suffixId = "gmail.com",
            password = "pw",
            rsaUid = UUID.randomUUID(),
            status = LoginStatus.ACTIVE,
            role = UserRole.USER,
            lastLoginAt = OffsetDateTime.now(),
            deletedAt = null
        )
        val invalid = valid.copy(
            id = UuidCreator.getTimeOrderedEpoch(),
            prefixId = "b".repeat(51) // too long -> should fail
        )
        // when: one transaction containing two creates where second fails
        var thrown = false
        try {
            com.gatchii.common.repository.RepositoryTransactionRunner.withTransaction {
                kotlinx.coroutines.runBlocking {
                    loginRepository.create(valid)
                    loginRepository.create(invalid)
                }
            }
        } catch (e: Exception) {
            thrown = true
        }
        assertThat(thrown).isTrue()
        // then: first insert should be rolled back
        val found = loginRepository.findUser(prefixOk, "gmail.com")
        assertThat(found).isNull()
    }

}
