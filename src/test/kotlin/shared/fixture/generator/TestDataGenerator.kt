package shared.fixture.generator

import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * @author Devonshin
 * @date 2025-10-03
 */

/**
 * 랜덤 테스트 데이터 생성을 위한 유틸리티.
 * - 외부 라이브러리 의존 없이 SecureRandom 기반으로 동작합니다.
 * - 테스트에서 일관적 사용을 위해 기본값 규칙을 제공합니다.
 */
object TestDataGenerator {

    private val random = SecureRandom()
    private val alphaLower = ('a'..'z').toList()
    private val alphaUpper = ('A'..'Z').toList()
    private val digits = ('0'..'9').toList()
    private val safeSymbols = listOf('-', '_', '.')

    /**
     * 지정 길이의 영숫자 문자열을 생성합니다.
     */
    fun alphaNumeric(length: Int = 12): String {
        require(length > 0) { "length must be > 0" }
        val pool = alphaLower + alphaUpper + digits
        return buildString(length) { repeat(length) { append(pool[random.nextInt(pool.size)]) } }
    }

    /**
     * 지정 길이의 숫자만으로 구성된 문자열을 생성합니다.
     */
    fun numeric(length: Int = 6): String {
        require(length > 0) { "length must be > 0" }
        return buildString(length) { repeat(length) { append(digits[random.nextInt(digits.size)]) } }
    }

    /**
     * 비교적 안전한 패스워드 생성(영대/영소/숫자/심볼 혼합).
     */
    fun password(length: Int = 16): String {
        require(length >= 8) { "password length must be >= 8" }
        val pool = alphaLower + alphaUpper + digits + safeSymbols
        return buildString(length) { repeat(length) { append(pool[random.nextInt(pool.size)]) } }
    }

    /**
     * 간단한 이메일 주소 생성.
     */
    fun email(localLength: Int = 8, domain: String = "example.com"): String {
        val local = alphaNumeric(localLength).lowercase()
        return "$local@$domain"
    }

    /**
     * UUID v4 문자열 생성.
     */
    fun uuid(): String = UUID.randomUUID().toString()

    /**
     * 현재 시각 기준으로 +/- seconds 범위의 OffsetDateTime 생성.
     */
    fun randomOffsetDateTimeWithinSeconds(rangeSeconds: Long = 3600): OffsetDateTime {
        require(rangeSeconds > 0) { "rangeSeconds must be > 0" }
        val delta = random.nextLong(rangeSeconds * 2 + 1) - rangeSeconds
        return OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delta)
    }
}