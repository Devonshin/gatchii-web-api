package com.gatchii.domain.jwk

import com.typesafe.config.ConfigFactory
import io.ktor.util.logging.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.*

@UnitTest
class JwkHandlerTest {

    private val logger = KtorSimpleLogger(this::class::simpleName.get() ?: "JwkHandlerTest")

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
    }

    @Test
    fun `getConfigValue returns value if key exists`() {
        //given
        val result = JwkHandler.getConfigValue("maxCapacity")?.toInt()
        //then
        assert(result != null)
    }

    @Test
    fun `getConfigValue returns null if key does not exist`() {
        //given
        val key = "test.nonExistingKey"
        //when
        val result = JwkHandler.getConfigValue(key)
        //then
        assertNull(result)
    }

    @Test
    fun `add jwk does not exceed max capacity`() {

        //given
        val maxCapacity = JwkHandler.getConfigValue("maxCapacity")?.toInt() ?: 0
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
        jwkModelList.forEach { JwkHandler.addJwk(it) }

        //then
        val jwks = JwkHandler.getJwks()
        val activeCount = jwks.count() { it.status == JwkStatus.ACTIVE }
        val inactiveCount = jwks.count() { it.status == JwkStatus.INACTIVE }
        assertEquals(maxCapacity, activeCount)
        assertEquals(inactiveCount, maxCapacityPlus - maxCapacity)
        assertEquals(maxCapacityPlus, JwkHandler.getJwks().size)
    }

    @Test
    fun `add jwk removes oldest when capacity exceeded`() {

        //given
        val maxCapacity = JwkHandler.getConfigValue("maxCapacity")?.toInt() ?: 0
        val oldestJwk = JwkModel(
            privateKey = "privateKey_oldest",
            publicKey = "publicKey_oldest",
            createdAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        JwkHandler.addJwk(oldestJwk)

        val newJwk = JwkModel(
            privateKey = "privateKey_new",
            publicKey = "publicKey_new",
            createdAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        repeat(maxCapacity) {
            JwkHandler.addJwk(
                JwkModel(
                    privateKey = "privateKey$it",
                    publicKey = "publicKey$it",
                    createdAt = OffsetDateTime.now(),
                    id = UUID.randomUUID()
                )
            )
        }

        //when
        JwkHandler.addJwk(newJwk)

        //then
        assertTrue(JwkHandler.getJwks().contains(oldestJwk))
        assertTrue(JwkHandler.getJwks().contains(newJwk))
    }

    @Test
    fun `add jwk adds to front of list`() {
        //given
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
        JwkHandler.addJwk(firstJwk)
        JwkHandler.addJwk(secondJwk)

        //then
        assertEquals(secondJwk, JwkHandler.getJwks().first())
        assertEquals(firstJwk, JwkHandler.getJwks().last())
    }

    @Test
    fun `add jwk retains correct order after multiple additions`() {

        //given
        val jwks = List(5) {
            JwkModel(
                privateKey = "privateKey$it",
                publicKey = "publicKey$it",
                createdAt = OffsetDateTime.now(),
                id = UUID.randomUUID()
            )
        }

        //when
        jwks.forEach { JwkHandler.addJwk(it) }

        //then
        val deque = JwkHandler.getJwks()
        logger.info("deque size: ${deque.size}")
        jwks.reversed().forEachIndexed { index, jwk ->
            assertEquals(jwk, deque[index])
        }
    }

    @RepeatedTest(30)
    fun `test getRandomUsableJwk when jwks has one element should return the only element`() {
        // given
        val jwk = JwkModel(
            privateKey = "privateKey1",
            publicKey = "publicKey1",
            createdAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        JwkHandler.addJwk(jwk)

        // when
        val randomJwk = JwkHandler.getRandomActiveJwk()

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
        JwkHandler.addJwk(jwk1)
        JwkHandler.addJwk(jwk2)

        // when
        val randomJwk = JwkHandler.getRandomActiveJwk().get()
        logger.info("jwk1: $jwk1")
        logger.info("jwk2: $jwk2")
        logger.info("randomJwk: $randomJwk")
        // then
        Assertions.assertTrue(randomJwk == jwk1 || randomJwk == jwk2)
    }

    @Test
    fun `test addJwk moves oldest jwk to inactiveJwks when max capacity is reached`() = runTest {
        // given
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
        JwkHandler.setConfig(ConfigFactory.parseString("maxCapacity=1"))

        // when
        JwkHandler.addJwk(firstJwk)
        JwkHandler.addJwk(secondJwk)

        // then
        val activeJwks = JwkHandler.getJwks().filter { it.status == JwkStatus.ACTIVE }
        val inactiveJwks = JwkHandler.getJwks().filter { it.status == JwkStatus.INACTIVE }
        assert(inactiveJwks.size == 1)
        assert(activeJwks.size == 1)
        assert(activeJwks.contains(secondJwk))
        assert(inactiveJwks.first().id == firstJwk.id)
        JwkHandler.setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
    }

    @Test
    fun `test addJwk does not exceed max capacity of activeJwks`() = runTest {
        // given
        JwkHandler.setConfig(ConfigFactory.parseString("maxCapacity=2"))

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
        JwkHandler.addJwk(jwk1)
        JwkHandler.addJwk(jwk2)
        JwkHandler.addJwk(jwk3)

        // then
        val activeJwks = JwkHandler.getJwks().filter { it.status == JwkStatus.ACTIVE }

        assert(activeJwks.size == 2)
        assert(activeJwks.contains(jwk3))
        assert(activeJwks.contains(jwk2))
        JwkHandler.setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
    }

    @Test
    fun `test addJwk retains correct order in activeJwks`() = runTest {
        // given
        JwkHandler.setConfig(ConfigFactory.parseString("maxCapacity=3"))
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
        JwkHandler.addJwk(jwk1)
        JwkHandler.addJwk(jwk2)
        JwkHandler.addJwk(jwk3)

        // then
        val activeJwks = JwkHandler.getJwks().filter { it.status == JwkStatus.ACTIVE }
        assert(activeJwks.size == 3)
        assert(activeJwks[0] == jwk3)
        assert(activeJwks[1] == jwk2)
        assert(activeJwks[2] == jwk1)
        JwkHandler.setConfig(ConfigFactory.load("application-test.conf").getConfig("jwk"))
    }

    // Test if removeJwks removes active jwks that are marked as deleted
    @Test
    fun test_removeJwks_removes_active_jwks_that_are_marked_as_deleted() = runTest {
        // given
        JwkHandler.clearAll()
        val newJwk = JwkModel(
            privateKey = "privateKey1",
            publicKey = "publicKey1",
            createdAt = OffsetDateTime.now(),
            deletedAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        JwkHandler.addJwk(newJwk)

        // when
        JwkHandler.removeJwks(listOf(newJwk))

        // then
        assertFalse(JwkHandler.getJwks().any { it.id == newJwk.id })
    }

    // Test if removeJwks does not remove active jwks that are not marked as deleted
    @Test
    fun test_removeJwks_does_not_remove_active_jwks_that_are_not_marked_as_deleted() = runTest {
        // given
        JwkHandler.clearAll()
        val newJwk = JwkModel(
            privateKey = "privateKey1",
            publicKey = "publicKey1",
            createdAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        JwkHandler.addJwk(newJwk)

        // when
        JwkHandler.removeJwks(listOf(newJwk))

        // then
        assertTrue(JwkHandler.getJwks().any { it.id == newJwk.id })
    }

    // Test if removeJwks removes inactive jwks that are marked as deleted
    @Test
    fun test_removeJwks_removes_inactive_jwks_that_are_marked_as_deleted() = runTest {
        // given
        JwkHandler.clearAll()
        val newJwk = JwkModel(
            privateKey = "privateKey1",
            publicKey = "publicKey1",
            createdAt = OffsetDateTime.now(),
            deletedAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        JwkHandler.addInactiveJwk(newJwk)

        // when
        JwkHandler.removeJwks(listOf(newJwk))

        // then
        assertFalse(JwkHandler.getJwks().any { it.id == newJwk.id })
    }

    // Test if removeJwks does not remove inactive jwks that are not marked as deleted
    @Test
    fun test_removeJwks_does_not_remove_inactive_jwks_that_are_not_marked_as_deleted() = runTest {
        // given
        JwkHandler.clearAll()
        val newJwk = JwkModel(
            privateKey = "privateKey1",
            publicKey = "publicKey1",
            createdAt = OffsetDateTime.now(),
            id = UUID.randomUUID()
        )
        JwkHandler.addInactiveJwk(newJwk)
        // when
        JwkHandler.removeJwks(listOf(newJwk))
        // then
        assertTrue(JwkHandler.getJwks().any { it.id == newJwk.id })
    }


}