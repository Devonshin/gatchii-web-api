package shared.fixture.builder

import com.gatchii.domain.login.LoginStatus
import com.gatchii.domain.login.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import shared.common.UnitTest

/**
 * @author Devonshin
 * @date 2025-10-03
 */
@UnitTest
class UserTestBuilderTest {

  @Test
  fun `UserTestBuilder 기본값 및 커스터마이징`() {
    // Given
    val defaultUser = UserTestBuilder().build()
    val admin = UserTestBuilder()
      .withPrefixId("tester")
      .withSuffixId("7777")
      .withPassword("secret")
      .withStatus(LoginStatus.INACTIVE)
      .withRole(UserRole.ADMIN)
      .build()
    // Then
    assertThat(defaultUser.prefixId).isNotBlank()
    assertThat(defaultUser.suffixId).isNotBlank()
    assertThat(defaultUser.password).isNotBlank()

    assertThat(admin.prefixId).isEqualTo("tester")
    assertThat(admin.suffixId).isEqualTo("7777")
    assertThat(admin.password).isEqualTo("secret")
    assertThat(admin.role).isEqualTo(UserRole.ADMIN)
    assertThat(admin.status).isEqualTo(LoginStatus.INACTIVE)
  }
}