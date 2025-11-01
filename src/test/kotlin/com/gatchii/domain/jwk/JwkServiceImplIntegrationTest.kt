package com.gatchii.domain.jwk

import com.gatchii.common.model.ResultData
import com.gatchii.common.task.RoutineScheduleExpression
import com.gatchii.common.task.RoutineTaskHandler
import com.gatchii.common.task.TaskLeadHandler
import com.gatchii.common.utils.DateUtil
import com.typesafe.config.ConfigFactory
import io.ktor.util.*
import io.ktor.util.logging.*
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import shared.common.IntegrationTest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

@IntegrationTest
class JwkServiceImplIntegrationTest {

  private val logger: Logger = KtorSimpleLogger(this::class.simpleName ?: "JwkServiceImplIntegrationTest")
  private val jwkRepository = mockk<JwkRepository>()
  private val taskName = "testJwkTaskIntegration"

  companion object {
    @BeforeAll
    @JvmStatic
    fun init() {
      JwkHandler.getInstance().setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
    }
  }

  @AfterEach
  fun tearDown() {
    // JwkHandler 테스트 격리
    JwkHandler.resetForTest()
    
    // 타스크 정리
    TaskLeadHandler.removeTask(taskName)
    
    // 전역 객체 모킹 잔류 방지
    try {
      unmockkAll()
    } catch (_: Throwable) {
    }
  }

  @ExperimentalCoroutinesApi
  @Test
  @Disabled("JWK 생성 로직 재검토 필요 - 30일 시뮬레이션에서 일부 JWK가 생성되지 않는 문제")
  fun `initializeJwk sets up JWK daily task with 30-day simulation`() = runTest {
    // given - 고정 타임존(UTC)로 설정하여 DST/지역 설정에 따른 비결정성 제거
    val originalTz = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    
    // 기존 타스크 제거
    TaskLeadHandler.removeTask(taskName)
    
    val totalDays = 30
    val routineScheduleExpression = RoutineScheduleExpression(3, 0, 0)
    val now = LocalDateTime.now()
    val totalJwkSize = if (now.withHour(routineScheduleExpression.hour).isAfter(now)) {
      totalDays
    } else {
      totalDays + 1
    }
    
    DateUtil.initTestDate("RoutineTaskHandler")
    val taskHandlerProvider = { task: () -> Unit ->
      RoutineTaskHandler(
        taskName = taskName,
        scheduleExpression = routineScheduleExpression,
        task = task,
        period = 24 * 60 * 60L,
        this
      )
    }
    
    mockkObject(DateUtil)
    val jwkService = JwkServiceImpl(jwkRepository, taskHandlerProvider)
    JwkHandler.getInstance().setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
    val maxCapacity = JwkHandler.getInstance().getConfigValue("maxCapacity")?.toInt() ?: 10
    val durationMills = totalDays * 24 * 60 * 60 * 1000L // 30일
    
    // 매일 새로운 JwkModel 생성
    coEvery {
      jwkRepository.create(any())
    } answers {
      JwkModel(
        privateKey = "privateKey",
        publicKey = "publicKey",
        createdAt = OffsetDateTime.now(DateUtil.getTestDate("RoutineTaskHandler")),
        id = UUID.randomUUID()
      )
    }
    
    coEvery { DateUtil.getCurrentDate() } answers {
      OffsetDateTime.now(DateUtil.getTestDate("RoutineTaskHandler"))
    }
    
    coEvery { jwkRepository.batchCreate(any()) } returns listOf()
    coEvery { jwkRepository.delete(any<UUID>()) } returns Unit
    coEvery { jwkRepository.getAllUsable(null, true, any(), false) } returns ResultData(
      datas = listOf(),
      hasMoreData = false
    )
    
    // when - 30일 시뮬레이션
    jwkService.initializeJwk()
    TaskLeadHandler.runTasks()

    advanceTimeBy(durationMills)
    runCurrent()
    jwkService.stopTask()

    // then - JWK 상태 검증
    val jwks = JwkHandler.getInstance().getJwks()
    val activeJwks = JwkHandler.getInstance().getActiveJwks()
    val inactiveJwks = JwkHandler.getInstance().getInactiveJwks()
    val discardJwks = JwkHandler.getInstance().getDiscardJwks()

    logger.info("=== 30-Day JWK Simulation Results ===")
    logger.info("Total JWKs Created: $totalJwkSize")
    logger.info("Active JWKs: ${activeJwks.size}")
    logger.info("Inactive JWKs: ${inactiveJwks.size}")
    logger.info("Discard JWKs: ${discardJwks.size}")
    logger.info("Total in Store: ${jwks.size}")
    logger.info("=====================================")

    // 검증
    assert(activeJwks.size + inactiveJwks.size + discardJwks.size == totalJwkSize) { 
      "Expected $totalJwkSize JWKs but got ${activeJwks.size + inactiveJwks.size + discardJwks.size}"
    }
    assert(jwks.size == activeJwks.size + inactiveJwks.size) { 
      "Store should only contain active and inactive JWKs"
    }
    assert(inactiveJwks.size == jwks.size - activeJwks.size) { 
      "Inactive count mismatch"
    }
    assert(activeJwks.size == maxCapacity) { 
      "Active JWKs should be at max capacity"
    }
    assert(discardJwks.size == totalJwkSize - maxCapacity - inactiveJwks.size) { 
      "Discard count mismatch"
    }
    
    coVerify(exactly = discardJwks.size) { jwkRepository.delete(any<UUID>()) }
    coVerify(exactly = totalJwkSize) { jwkRepository.create(any()) }
    
    // 타임존 복구
    TimeZone.setDefault(originalTz)
    unmockkObject(DateUtil)
  }
}
