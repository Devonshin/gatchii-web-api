/**
 * @author Devonshin
 * @date 2025-10-09
 */
package shared

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.gatchii.common.const.Constants.Companion.USER_UID
import com.gatchii.domain.jwk.JwkService
import com.gatchii.plugins.ErrorResponse
import com.gatchii.plugins.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.logging.*
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.time.OffsetDateTime

private val logger: Logger = KtorSimpleLogger("shared.TestSecurityConfig")

/**
 * Test-specific Security configuration that retrieves JWK from database
 * instead of using HTTP-based JwkProvider
 */
fun Application.configureTestSecurity(jwkService: JwkService) {

  if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
    Security.addProvider(BouncyCastleProvider())
  }

  val env = environment.config

  install(Authentication) {
    jwt("auth-jwt") {
      testSecuritySetup(
        "auth-jwt",
        this@jwt,
        JwtConfig(
          audience = env.config("jwt").property("audience").getString(),
          issuer = env.config("jwt").property("issuer").getString(),
          realm = env.config("jwt").property("realm").getString(),
          jwkIssuer = env.config("jwt").property("jwkIssuer").getString(),
          expireSec = env.config("jwt").property("expireSec").getString().toLong()
        ),
        jwkService
      )
    }
    jwt("refresh-jwt") {
      testSecuritySetup(
        "refresh-jwt",
        this@jwt,
        JwtConfig(
          audience = env.config("rfrst").property("audience").getString(),
          issuer = env.config("rfrst").property("issuer").getString(),
          realm = env.config("rfrst").property("realm").getString(),
          jwkIssuer = env.config("rfrst").property("jwkIssuer").getString(),
          expireSec = env.config("rfrst").property("expireSec").getString().toLong()
        ),
        jwkService
      )
    }
  }
  logger.info("Test Security installed with DB-based JWK provider")
}

/**
 * Custom JwkProvider that reads from database instead of HTTP endpoint
 */
class DatabaseJwkProvider(private val jwkService: JwkService) : JwkProvider {
  override fun get(keyId: String?): Jwk {
    return runBlocking {
      val jwkList = jwkService.findAllJwk()
      val jwkMap = if (keyId != null) {
        jwkList.find { it["kid"] == keyId } ?: jwkList.firstOrNull()
      } else {
        jwkList.firstOrNull()
      } ?: throw IllegalStateException("No JWK found in database")
      
      Jwk.fromValues(jwkMap)
    }
  }
}

fun testSecuritySetup(
  name: String,
  config: JWTAuthenticationProvider.Config,
  jwtConfig: JwtConfig,
  jwkService: JwkService
) {
  val jwtAudience = jwtConfig.audience
  val jwtRealm = jwtConfig.realm
  val jwtIssuer = jwtConfig.issuer
  val authFailureKey = AttributeKey<String>("authFailure")
  
  // Use database-based JWK provider instead of HTTP-based
  val jwkProvider = DatabaseJwkProvider(jwkService)
  
  logger.info("$name JWT Config: realm=$jwtRealm, issuer=$jwtIssuer, audience=$jwtAudience (DB-based JWK)")
  
  config.realm = jwtRealm
  config.verifier(jwkProvider, jwtIssuer) {
    acceptLeeway(30)
    withAudience(jwtAudience)
    withIssuer(jwtIssuer)
  }

  config.validate { credential ->
    val payload = credential.payload
    logger.info(
      " is valid ?  = ${payload.expiresAt} : " +
          "\npayload.expiresAt?.time : ${payload.expiresAt?.time?.div(1000)}" +
          "\nOffsetDateTime.now().toEpochSecond() : ${OffsetDateTime.now().toEpochSecond()}" +
          "\nminusTime : ${payload.expiresAt?.time?.minus(OffsetDateTime.now().toEpochSecond() * 1000)}"
    )

    if (payload.expiresAt?.time?.minus(OffsetDateTime.now().toEpochSecond() * 1000).let { it!! < 0 }) {
      attributes.put(authFailureKey, "Token has expired")
      logger.info(" is valid ? = Token has expired")
      return@validate null
    }
    val userUid = payload.getClaim(USER_UID).asString()
    if (userUid == null) {
      attributes.put(authFailureKey, "userUid is null")
      logger.info(" is valid ? = userUid is null")
      return@validate null
    }

    userUid.takeIf { it.isNotBlank() }?.let { JWTPrincipal(credential.payload) }
  }

  config.challenge { defaultScheme, realm ->
    val failureReason = call.attributes.getOrNull(authFailureKey)
    logger.error("$name : Invalid or expired token failureReason=[$failureReason], realm=$realm, issuer=$jwtIssuer")
    call.respond(
      HttpStatusCode.Unauthorized,
      ErrorResponse(
        message = failureReason ?: "Token is not valid or has expired",
        code = HttpStatusCode.Unauthorized.value,
        path = call.request.uri
      )
    )
  }
}
