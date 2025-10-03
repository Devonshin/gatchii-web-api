package shared.fixture.builder

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import shared.common.UnitTest

/**
 * @author Devonshin
 * @date 2025-10-03
 */
@UnitTest
class JwkTestBuilderTest {

    @Test
    fun `JwkTestBuilder 기본값 생성`() {
        // Given
        val jwk = JwkTestBuilder().build()
        // Then
        assertThat(jwk.privateKey).isNotBlank()
        assertThat(jwk.publicKey).isNotBlank()
        assertThat(jwk.createdAt).isNotNull()
    }
}