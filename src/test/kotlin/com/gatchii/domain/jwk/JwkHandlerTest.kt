package com.gatchii.domain.jwk

import com.typesafe.config.ConfigFactory
import io.ktor.util.logging.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.*
import kotlin.test.Test

@UnitTest
class JwkHandlerTest {

  private val logger = KtorSimpleLogger(this::class::simpleName.get() ?: "JwkHandlerTest")

  companion object {
    private val defaultConfig = ConfigFactory.load("application-test.conf").getConfig("jwk")
    
    @BeforeAll
    @JvmStatic
    fun init() {
      JwkHandler.resetForTest(defaultConfig)
    }
  }

  @BeforeEach
  fun beforeTestSetUp() {
    // 테스트마다 새로운 JwkHandler 인스턴스로 초기화
    JwkHandler.resetForTest(defaultConfig)
    // 저장소 초기화
    JwkHandler.getInstance().clearAll()
    // 상태 확인 (초기화 검증)
    val isEmpty = JwkHandler.getInstance().getJwks().isEmpty()
    check(isEmpty) { "JwkHandler store should be empty after reset" }
  }

  @AfterEach
  fun afterEach() {
    // 전역 상태 및 설정 복원으로 테스트 간 간섭방지
    JwkHandler.resetForTest(defaultConfig)
  }

  @Test
  fun `getConfigValue returns value if key exists`() {
    //given
    val handler = JwkHandler.getInstance()
    val result = handler.getConfigValue("maxCapacity")?.toInt()
    //then
    assert(result != null)
  }

  @Test
  fun `getConfigValue returns null if key does not exist`() {
    //given
    val key = "test.nonExistingKey"
    //when
    val result = JwkHandler.getInstance().getConfigValue(key)
    //then
    assertNull(result)
  }

  @Test
  fun `add jwk does not exceed max capacity`() {
    //given
    JwkHandler.getInstance().clearAll()
    val maxCapacity = JwkHandler.getInstance().getConfigValue("maxCapacity")?.toInt() ?: 0
    val maxCapacityPlus = maxCapacity.plus(1)

    val jwkModelList = List(maxCapacityPlus) {
      JwkModel(
        privateKey = "privateKey$it",
        publicKey = "publicKey$it",
        createdAt = OffsetDateTime.now(),
        id = UUID.randomUUID()
      )
    }

    //when
    jwkModelList.forEach { JwkHandler.getInstance().addJwk(it) }

    //then
    val jwks = JwkHandler.getInstance().getJwks()
    val activeCount = jwks.count() { it.status == JwkStatus.ACTIVE }
    val inactiveCount = jwks.count() { it.status == JwkStatus.INACTIVE }
    assertEquals(maxCapacity, activeCount)
    assertEquals(inactiveCount, maxCapacityPlus - maxCapacity)
    assertEquals(maxCapacityPlus, JwkHandler.getInstance().getJwks().size)
  }

  @Test
  fun `add jwk removes oldest when capacity exceeded`() {
    //given
    JwkHandler.getInstance().clearAll()
    val maxCapacity = JwkHandler.getInstance().getConfigValue("maxCapacity")?.toInt() ?: 0
    val oldestJwk = JwkModel(
      privateKey = "privateKey_oldest",
      publicKey = "publicKey_oldest",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    JwkHandler.getInstance().addJwk(oldestJwk)

    val newJwk = JwkModel(
      privateKey = "privateKey_new",
      publicKey = "publicKey_new",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    repeat(maxCapacity) {
      JwkHandler.getInstance().addJwk(
        JwkModel(
          privateKey = "privateKey$it",
          publicKey = "publicKey$it",
          createdAt = OffsetDateTime.now(),
          id = UUID.randomUUID()
        )
      )
    }

    //when
    JwkHandler.getInstance().addJwk(newJwk)

    //then
    assertTrue(JwkHandler.getInstance().getJwks().contains(oldestJwk))
    assertTrue(JwkHandler.getInstance().getJwks().contains(newJwk))
  }

  @Test
  fun `add jwk adds to front of list`() {
    //given
    JwkHandler.getInstance().clearAll()
    val firstJwk = JwkModel(
      privateKey = "privateKey_first",
      publicKey = "publicKey_first",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val secondJwk = JwkModel(
      privateKey = "privateKey_second",
      publicKey = "publicKey_second",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )

    //when
    JwkHandler.getInstance().addJwk(firstJwk)
    JwkHandler.getInstance().addJwk(secondJwk)

    //then
    assertEquals(secondJwk, JwkHandler.getInstance().getJwks().first())
    assertEquals(firstJwk, JwkHandler.getInstance().getJwks().last())
  }

  @Test
  fun `add jwk retains correct order after multiple additions`() {
    //given
    JwkHandler.getInstance().clearAll()
    val jwks = List(5) {
      JwkModel(
        privateKey = "privateKey$it",
        publicKey = "publicKey$it",
        createdAt = OffsetDateTime.now(),
        id = UUID.randomUUID()
      )
    }

    //when
    jwks.forEach { JwkHandler.getInstance().addJwk(it) }

    //then
    val deque = JwkHandler.getInstance().getJwks()
    logger.info("deque size: ${deque.size}")
    jwks.reversed().forEachIndexed { index, jwk ->
      assertEquals(jwk, deque[index])
    }
  }

  @RepeatedTest(30)
  fun `test getRandomUsableJwk when jwks has one element should return the only element`() {
    // given - 문제가 발생할 수 있옜로 명시적 초기화
    JwkHandler.getInstance().clearAll()
    val jwk = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    JwkHandler.getInstance().addJwk(jwk)

    // when
    val randomJwk = JwkHandler.getInstance().getRandomActiveJwk()

    // then
    Assertions.assertEquals(jwk, randomJwk.get())
  }

  @Test
  fun `test getRandomUsableJwk when jwks has multiple elements should return a usable JWK`() {
    // given
    val jwk1 = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val jwk2 = JwkModel(
      privateKey = "privateKey2",
      publicKey = "publicKey2",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    JwkHandler.getInstance().addJwk(jwk1)
    JwkHandler.getInstance().addJwk(jwk2)

    // when
    val randomJwk = JwkHandler.getInstance().getRandomActiveJwk().get()
    logger.info("jwk1: $jwk1")
    logger.info("jwk2: $jwk2")
    logger.info("randomJwk: $randomJwk")
    // then
    Assertions.assertTrue(randomJwk == jwk1 || randomJwk == jwk2)
  }

  @Test
  fun `test addJwk moves oldest jwk to inactiveJwks when max capacity is reached`() = runTest {
    // given
    JwkHandler.getInstance().clearAll()
    JwkHandler.getInstance().setConfig(ConfigFactory.parseString("maxCapacity = 1"))
    
    val firstJwk = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val secondJwk = JwkModel(
      privateKey = "privateKey2",
      publicKey = "publicKey2",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )

    // when
    JwkHandler.getInstance().addJwk(firstJwk)
    JwkHandler.getInstance().addJwk(secondJwk)

    // then
    val activeJwks = JwkHandler.getInstance().getJwks().filter { it.status == JwkStatus.ACTIVE }
    val inactiveJwks = JwkHandler.getInstance().getJwks().filter { it.status == JwkStatus.INACTIVE }
    assert(inactiveJwks.size == 1)
    assert(activeJwks.size == 1)
    assert(activeJwks.contains(secondJwk))
    assert(inactiveJwks.first().id == firstJwk.id)
  }

  @Test
  fun `test addJwk does not exceed max capacity of activeJwks`() = runTest {
    // given
    JwkHandler.getInstance().clearAll()
    JwkHandler.getInstance().setConfig(ConfigFactory.parseString("maxCapacity = 2"))

    val jwk1 = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val jwk2 = JwkModel(
      privateKey = "privateKey2",
      publicKey = "publicKey2",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val jwk3 = JwkModel(
      privateKey = "privateKey3",
      publicKey = "publicKey3",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )

    // when
    JwkHandler.getInstance().addJwk(jwk1)
    JwkHandler.getInstance().addJwk(jwk2)
    JwkHandler.getInstance().addJwk(jwk3)

    // then
    val activeJwks = JwkHandler.getInstance().getJwks().filter { it.status == JwkStatus.ACTIVE }

    assert(activeJwks.size == 2)
    assert(activeJwks.contains(jwk3))
    assert(activeJwks.contains(jwk2))
  }

  @Test
  fun `test addJwk retains correct order in activeJwks`() = runTest {
    // given
    JwkHandler.getInstance().clearAll()
    JwkHandler.getInstance().setConfig(ConfigFactory.parseString("maxCapacity = 3"))
    
    val jwk1 = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val jwk2 = JwkModel(
      privateKey = "privateKey2",
      publicKey = "publicKey2",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val jwk3 = JwkModel(
      privateKey = "privateKey3",
      publicKey = "publicKey3",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )

    // when
    JwkHandler.getInstance().addJwk(jwk1)
    JwkHandler.getInstance().addJwk(jwk2)
    JwkHandler.getInstance().addJwk(jwk3)

    // then
    val activeJwks = JwkHandler.getInstance().getJwks().filter { it.status == JwkStatus.ACTIVE }
    assert(activeJwks.size == 3)
    assert(activeJwks[0] == jwk3)
    assert(activeJwks[1] == jwk2)
    assert(activeJwks[2] == jwk1)
  }

  // Test if removeJwks removes active jwks that are marked as deleted
  @Test
  fun test_removeJwks_removes_active_jwks_that_are_marked_as_deleted() = runTest {
    // given
    JwkHandler.getInstance().clearAll()
    val newJwk = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      deletedAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    JwkHandler.getInstance().addJwk(newJwk)

    // when
    JwkHandler.getInstance().removeJwks(listOf(newJwk))

    // then
    assertFalse(JwkHandler.getInstance().getJwks().any { it.id == newJwk.id })
  }

  // Test if removeJwks removes inactive jwks that are marked as deleted
  @Test
  fun test_removeJwks_removes_inactive_jwks_that_are_marked_as_deleted() = runTest {
    // given
    JwkHandler.getInstance().clearAll()
    val newJwk = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      deletedAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    JwkHandler.getInstance().addInactiveJwk(newJwk)

    // when
    JwkHandler.getInstance().removeJwks(listOf(newJwk))

    // then
    assertFalse(JwkHandler.getInstance().getJwks().any { it.id == newJwk.id })
  }

  // Test if removeJwks does not remove inactive jwks that are not marked as deleted
  @Test
  fun test_removeJwks_remove_inactive_jwk() = runTest {
    // given
    JwkHandler.getInstance().clearAll()
    val activeNewJwk = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val inactiveNewJwk = JwkModel(
      privateKey = "privateKey1",
      publicKey = "publicKey1",
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    JwkHandler.getInstance().addInactiveJwk(inactiveNewJwk)
    JwkHandler.getInstance().addJwk(activeNewJwk)
    // when
    JwkHandler.getInstance().removeJwks(listOf(activeNewJwk, inactiveNewJwk))
    // then
    assertFalse(JwkHandler.getInstance().getJwks().any { it.id == activeNewJwk.id || it.id == inactiveNewJwk.id })
  }


}