package shared.fixture.generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import shared.common.UnitTest

/**
 * @author Devonshin
 * @date 2025-10-03
 */
@UnitTest
class TestDataGeneratorTest {

    @Test
    fun `alphaNumeric 기본 동작`() {
        // Given
        // When
        val s = TestDataGenerator.alphaNumeric(12)
        // Then
        assertThat(s).hasSize(12)
        assertThat(s).matches("[A-Za-z0-9]{12}")
    }

    @Test
    fun `numeric 기본 동작`() {
        // Given
        // When
        val s = TestDataGenerator.numeric(6)
        // Then
        assertThat(s).hasSize(6)
        assertThat(s).matches("[0-9]{6}")
    }

    @Test
    fun `password 최소 길이 및 다양성`() {
        // Given
        val p = TestDataGenerator.password(12)
        // Then
        assertThat(p).hasSize(12)
    }

    @Test
    fun `email 기본 동작`() {
        // Given
        val email = TestDataGenerator.email(10, "test.dev")
        // Then
        assertThat(email).contains("@test.dev")
        assertThat(email.substringBefore("@")).hasSize(10)
    }

    @Test
    fun `uuid 형식`() {
        // Given
        val id = TestDataGenerator.uuid()
        // Then
        assertThat(id).matches("[0-9a-fA-F-]{36}")
    }
}