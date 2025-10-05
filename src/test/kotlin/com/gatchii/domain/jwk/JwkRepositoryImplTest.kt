package com.gatchii.domain.jwk

import com.gatchii.common.exception.NotSupportMethodException
import com.gatchii.common.utils.ECKeyPairHandler
import io.ktor.util.*
import io.ktor.util.logging.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.common.UnitTest
import shared.repository.DatabaseFactoryForTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.BeforeTest

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 18/12/2024 */

@UnitTest
class JwkRepositoryImplTest {
  private val log = KtorSimpleLogger(this::class.simpleName ?: "JwkRepositoryImplTest")
  private lateinit var jwkRepository: JwkRepositoryImpl
  private var toReadJwkModel: JwkModel? = null
  private var toDeleteJwkModel: JwkModel? = null

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

    val list = List<JwkModel>(10) {
      val generateKeyPair = ECKeyPairHandler.generateKeyPair()
      JwkModel(
        privateKey = generateKeyPair.private.encoded.encodeBase64(),
        publicKey = generateKeyPair.public.encoded.encodeBase64(),
        createdAt = OffsetDateTime.now(),
        deletedAt = if (it % 4 == 0) OffsetDateTime.now() else null
      )
    }
    runBlocking {
      transaction {
        JwkTable.deleteAll()
      }
      val batchCreate = jwkRepository.batchCreate(list)
      toReadJwkModel = batchCreate[0]
      toDeleteJwkModel = batchCreate[1]
    }
  }

  val jwkModel = JwkModel(
    privateKey = "privateKey",
    publicKey = "publicKey",
    createdAt = OffsetDateTime.now(),
    deletedAt = OffsetDateTime.now()
  )
  val jwkModel1 = JwkModel(
    privateKey = "privateKey1",
    publicKey = "publicKey1",
    createdAt = OffsetDateTime.now(),
    deletedAt = OffsetDateTime.now(),
    id = UUID.randomUUID(),
  )
  val jwkModel2 = JwkModel(
    privateKey = "privateKey2",
    publicKey = "publicKey2",
    createdAt = OffsetDateTime.now(),
    deletedAt = OffsetDateTime.now(),
    id = UUID.randomUUID(),
  )
  val jwkModel3 = JwkModel(
    privateKey = "privateKey3",
    publicKey = "publicKey3",
    createdAt = OffsetDateTime.now(),
    deletedAt = OffsetDateTime.now(),
    id = UUID.randomUUID(),
  )

  @Test
  fun `create without id should has generated id`() = runTest {
    //given
    //when
    val create = jwkRepository.create(jwkModel)
    //then
    assert(create.id != null)
    assert(create.privateKey == jwkModel.privateKey)
    assert(create.publicKey == jwkModel.publicKey)
    assert(create.createdAt == jwkModel.createdAt)
    assert(create.deletedAt == jwkModel.deletedAt)
  }

  @Test
  fun `create with id should has original id`() = runTest {
    //given
    //when
    val create = jwkRepository.create(jwkModel1)
    //then
    assert(create.id == jwkModel1.id)
    assert(create.privateKey == jwkModel1.privateKey)
    assert(create.publicKey == jwkModel1.publicKey)
    assert(create.createdAt == jwkModel1.createdAt)
    assert(create.deletedAt == jwkModel1.deletedAt)
  }

  @Test
  fun batchCreate() = runTest {
    //given
    val listOf = listOf(jwkModel1, jwkModel2, jwkModel3)
    val groupBy = listOf.groupBy { it.id }.mapValues { it.value.first() }
    //when
    val creates =
      jwkRepository.batchCreate(listOf)
    //then
    assert(creates.size == 3)
    for (created in creates) {
      assert(groupBy[created.id] != null)
      val model = groupBy[created.id]!!
      assert(created.privateKey == model.privateKey)
      assert(created.publicKey == model.publicKey)
      assert(created.createdAt == model.createdAt)
      assert(created.deletedAt == model.deletedAt)
    }
  }

  @Test
  fun `test if getOne returns correct item by index`() = runTest {
    //given
    val list = jwkRepository.findAll().filter { it.deletedAt == null }
    //when
    val itemAtIndexZero = jwkRepository.getUsableOne(0)
    //then
    assert(list.isNotEmpty())
    assert(itemAtIndexZero != null)
    assert(itemAtIndexZero?.id == list[0].id)
  }

  @Test
  fun `test if getOne returns null for out of bounds index`() = runTest {
    //given
    val listSize = jwkRepository.findAll().size
    //when
    assertThrows<IndexOutOfBoundsException> { jwkRepository.getUsableOne(listSize + 1) }
    //then
  }

  @Test
  fun `test if getOne does not return deleted items`() = runTest {
    //given
    val list = jwkRepository.findAll() // Assuming not all entries are marked as deleted
    val lastIndex = list.filter { it.deletedAt == null }.let { nonDeletedList ->
      nonDeletedList.size - 1
    }
    //when
    val potentialDeleted = jwkRepository.getUsableOne(lastIndex)
    //then
    assert(potentialDeleted != null)
    assert(potentialDeleted?.deletedAt == null)
  }

  @Test
  fun findAll() = runTest {
    //given
    //when
    val list = jwkRepository.findAll()
    val expected = jwkRepository.getAllUsable(null).datas
    //then
    assert(list.size == expected.size)
  }

  @Test
  fun `read`() = runTest {
    //given
    val id = toReadJwkModel?.id!!
    //when
    val read = jwkRepository.read(id)
    //then
    assert(read?.id == id)
    assert(read?.privateKey == toReadJwkModel?.privateKey)
    assert(read?.publicKey == toReadJwkModel?.publicKey)
    assert(read?.createdAt == toReadJwkModel?.createdAt)
  }

  @Test
  fun `usable jwk check`() = runTest {
    //given
    val id = toReadJwkModel?.id!!
    //when
    val read = jwkRepository.read(id)
    //then

    assert(read?.id == id)
    assert(read?.privateKey == toReadJwkModel?.privateKey)
    assert(read?.publicKey == toReadJwkModel?.publicKey)
    assert(read?.createdAt == toReadJwkModel?.createdAt)
  }

  @Test
  fun update() = runTest {
    assertThrows<NotSupportMethodException> {
      jwkRepository.update(jwkModel)
    }
  }

  @Test
  fun `delete by object`() = runTest {
    //given
    val id = toDeleteJwkModel?.id!!
    //when
    jwkRepository.delete(domain = toDeleteJwkModel!!)
    //then
    val read = jwkRepository.read(id)
    assert(read?.deletedAt != null)
  }

  @Test
  fun `test getAllUsable returns all usable jwks`() = runTest {
    //given
    val allJwks = jwkRepository.findAll()
    val expectedJwks = allJwks.filter { it.deletedAt == null }

    //when
    val result = jwkRepository.getAllUsable(null)

    //then
    assert(result.datas.size == expectedJwks.size)
    assert(result.datas.containsAll(expectedJwks))
    assert(result.datas.all { it.deletedAt == null })
  }

  @Test
  fun `test getAllUsable jwks started before lastId`() = runTest {
    //given
    val sortedAllJwks = jwkRepository.findAll().sortedByDescending { it.id }
    val fifthId = sortedAllJwks[5].id!! // Picking a middle id
    val expectedJwks = sortedAllJwks.filter { it.id.toString() < fifthId.toString() }

    //when
    val result = jwkRepository.getAllUsable(fifthId, true, 10, true)

    //then
    assert(result.datas.containsAll(expectedJwks))
  }

  @Test
  fun `test getAllUsable limits results item count`() = runTest {
    //given
    val listSize = 10
    val allJwks = List(15) {
      JwkModel(
        privateKey = "privateKey$it",
        publicKey = "publicKey$it",
        createdAt = OffsetDateTime.now(),
        deletedAt = if (it % 3 == 0) OffsetDateTime.now() else null,
        id = UUID.randomUUID()
      )
    }
    runBlocking { jwkRepository.batchCreate(allJwks) }
    //when
    val result = jwkRepository.getAllUsable(null, true, listSize)
    //then
    assert(result.datas.size <= listSize)
    assert(result.datas.all { it.deletedAt == null })
  }

  @Test
  fun `test getAllUsable slice for 32 non deleted datas`() = runTest {
    //given
    val sliceSize = 7
    val listSize = 25
    val allJwks = List(listSize) {
      val descIdx = listSize - (it + 1)
      JwkModel(
        privateKey = "privateKey${descIdx}",
        publicKey = "publicKey$descIdx",
        createdAt = OffsetDateTime.now(),
      )
    }
    runBlocking { jwkRepository.batchCreate(allJwks) }
    //when
    var result = jwkRepository.getAllUsable(null, true, sliceSize)
    var count = 1
    while (result.hasMoreData) { //don't touch for test
      result = jwkRepository.getAllUsable(result.lastId(), true, sliceSize)
      count++
    }

    //then
    assert(count == 5)
    assert(result.datas.all { it.deletedAt == null })
  }

  @Test
  fun `test getAllUsable slice for 35 datas with 12 deleted `() = runTest {
    //given
    val sliceSize = 7
    val listSize = 25
    val allJwks = List(listSize) {
      val descIdx = listSize - (it + 1)
      JwkModel(
        privateKey = "privateKey${descIdx}",
        publicKey = "publicKey$descIdx",
        createdAt = OffsetDateTime.now(),
        deletedAt = if (it % 3 == 0) OffsetDateTime.now() else null,
      )
    }
    runBlocking { jwkRepository.batchCreate(allJwks) }
    //when
    var result = jwkRepository.getAllUsable(null, true, sliceSize, true)
    var count = 1
    var deletedCount = result.datas.count { it.deletedAt != null }
    while (result.hasMoreData) { //don't touch for test
      val lastId = result.lastId()
      result = jwkRepository.getAllUsable(lastId, true, sliceSize, true)
      count++
      deletedCount += result.datas.count { it.deletedAt != null }
    }

    //then
    assert(count == 5)
    assert(deletedCount == 12)
  }

  @Test
  fun `delete by id should soft delete`() = runTest {
    // given
    val id = toDeleteJwkModel?.id!!
    // when
    jwkRepository.delete(id)
    // then
    val read = jwkRepository.read(id)
    assert(read?.deletedAt != null)
  }

  @Test
  fun `read returns null for non-existent id`() = runTest {
    // when
    val read = jwkRepository.read(UUID.randomUUID())
    // then
    assert(read == null)
  }

  @Test
  fun `findAll is sorted by id desc`() = runTest {
    // when
    val list = jwkRepository.findAll()
    // then
    val ids = list.map { it.id!! }
    val sorted = ids.sortedDescending()
    assert(ids == sorted)
  }

  @Test
  fun `create with too long privateKey should throw`() = runTest {
    // given: privateKey length limit is 512
    val tooLong = "a".repeat(513)
    val model = JwkModel(
      privateKey = tooLong,
      publicKey = "publicKey",
      createdAt = OffsetDateTime.now()
    )
    // then
    assertThrows<Exception> {
      runBlocking { jwkRepository.create(model) }
    }
  }

  @Test
  fun `batchCreate should throw when one item is invalid`() = runTest {
    // given
    val valid = JwkModel(privateKey = "ok", publicKey = "ok", createdAt = OffsetDateTime.now())
    val invalid = JwkModel(privateKey = "a".repeat(513), publicKey = "ok", createdAt = OffsetDateTime.now())
    // then
    assertThrows<Exception> {
      runBlocking { jwkRepository.batchCreate(listOf(valid, invalid)) }
    }
    // 주의: Exposed batchInsert의 트랜잭션 동작은 부분 삽입이 발생할 수 있어, 삽입 결과 건수에 대한 단언은 하지 않습니다.
  }

  @Test
  fun `withTransaction rolls back all writes when any operation fails`() = runTest {
    // given
    val before = jwkRepository.findAll().size
    val valid = JwkModel(privateKey = "ok", publicKey = "ok", createdAt = OffsetDateTime.now())
    val invalid = JwkModel(privateKey = "a".repeat(513), publicKey = "ok", createdAt = OffsetDateTime.now())
    // when: 하나의 트랜잭션 경계 안에서 유효/무효 연속 호출 → 실패 시 전체 롤백 기대
    assertThrows<Exception> {
      com.gatchii.common.repository.RepositoryTransactionRunner.withTransaction {
        runBlocking {
          jwkRepository.create(valid)
          jwkRepository.create(invalid) // 예외 발생 지점
        }
      }
    }
    // then: 첫 번째 create 역시 롤백되어 총 개수 변화가 없어야 함
    val after = jwkRepository.findAll().size
    assert(after == before)
  }
}
