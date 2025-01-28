package shared

import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.gatchii.domains.jwk.JwkResponse
import com.gatchii.domains.jwt.RefreshTokenRouteTest.Companion.logger
import com.gatchii.utils.ECKeyPairHandler.Companion.PRAM_SPEC
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

/** Package: shared Created: Devonshin Date: 25/12/2024 */

class TestJwkServer {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    //private lateinit var server: TestApplicationEngine
    private lateinit var server: NettyApplicationEngine
    val url: String
        get() {
            return "http://localhost:${server.environment.connectors[0].port}"
        }

    // Generate EC Key Pair
    fun generateKeyPair(): KeyPair {
        val ecSpec = ECNamedCurveTable.getParameterSpec(PRAM_SPEC) // Ensure BouncyCastle is used here
        if (ecSpec == null) {
            throw IllegalStateException("Curve secp256r1 is not supported")
        }
        val keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(ecSpec, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    val keyId = "secp256r1-key-${System.currentTimeMillis()}"
    fun getJwk(keyPair: KeyPair): ECKey {
        // Create ECKey using Nimbus JOSE + JWT
        val ecKey = ECKey.Builder(Curve.P_256, keyPair.public as ECPublicKey?)
            .privateKey(keyPair.private)
            .keyID(keyId)
            .algorithm(com.nimbusds.jose.Algorithm("ES256"))
            .build()
        return ecKey // Return public key representation
    }

    val keyPair = generateKeyPair()
    val jwkJson: String? = getJwk(keyPair).toJSONString()

    fun getGeneratedKeyPair(): KeyPair {
        return keyPair
    }

    fun getJwkProvider(): ECDSAKeyProvider {
        return object : ECDSAKeyProvider {
            override fun getPrivateKey(): ECPrivateKey {
                return keyPair.private as ECPrivateKey
            }

            override fun getPublicKeyById(keyId: String?): ECPublicKey {
                return keyPair.public as ECPublicKey
            }

            override fun getPrivateKeyId(): String {
                return keyId
            }
        }
    }

    val jwkList = setOf(Json.decodeFromString<Map<String, String>>(jwkJson!!))

    fun start() {
        /*server = TestApplicationEngine(createTestEnvironment {
            connector {
                host = "localhost"
                port = 8880 // test server port
            }
        }).apply {
            start(wait = true)
            val list = mutableSetOf<Map<String, String>>()
            list.add(Json.decodeFromString<Map<String, String>>(jwkJson))
            application.routing {
                get("/") {
                    // Example response for testing purposes
                    logger.info("jwk server root.. ")
                    call.respondText("Hello, world!", ContentType.Text.Plain)
                }
                get("/.well-known/jwks.json") {
                    // Example response for testing purposes
                    logger.info("well-known.jwks.json... ")
                    call.respond(HttpStatusCode.OK, JwkResponse(list))
                }
            }
        }*/
        server = embeddedServer(Netty, port = 8880) {
            routing {
                get("/") {
                    // Example response for testing purposes
                    logger.info("jwk server root.. ")
                    call.respondText("Hello, world!", ContentType.Text.Plain)
                }
                get("/.well-known/jwks.json") {
                    // Example response for testing purposes
                    logger.info("well-known.jwks.json...")
                    call.respondText(
                        Json.encodeToString(JwkResponse(jwkList)),
                        contentType = ContentType.Application.Json
                    )
                }
            }
        }
        server.start(wait = false)
        logger.debug("jwkServer.start().. {}", server.environment.connectors)
    }

    fun stop() {
        logger.debug("jwkServer.stop()..")
        server.stop(0, 0)
    }
}
