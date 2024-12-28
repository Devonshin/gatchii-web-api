package com.gatchii.domains.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPrivateKey
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPublicKey
import io.ktor.util.logging.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*
import kotlin.random.Random

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 16/09/2024 */

class JwkServiceImpl(
    private val jwkRepository: JwkRepository,
) : JwkService {

    private val logger = KtorSimpleLogger("com.gatchii.domains.jwk.JwkServiceImpl")

    override suspend fun findRandomJwk(): JwkModel {
        val jwk = (jwkRepository.getUsableOne(Random.nextInt(0, 20))
            ?: throw NoSuchElementException("No usable jwk found."))
        return jwk
    }

    override suspend fun findJwk(id: UUID): JwkModel {
        val jwk = (jwkRepository.read(id))
            ?: throw NoSuchElementException("No usable jwk found.")
        return jwk
    }

    override suspend fun convertAlgorithm(jwk: JwkModel): Algorithm {
        val algorithm = Algorithm.ECDSA256(getProvider(jwk))
        return algorithm
    }

    override suspend fun getProvider(jwk: JwkModel): ECDSAKeyProvider {
        return object : ECDSAKeyProvider {
            override fun getPrivateKey(): ECPrivateKey {
                return convertPrivateKey(jwk.privateKey) as ECPrivateKey
            }

            override fun getPublicKeyById(keyId: String?): ECPublicKey {
                return convertPublicKey(jwk.publicKey) as ECPublicKey
            }

            override fun getPrivateKeyId(): String {
                return jwk.id.toString()
            }
        }
    }
}

