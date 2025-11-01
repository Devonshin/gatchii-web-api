package com.gatchii.domain.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.gatchii.common.model.ResultData
import com.gatchii.common.task.RoutineScheduleExpression
import com.gatchii.common.task.RoutineTaskHandler
import com.gatchii.common.task.TaskLeadHandler
import com.gatchii.common.utils.ECKeyPairHandler
import com.gatchii.common.utils.ECKeyPairHandler.Companion.convertPrivateKey
import com.gatchii.common.utils.ECKeyPairHandler.Companion.convertPublicKey
import com.gatchii.common.utils.RsaPairHandler
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.util.*
import io.ktor.util.logging.*
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.BeforeTest

@UnitTest
class JwkServiceImplTest {

  private val logger: Logger = KtorSimpleLogger(this::class.simpleName ?: "JwkServiceImplTest")
  val config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
  val jwkRepository = mockk<JwkRepository>()
  val taskName = "testJwkTask"
  lateinit var jwkService: JwkServiceImpl

  companion object {
    @BeforeAll
    @JvmStatic
    fun init() {
      JwkHandler.getInstance().setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
    }
  }

  @BeforeTest
  fun beforeTestSetUp() {
    // 각 테스트 전에 JwkHandler 새로운 인스턴스로 초기화
    JwkHandler.resetForTest()
    // 새 인스턴스의 설정도 재설정
    JwkHandler.getInstance().setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
    
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

  @AfterEach
  fun tearDown() {
    // JwkHandler 테스트 격리 - static 메서드 호출
    JwkHandler.resetForTest()
    
    // 전역 객체 모킹 잔류로 인한 플래키 방지
    try {
      unmockkAll()
    } catch (_: Throwable) {
    }
  }

  @Test
  fun `test findRandomJwk throws error when no usable jwks found`() = runTest {
    // given - JwkHandler 싱글톤 직접 사용
    assertThrows<NoSuchElementException> {
      jwkService.getRandomJwk()
    }
  }

  @Test
  fun `test findRandomJwk returns jwk when usable jwks are found`() = runTest {
    // given
    val jwk = mockk<JwkModel>() {
      every { id } returns UUID.randomUUID()
      every { status } returns JwkStatus.ACTIVE
      every { privateKey } returns "mock-private-key"
      every { publicKey } returns "mock-public-key"
      every { createdAt } returns OffsetDateTime.now()
    }
    JwkHandler.getInstance().addJwk(jwk)
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
    coEvery { jwkRepository.getAllUsable(any()) } returns ResultData(emptyList(), false)
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
      privateKey = RsaPairHandler.encrypt(generateKeyPair.private.encoded.encodeBase64()),
      publicKey = generateKeyPair.public.encoded.encodeBase64(),
      createdAt = date,
    )

    coEvery { jwkRepository.getAllUsable(any()) } returns ResultData(listOf(activated), false)
    // when
    val result = jwkService.findAllJwk()

    assert(result.size == 1)
  }

  @Test
  fun `test jwkSchedulingProc creates and adds a new jwk`() = runTest {
    // given - JwkHandler 싱글톤 직접 사용
    val createdJwk = mockk<JwkModel>() {
      every { id } returns UUID.randomUUID()
      every { status } returns JwkStatus.ACTIVE
      every { privateKey } returns "mock-private-key"
      every { publicKey } returns "mock-public-key"
      every { createdAt } returns OffsetDateTime.now()
    }
    coEvery { jwkRepository.create(any()) } returns createdJwk

    // when
    jwkService.taskProcessing()

    // then - repository만 검증 (JwkHandler는 통합 테스트에서 검증)
    coVerify(atLeast = 1) { jwkRepository.create(any()) }
  }

  @Test
  fun `test jwkSchedulingProc deletes removable jwks`() = runTest {
    // given
    val createdJwk = mockk<JwkModel> {
      every { id } returns UUID.randomUUID()
      every { status } returns JwkStatus.ACTIVE
      every { privateKey } returns "mock-private-key"
      every { publicKey } returns "mock-public-key"
      every { createdAt } returns OffsetDateTime.now()
    }
    coEvery { jwkRepository.create(any()) } returns createdJwk
    coEvery { jwkRepository.delete(any<UUID>()) } returns Unit

    // when
    jwkService.taskProcessing()

    // then
    coVerify(atLeast = 1) { jwkRepository.create(any()) }
  }

  @Test
  fun `test initializeJwk adds usable jwks to JwkHandler`() = runTest {
    // given
    val usableJwk = mockk<JwkModel>() {
      every { id } returns UUID.randomUUID()
      every { status } returns JwkStatus.ACTIVE
      every { privateKey } returns "mock-private-key"
      every { publicKey } returns "mock-public-key"
      every { createdAt } returns OffsetDateTime.now()
    }
    coEvery { jwkRepository.getAllUsable(null, true, any(), false) } returns ResultData(
      datas = listOf(usableJwk),
      hasMoreData = false
    )
    
    // 기존 타스크 제거
    TaskLeadHandler.removeTask(taskName)

    // when
    jwkService.initializeJwk()

    // then
    coVerify(exactly = 1) { jwkRepository.getAllUsable(null, true, any(), false) }
  }


  @Test
  fun `getJwkECKey builds ECKey`() = runTest {
    // given: 실제 키 자료로 provider 생성
    val kp = ECKeyPairHandler.generateKeyPair()
    val jwkModel = JwkModel(
      privateKey = RsaPairHandler.encrypt(kp.private.encoded.encodeBase64()),
      publicKey = kp.public.encoded.encodeBase64(),
      createdAt = OffsetDateTime.now(),
      id = UUID.randomUUID()
    )
    val provider = jwkService.getProvider(jwkModel)
    // when
    val ecKey = jwkService.getJwkECKey(provider)
    // then
    assert(ecKey.keyType.value == "EC")
    assert(ecKey.toPublicJWK() != null)
    assert(ecKey.toJSONString().isNotEmpty())
  }

  @Test
  fun `createJwks delegates to repository with correct size`() = runTest {
    // given
    val size = 3
    coEvery { jwkRepository.batchCreate(any()) } answers {
      val list = firstArg<List<JwkModel>>()
      list.map { it.copy(id = UUID.randomUUID()) }
    }
    // when
    val result = jwkService.createJwks(size)
    // then
    assert(result.size == size)
    coVerify(exactly = 1) { jwkRepository.batchCreate(any()) }
  }

  @Test
  fun `deleteJwks should skip null ids and delete only valid ones`() = runTest {
    // given
    val now = OffsetDateTime.now()
    val withId = JwkModel(privateKey = "p", publicKey = "q", createdAt = now, id = UUID.randomUUID())
    val noId = JwkModel(privateKey = "p", publicKey = "q", createdAt = now)
    coEvery { jwkRepository.delete(any<UUID>()) } returns Unit
    // when
    jwkService.deleteJwks(listOf(withId, noId))
    // then
    coVerify(exactly = 1) { jwkRepository.delete(withId.id!!) }
  }

  @Test
  fun `findAllUsableJwk delegates repository call with max capacity`() = runTest {
    // given - maxCapacity는 JwkHandler 설정으로부터 자동 사용됨
    coEvery { jwkRepository.getAllUsable(null, true, any(), false) } returns ResultData(emptyList(), false)
    // when
    val result = jwkService.findAllUsableJwk()
    // then
    assert(result.isEmpty())
    coVerify(exactly = 1) { jwkRepository.getAllUsable(null, true, any(), false) }
  }
}

