package shared.fixture.builder

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.common.utils.ECKeyPairHandler
import com.gatchii.plugins.JwtConfig
import com.gatchii.utils.JwtHandler
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*

/**
 * @author Devonshin
 * @date 2025-10-03
 */
class JwtTestBuilder {
  private var audience: String = "TestAudience"
  private var issuer: String = "TestIssuer"
  private var expireSec: Long = 60
  private var jwtId: String? = UUID.randomUUID().toString()
  private val claims: MutableMap<String, Any> = mutableMapOf(
    "username" to "tester",
    "role" to "user"
  )
  private var algorithm: Algorithm = run {
    val kp = ECKeyPairHandler.generateKeyPair()
    Algorithm.ECDSA256(kp.public as ECPublicKey, kp.private as ECPrivateKey)
  }

  fun withAudience(value: String) = apply { this.audience = value }
  fun withIssuer(value: String) = apply { this.issuer = value }
  fun withExpireSec(value: Long) = apply { this.expireSec = value }
  fun withJwtId(value: String?) = apply { this.jwtId = value }
  fun withAlgorithm(value: Algorithm) = apply { this.algorithm = value }
  fun withClaim(key: String, value: Any) = apply { this.claims[key] = value }
  fun withClaims(map: Map<String, Any>) = apply { this.claims.putAll(map) }

  fun buildConfig(): JwtConfig = JwtConfig(
    audience = audience,
    issuer = issuer,
    expireSec = expireSec,
  )

  /**
   * 내부 Algorithm 확인용(테스트 전용).
   */
  fun algorithm(): Algorithm = this.algorithm

  /**
   * JWT 토큰 문자열 생성.
   */
  fun buildToken(): String = JwtHandler.generate(
    jwtId = jwtId,
    claim = claims,
    algorithm = algorithm,
    jwtConfig = buildConfig(),
  )
}