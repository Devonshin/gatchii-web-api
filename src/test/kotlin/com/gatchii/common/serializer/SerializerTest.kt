/**
 * @author Devonshin
 * @date 2025-10-10
 */
package com.gatchii.common.serializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 직렬화 클래스 테스트
 * 
 * 태스크 #5.2: 직렬화 클래스 테스트 구현
 * 
 * 검증 항목:
 * 1. UUIDSerializer - UUID 직렬화/역직렬화
 * 2. OffsetDateTimeSerializer - OffsetDateTime 직렬화/역직렬화
 */
@UnitTest
class SerializerTest {

  private val json = Json { prettyPrint = false }

  /**
   * UUIDSerializer - 기본 직렬화 테스트
   */
  @Test
  fun `UUIDSerializer should serialize UUID to string`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = UUIDSerializer::class)
      val id: UUID
    )
    
    val uuid = UUID.randomUUID()
    val testData = TestData(uuid)
    
    // When
    val serialized = json.encodeToString(testData)
    
    // Then
    assertNotNull(serialized, "Serialized string should not be null")
    assertTrue(serialized.contains(uuid.toString()), "Serialized string should contain UUID")
  }

  /**
   * UUIDSerializer - 역직렬화 테스트
   */
  @Test
  fun `UUIDSerializer should deserialize string to UUID`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = UUIDSerializer::class)
      val id: UUID
    )
    
    val uuid = UUID.randomUUID()
    val jsonString = """{"id":"$uuid"}"""
    
    // When
    val deserialized = json.decodeFromString<TestData>(jsonString)
    
    // Then
    assertNotNull(deserialized, "Deserialized object should not be null")
    assertEquals(uuid, deserialized.id, "Deserialized UUID should match original")
  }

  /**
   * UUIDSerializer - 직렬화/역직렬화 왕복 테스트
   */
  @Test
  fun `UUIDSerializer should support round-trip serialization`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = UUIDSerializer::class)
      val id: UUID,
      @Serializable(with = UUIDSerializer::class)
      val secondId: UUID
    )
    
    val originalData = TestData(
      id = UUID.randomUUID(),
      secondId = UUID.randomUUID()
    )
    
    // When
    val serialized = json.encodeToString(originalData)
    val deserialized = json.decodeFromString<TestData>(serialized)
    
    // Then
    assertEquals(originalData.id, deserialized.id, "First UUID should match")
    assertEquals(originalData.secondId, deserialized.secondId, "Second UUID should match")
  }

  /**
   * UUIDSerializer - 특수 UUID 형식 테스트
   */
  @Test
  fun `UUIDSerializer should handle various UUID formats`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = UUIDSerializer::class)
      val id: UUID
    )
    
    val testUUIDs = listOf(
      UUID.fromString("00000000-0000-0000-0000-000000000000"), // nil UUID
      UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), // max UUID
      UUID.randomUUID()
    )
    
    // When & Then
    testUUIDs.forEach { uuid ->
      val testData = TestData(uuid)
      val serialized = json.encodeToString(testData)
      val deserialized = json.decodeFromString<TestData>(serialized)
      
      assertEquals(uuid, deserialized.id, "UUID $uuid should survive serialization")
    }
  }

  /**
   * OffsetDateTimeSerializer - 기본 직렬화 테스트
   */
  @Test
  fun `OffsetDateTimeSerializer should serialize OffsetDateTime to string`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = OffsetDateTimeSerializer::class)
      val timestamp: OffsetDateTime
    )
    
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val testData = TestData(now)
    
    // When
    val serialized = json.encodeToString(testData)
    
    // Then
    assertNotNull(serialized, "Serialized string should not be null")
    assertTrue(serialized.contains("timestamp"), "Serialized string should contain field name")
  }

  /**
   * OffsetDateTimeSerializer - 역직렬화 테스트
   */
  @Test
  fun `OffsetDateTimeSerializer should deserialize string to OffsetDateTime`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = OffsetDateTimeSerializer::class)
      val timestamp: OffsetDateTime
    )
    
    val dateTime = OffsetDateTime.of(2025, 10, 10, 12, 30, 45, 0, ZoneOffset.UTC)
    val isoString = dateTime.toString()
    val jsonString = """{"timestamp":"$isoString"}"""
    
    // When
    val deserialized = json.decodeFromString<TestData>(jsonString)
    
    // Then
    assertNotNull(deserialized, "Deserialized object should not be null")
    assertEquals(dateTime, deserialized.timestamp, "Deserialized datetime should match original")
  }

  /**
   * OffsetDateTimeSerializer - 직렬화/역직렬화 왕복 테스트
   */
  @Test
  fun `OffsetDateTimeSerializer should support round-trip serialization`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = OffsetDateTimeSerializer::class)
      val createdAt: OffsetDateTime,
      @Serializable(with = OffsetDateTimeSerializer::class)
      val updatedAt: OffsetDateTime
    )
    
    val originalData = TestData(
      createdAt = OffsetDateTime.now(ZoneOffset.UTC),
      updatedAt = OffsetDateTime.now(ZoneOffset.of("+09:00"))
    )
    
    // When
    val serialized = json.encodeToString(originalData)
    val deserialized = json.decodeFromString<TestData>(serialized)
    
    // Then
    assertEquals(originalData.createdAt, deserialized.createdAt, "CreatedAt should match")
    assertEquals(originalData.updatedAt, deserialized.updatedAt, "UpdatedAt should match")
  }

  /**
   * OffsetDateTimeSerializer - 다양한 타임존 테스트
   */
  @Test
  fun `OffsetDateTimeSerializer should handle various time zones`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = OffsetDateTimeSerializer::class)
      val timestamp: OffsetDateTime
    )
    
    val testDateTimes = listOf(
      OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), // UTC
      OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.of("+09:00")), // KST
      OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.of("-08:00")), // PST
      OffsetDateTime.of(2025, 12, 31, 23, 59, 59, 999999999, ZoneOffset.UTC) // Max nano
    )
    
    // When & Then
    testDateTimes.forEach { dateTime ->
      val testData = TestData(dateTime)
      val serialized = json.encodeToString(testData)
      val deserialized = json.decodeFromString<TestData>(serialized)
      
      assertEquals(dateTime, deserialized.timestamp, "DateTime $dateTime should survive serialization")
    }
  }

  /**
   * 복합 객체 직렬화 테스트 - UUID와 OffsetDateTime 함께 사용
   */
  @Test
  fun `serializers should work together in complex object`() {
    // Given
    @Serializable
    data class ComplexData(
      @Serializable(with = UUIDSerializer::class)
      val id: UUID,
      val name: String,
      @Serializable(with = OffsetDateTimeSerializer::class)
      val createdAt: OffsetDateTime,
      @Serializable(with = OffsetDateTimeSerializer::class)
      val updatedAt: OffsetDateTime?
    )
    
    val originalData = ComplexData(
      id = UUID.randomUUID(),
      name = "Test Entity",
      createdAt = OffsetDateTime.now(ZoneOffset.UTC),
      updatedAt = null
    )
    
    // When
    val serialized = json.encodeToString(originalData)
    val deserialized = json.decodeFromString<ComplexData>(serialized)
    
    // Then
    assertEquals(originalData.id, deserialized.id, "ID should match")
    assertEquals(originalData.name, deserialized.name, "Name should match")
    assertEquals(originalData.createdAt, deserialized.createdAt, "CreatedAt should match")
    assertEquals(originalData.updatedAt, deserialized.updatedAt, "UpdatedAt should match")
  }

  /**
   * Nullable 값 처리 테스트 - UUID
   */
  @Test
  fun `UUIDSerializer should handle nullable UUID correctly`() {
    // Given - nullable UUID가 null일 때
    @Serializable
    data class TestData(
      @Serializable(with = UUIDSerializer::class)
      val id: UUID?
    )
    
    val dataWithNull = TestData(id = null)
    val dataWithValue = TestData(id = UUID.randomUUID())
    
    // When
    val serializedNull = json.encodeToString(dataWithNull)
    val serializedValue = json.encodeToString(dataWithValue)
    
    val deserializedNull = json.decodeFromString<TestData>(serializedNull)
    val deserializedValue = json.decodeFromString<TestData>(serializedValue)
    
    // Then
    assertEquals(null, deserializedNull.id, "Null UUID should remain null")
    assertEquals(dataWithValue.id, deserializedValue.id, "Non-null UUID should match")
  }

  /**
   * Nullable 값 처리 테스트 - OffsetDateTime
   */
  @Test
  fun `OffsetDateTimeSerializer should handle nullable OffsetDateTime correctly`() {
    // Given
    @Serializable
    data class TestData(
      @Serializable(with = OffsetDateTimeSerializer::class)
      val timestamp: OffsetDateTime?
    )
    
    val dataWithNull = TestData(timestamp = null)
    val dataWithValue = TestData(timestamp = OffsetDateTime.now(ZoneOffset.UTC))
    
    // When
    val serializedNull = json.encodeToString(dataWithNull)
    val serializedValue = json.encodeToString(dataWithValue)
    
    val deserializedNull = json.decodeFromString<TestData>(serializedNull)
    val deserializedValue = json.decodeFromString<TestData>(serializedValue)
    
    // Then
    assertEquals(null, deserializedNull.timestamp, "Null timestamp should remain null")
    assertEquals(dataWithValue.timestamp, deserializedValue.timestamp, "Non-null timestamp should match")
  }
}
