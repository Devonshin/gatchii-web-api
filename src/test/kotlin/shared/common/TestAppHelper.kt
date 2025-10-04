package shared.common

import com.gatchii.plugins.configureSerialization
import com.gatchii.plugins.configureStatusPages
import com.gatchii.plugins.configureValidation
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.testing.*

/**
 * 공통 Ktor 테스트 애플리케이션 초기화 유틸리티
 * - Serialization → Validation → (StatusPages) → (DoubleReceive) → (Security)
 */
fun ApplicationTestBuilder.setupCommonApp(
    configResource: String = "application-test.conf",
    installStatusPages: Boolean = true,
    installSecurity: Boolean = false,
    installDoubleReceive: Boolean = false,
    securityInstall: (Application.() -> Unit)? = null,
) {
    environment {
        config = HoconApplicationConfig(ConfigFactory.load(configResource))
    }
    application {
        // 순서 중요: JSON → Validation → StatusPages → 추가 플러그인
        configureSerialization()
        configureValidation()
        if (installStatusPages) {
            configureStatusPages()
        }
        if (installDoubleReceive) {
            install(DoubleReceive)
        }
        if (installSecurity) {
            securityInstall?.invoke(this)
        }
    }
}