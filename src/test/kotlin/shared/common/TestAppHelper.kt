package shared.common

import com.gatchii.plugins.configureSerialization
import com.gatchii.plugins.configureStatusPages
import com.gatchii.plugins.configureValidation
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.testing.*
import java.net.ServerSocket

/**
 * 테스트용 사용 가능한 포트를 찾습니다.
 * 포트 충돌을 피하기 위해 OS가 할당한 자유 포트를 사용합니다.
 */
fun findAvailablePort(): Int {
  return try {
    ServerSocket(0).use { it.localPort }
  } catch (e: Exception) {
    8880 // 기본값
  }
}

/**
 * 공통 Ktor 테스트 애플리케이션 초기화 유틸리티
 * - Serialization → Validation → (StatusPages) → (DoubleReceive) → (Security)
 * - 동적 포트 할당으로 포트 충돌 방지
 */
fun ApplicationTestBuilder.setupCommonApp(
  configResource: String = "application-test.conf",
  installStatusPages: Boolean = true,
  installSecurity: Boolean = false,
  installDoubleReceive: Boolean = false,
  securityInstall: (Application.() -> Unit)? = null,
  useRandomPort: Boolean = true,
) {
  environment {
    var baseConfig = ConfigFactory.load(configResource)
    
    // 포트를 동적으로 할당
    if (useRandomPort) {
      val port = findAvailablePort()
      baseConfig = baseConfig.withValue("ktor.deployment.port", ConfigValueFactory.fromAnyRef(port))
    }
    
    config = HoconApplicationConfig(baseConfig)
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