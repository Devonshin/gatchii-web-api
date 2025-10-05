package shared.fixture.aaa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import shared.common.UnitTest

/**
 * @author Devonshin
 * @date 2025-10-03
 */
@UnitTest
class AAAHelpersTest : BaseAaaTest() {

  @Test
  fun `givenWhenThen 기본 흐름`() {
    givenWhenThen(
      arrange = { 2 to 3 },
      act = { (a, b) -> a + b },
      assert = { (a, b), r ->
        assertThat(r).isEqualTo(a + b)
      }
    )
  }

  @Test
  fun `arrange-act-assert 분리 사용`() {
    // Arrange
    val pair = arrange { 5 to 7 }
    // Act
    val result = act(pair) { (a, b) -> a * b }
    // Assert
    asserting {
      assertThat(result).isEqualTo(35)
    }
  }
}