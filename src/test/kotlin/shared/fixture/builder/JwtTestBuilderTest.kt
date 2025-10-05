package shared.fixture.builder

import com.gatchii.utils.JwtHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import shared.common.UnitTest

/**
 * @author Devonshin
 * @date 2025-10-03
 */
@UnitTest
class JwtTestBuilderTest {

  @Test
  fun `JwtTestBuilder 토큰 생성 및 검증`() {
    // Given
    val builder = JwtTestBuilder()
      .withIssuer("BuilderIssuer")
      .withAudience("BuilderAudience")
      .withExpireSec(60)
      .withClaim("username", "builder")
    val token = builder.buildToken()
    // When
    val verified = JwtHandler.verify(token, builder.algorithm(), builder.buildConfig())
    // Then
    assertThat(token).isNotBlank()
    assertThat(verified).isTrue()
  }
}