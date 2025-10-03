package shared.fixture.aaa

/**
 * @author Devonshin
 * @date 2025-10-03
 */

/**
 * AAA(Arrange-Act-Assert) 패턴을 표준화하기 위한 베이스 클래스.
 * - arrange: 테스트 준비 단계
 * - act: 실행 단계
 * - asserting: 검증 단계
 * - givenWhenThen: 세 단계를 한 번에 기술할 때 사용
 */
abstract class BaseAaaTest {

    protected inline fun <T> arrange(block: () -> T): T = block()

    protected inline fun <S, R> act(subject: S, crossinline action: (S) -> R): R = action(subject)

    protected inline fun asserting(block: () -> Unit) { block() }

    protected inline fun <A, R> givenWhenThen(
        crossinline arrange: () -> A,
        crossinline act: (A) -> R,
        crossinline assert: (A, R) -> Unit,
    ) {
        val a = arrange()
        val r = act(a)
        assert(a, r)
    }
}