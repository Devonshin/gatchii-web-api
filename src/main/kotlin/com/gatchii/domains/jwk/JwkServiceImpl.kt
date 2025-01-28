package com.gatchii.domains.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPrivateKey
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPublicKey
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.ktor.util.logging.*
import kotlinx.serialization.json.Json
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
            ?: throw NoSuchElementException("Not found usable jwks."))
        return jwk
    }

    override suspend fun findJwk(id: UUID): JwkModel {
        val jwk = (jwkRepository.read(id))
            ?: throw NoSuchElementException("Not found jwk. [$id]")
        return jwk
    }

    override suspend fun convertAlgorithm(provider: ECDSAKeyProvider): Algorithm {
        val algorithm = Algorithm.ECDSA256(provider)
        return algorithm
    }

    //jwk keyid 값을 설정하기 위해
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

    suspend fun getJwk(provider: ECDSAKeyProvider): ECKey {
        // Create ECKey using Nimbus JOSE + JWT
        val algo = convertAlgorithm(provider)
        val signingKeyId = algo.signingKeyId
        val ecKey = ECKey.Builder(Curve.P_256, provider.getPublicKeyById(signingKeyId))
            .privateKey(provider.privateKey)
            .keyID(signingKeyId)
            .algorithm(com.nimbusds.jose.Algorithm("ES256"))
            .build()
        return ecKey // Return public key representation
    }

    override suspend fun findAllJwk(): Set<Map<String, String>> {
        val jwkSet = mutableSetOf<Map<String, String>>()

        for (model in jwkRepository.findAll()) {
            if(model.deletedAt != null) continue
            val provider = getProvider(model)
            val jwk = getJwk(provider)
            jwkSet.add(Json.decodeFromString<Map<String, String>>(jwk.toJSONString()))
        }

        return jwkSet
    }

    override suspend fun deleteJwk(id: UUID): JwkModel {
        TODO("Not yet implemented")
    }

    override suspend fun createJwk(domain: JwkModel): JwkModel {
        TODO("Not yet implemented")
    }

}

