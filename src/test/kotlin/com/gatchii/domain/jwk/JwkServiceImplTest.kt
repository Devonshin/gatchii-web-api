package com.gatchii.domain.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.gatchii.common.model.ResultData
import com.gatchii.common.task.RoutineScheduleExpression
import com.gatchii.common.task.RoutineTaskHandler
import com.gatchii.common.task.TaskLeadHandler
import com.gatchii.utils.DateUtil
import com.gatchii.utils.ECKeyPairHandler
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPrivateKey
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPublicKey
import com.typesafe.config.ConfigFactory
import io.ktor.util.*
import io.ktor.util.logging.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*
import kotlin.test.BeforeTest

class JwkServiceImplTest {

    val logger: Logger = KtorSimpleLogger(this::class.simpleName ?: "JwkServiceImplTest")
    val jwkRepository = mockk<JwkRepository>()
    val taskName = "testJwkTask"
    lateinit var jwkService: JwkServiceImpl

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            JwkHandler.setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
        }
    }

    @BeforeTest
    fun beforeTestSetUp() {
        JwkHandler.clearAll()
        jwkService = JwkServiceImpl(jwkRepository) { task: () -> Unit ->
            RoutineTaskHandler(
                taskName = taskName,
                scheduleExpression = RoutineScheduleExpression(),
                task = task,
                period = 24 * 60 * 60L,
                CoroutineScope(Dispatchers.Default)
            )
        }
    }

    @Test
    fun `test findRandomJwk throws error when no usable jwks found`() = runTest {
        // given
        //coEvery { jwkRepository.getUsableOne(any()) } returns null
        //coEvery { jwkRepository.getAllUsable(any()) } returns ResultData<JwkModel>(emptyList(), 0, false)
        // when & then
        mockkObject(JwkHandler)
        coEvery { JwkHandler.getRandomActiveJwk() } returns Optional.empty()
        assertThrows<NoSuchElementException> {
            jwkService.getRandomJwk()
        }
        coVerify { JwkHandler.getRandomActiveJwk() }
    }

    @Test
    fun `test findRandomJwk returns jwk when usable jwks are found`() = runTest {
        // given
        val jwk = mockk<JwkModel>()
        JwkHandler.addJwk(jwk)
        // when
        val result = jwkService.getRandomJwk()
        // then
        assert(jwk == result)
    }

    @Test
    fun `test findJwk throws error when jwk not found`() = runTest {
        // given
        val id = UUID.randomUUID()
        coEvery { jwkRepository.read(id) } returns null

        // when & then
        assertThrows<NoSuchElementException> {
            jwkService.findJwk(id)
        }
    }

    @Test
    fun `test findJwk returns jwk when found`() = runTest {
        // given
        val id = UUID.randomUUID()
        val jwk = mockk<JwkModel>()
        coEvery { jwkRepository.read(id) } returns jwk
        // when
        val result = jwkService.findJwk(id)

        // then
        assert(jwk == result)
    }

    @Test
    fun `test convertAlgorithm creates Algorithm successfully`() = runTest {
        // given
        val provider = mockk<ECDSAKeyProvider>()
        // when
        val result = jwkService.convertAlgorithm(provider)
        // then
        assert(result is Algorithm)
    }

    @Test
    fun `test getProvider returns ECDSAKeyProvider`() = runTest {
        // given
        val jwkModel = JwkModel(
            privateKey = "mock-private-key",
            publicKey = "mock-public-key",
            createdAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        mockkObject(ECKeyPairHandler)

        coEvery { convertPrivateKey(any()) } returns mockk()
        coEvery { convertPublicKey(any()) } returns mockk()

        // when
        val provider = jwkService.getProvider(jwkModel)

        // then
        assert(jwkModel.id.toString() == provider.privateKeyId)
        unmockkObject(ECKeyPairHandler)
    }

    @Test
    fun `test findAllJwk returns empty set when no jwks present`() = runTest {
        // given
        coEvery { jwkRepository.findAll() } returns emptyList()
        // when
        val result = jwkService.findAllJwk()
        // then
        assert(result.isEmpty())
    }

    @Test
    fun `test findAllJwk filters out deleted jwks`() = runTest {
        // given
        val date = OffsetDateTime.now()
        val generateKeyPair = ECKeyPairHandler.generateKeyPair()

        val activated = JwkModel(
            privateKey = generateKeyPair.private.encoded.encodeBase64(),
            publicKey = generateKeyPair.public.encoded.encodeBase64(),
            createdAt = date,
        )
        val deleted = JwkModel(
            privateKey = generateKeyPair.private.encoded.encodeBase64(),
            publicKey = generateKeyPair.public.encoded.encodeBase64(),
            createdAt = date,
            deletedAt = date
        )

        coEvery { jwkRepository.findAll() } returns listOf(activated, deleted)
        // when
        val result = jwkService.findAllJwk()

        assert(result.size == 1)
    }

    @Test
    fun `test jwkSchedulingProc creates and adds a new jwk`() = runTest {
        // given
        mockkObject(JwkHandler)
        val createdJwk = mockk<JwkModel>()
        coEvery { jwkRepository.create(any()) } returns createdJwk
        coEvery { JwkHandler.addJwk(createdJwk) } returns Unit
        coEvery { JwkHandler.getRemovalJwks() } returns emptyList()

        // when
        jwkService.taskProcessing()

        // then
        coVerify(exactly = 1) {
            jwkRepository.create(any())
            JwkHandler.addJwk(createdJwk)
            JwkHandler.getRemovalJwks()
        }
        unmockkObject(JwkHandler)
    }

    @Test
    fun `test jwkSchedulingProc deletes removable jwks`() = runTest {
        // given
        mockkObject(JwkHandler)
        val createdJwk = mockk<JwkModel>()
        val removableJwk = mockk<JwkModel>(relaxed = true) {
            coEvery { id } returns UUID.randomUUID()
        }
        coEvery { jwkRepository.create(any()) } returns createdJwk
        coEvery { JwkHandler.getRemovalJwks() } returns listOf(removableJwk)
        coEvery { jwkRepository.delete(removableJwk.id!!) } returns Unit

        // when
        jwkService.taskProcessing()

        // then
        coVerify {
            jwkRepository.create(any())
            JwkHandler.getRemovalJwks()
            jwkRepository.delete(removableJwk.id!!)
        }
        unmockkObject(JwkHandler)
    }

    @Test
    fun `test initializeJwk adds usable jwks to JwkHandler`() = runTest {
        // given
        val usableJwk = mockk<JwkModel>()
        coEvery { jwkRepository.getAllUsable(null, true, any(), false) } returns ResultData(
            datas = listOf(usableJwk),
            hasMoreData = false
        )
        mockkObject(JwkHandler)
        mockkObject(TaskLeadHandler)
        coEvery { JwkHandler.addJwk(any()) } returns Unit
        coEvery { TaskLeadHandler.addTasks(any()) } returns Unit

        // when
        jwkService.initializeJwk()

        // then
        coVerify(exactly = 1) { JwkHandler.addJwk(usableJwk) }
        coVerify(exactly = 1) { jwkRepository.getAllUsable(null, true, any(), false) }
        coVerify(exactly = 1) { TaskLeadHandler.addTasks(any()) }
        unmockkObject(JwkHandler)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `test initializeJwk sets up JWK daily task`() = runTest {
        // given
        //val testDispatcher = StandardTestDispatcher(testScheduler)
        //val testScope = CoroutineScope(testDispatcher)
        val totalDays = 30
        DateUtil.initTestDate("testJwkServiceJob30DayTask")
        val taskHandlerProvider = { task: () -> Unit ->
            RoutineTaskHandler(
                taskName = taskName,
                scheduleExpression = RoutineScheduleExpression(3, 0, 0), //0h 0m 0s
                task = task,
                period = 24 * 60 * 60L,
                this
            )
        }
        mockkObject(DateUtil)
        val jwkService = JwkServiceImpl(jwkRepository, taskHandlerProvider)
        JwkHandler.setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
        val maxCapacity = JwkHandler.getConfigValue("maxCapacity")?.toInt() ?: 10
        val expireTimeSec = JwkHandler.getConfigValue("expireTimeSec")?.toInt()?.times(10) //10일
        val durationMills = totalDays * 24 * 60 * 60 * 1000L // 30일
        coEvery {
            jwkRepository.create(any())
        } answers {
            JwkModel(
                privateKey = "privateKey",
                publicKey = "publicKey",
                createdAt = OffsetDateTime.now(DateUtil.getTestDate("testJwkServiceJob30DayTask")),
                id = UUID.randomUUID()
            )
        }
        coEvery { DateUtil.getCurrentDate() } answers {
            OffsetDateTime.now(DateUtil.getTestDate("testJwkServiceJob30DayTask"))
        }
        coEvery { jwkRepository.delete(any<UUID>()) } returns Unit
        coEvery { jwkRepository.getAllUsable(null, true, any(), false) } returns ResultData(
            datas = listOf(),
            hasMoreData = false
        )
        // when
        jwkService.initializeJwk()
        TaskLeadHandler.runTasks()

        advanceTimeBy(durationMills)
        runCurrent()
        jwkService.stopTask()
        //testScope.cancel() // 스코프 전체 중단

        // then
        val jwks = JwkHandler.getJwks()
        val activeJwks = JwkHandler.getActiveJwks()
        val inactiveJwks = JwkHandler.getInactiveJwks()
        val discardJwks = JwkHandler.getDiscardJwks()

        assert(activeJwks.size + inactiveJwks.size + discardJwks.size == totalDays)
        assert(jwks.size == activeJwks.size + inactiveJwks.size)
        assert(inactiveJwks.size == jwks.size - activeJwks.size)
        assert(activeJwks.size == maxCapacity)
        assert(discardJwks.size == totalDays - maxCapacity - inactiveJwks.size)
        //assert(discardJwks.size == shouldRemoveJwks.size)
        //coVerify(exactly = 10) { jwkRepository.delete(any<UUID>()) }
        //coVerify(exactly = 30) { jwkRepository.create(any()) }
        unmockkObject(DateUtil)
    }
}

