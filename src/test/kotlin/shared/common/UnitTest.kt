package shared.common

import org.junit.jupiter.api.Tag

/**
 * Package: shared.common
 * Created: Devonshin
 * Date: 10/11/2024
 */

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("unitTest")
annotation class UnitTest()
