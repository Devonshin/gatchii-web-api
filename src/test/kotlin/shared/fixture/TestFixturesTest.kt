package shared.fixture

import com.auth0.jwt.algorithms.Algorithm
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
class TestFixturesTest {

  @Test
  fun `LoginModel 기본값 생성 및 커스터마이징 검증`() {
    // Given
    // When
    val defaultUser = TestFixtures.createLoginModel()
    val customUser = TestFixtures.createLoginModel(
      prefixId = "tester",
      suffixId = "9999",
      password = "secret",
      status = LoginStatus.INACTIVE,
      role = UserRole.ADMIN,
    )
    // Then
    assertThat(defaultUser.prefixId).isNotBlank()
    assertThat(defaultUser.suffixId).isNotBlank()
    assertThat(defaultUser.password).isNotBlank()

    assertThat(customUser.prefixId).isEqualTo("tester")
    assertThat(customUser.suffixId).isEqualTo("9999")
    assertThat(customUser.password).isEqualTo("secret")
    assertThat(customUser.status).isEqualTo(LoginStatus.INACTIVE)
    assertThat(customUser.role).isEqualTo(UserRole.ADMIN)
  }

  @Test
  fun `JWT 토큰 생성 기본값 동작 검증`() {
    // Given
    val algorithm: Algorithm = TestFixtures.createAlgorithmWithNewKeyPair()
    val jwtConfig = TestFixtures.createJwtConfig()
    // When
    val token = TestFixtures.createJwtToken(algorithm = algorithm, jwtConfig = jwtConfig)
    // Then
    assertThat(token).isNotBlank()
  }

  @Test
  fun `JwkModel 기본값 생성 검증`() {
    // Given
    // When
    val jwk = TestFixtures.createJwkModel()
    // Then
    assertThat(jwk.privateKey).isNotBlank()
    assertThat(jwk.publicKey).isNotBlank()
    assertThat(jwk.createdAt).isNotNull()
  }
}