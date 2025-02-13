package com.gatchii.domain.jwk

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.gatchii.common.task.TaskLeadHandler
import com.gatchii.config.GlobalConfig
import com.gatchii.utils.ECKeyPairHandler
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPrivateKey
import com.gatchii.utils.ECKeyPairHandler.Companion.convertPublicKey
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.ktor.util.*
import io.ktor.util.logging.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.time.OffsetDateTime
import java.util.*
import kotlin.jvm.optionals.getOrElse

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 16/09/2024 */

class JwkServiceImpl(
    private val jwkRepository: JwkRepository,
    private val taskHandlerProvider: (() -> Unit) -> TaskLeadHandler,
) : JwkService {

    private val logger = KtorSimpleLogger(this::class.simpleName ?: "JwkServiceImpl")
    lateinit var task: TaskLeadHandler

    init {
        val envVal = GlobalConfig.getConfigedValue("ktor.environment")
        logger.info("Application envVal : $envVal")
        if (!envVal.equals("test", ignoreCase = true)) {
            runBlocking {
                initializeJwk()
            }
        }
    }

    fun stopTask() {
        logger.info("stopTask called..")
        task.stopTask()
    }

    //jwk 1개 추가, active를 inactive로 변경, 제거
    override suspend fun taskProcessing() {
        val createdJwk = createJwk()
        JwkHandler.addJwk(createdJwk)
        JwkHandler.getRemovalJwks()
            .forEach { jwk ->
                logger.info("delete jwk : {}", jwk.id.toString())
                deleteJwk(jwk.id!!)
            }
    }

    //초기화
    override suspend fun initializeJwk() {
        val allUsableJwks: List<JwkModel> = findAllUsableJwk()
        logger.info("allUsableJwks size : ${allUsableJwks.size} ")
        if (allUsableJwks.isEmpty()) {
            val createdJwks = createJwks(JwkHandler.jwkMaxCapacity())
            for (model in createdJwks) {
                JwkHandler.addJwk(model)
            }
            logger.info("New create allUsableJwks size : ${allUsableJwks.size} ")
        } else {
            for (jwkModel in allUsableJwks) {
                JwkHandler.addJwk(jwkModel)
            }
        }
        task = taskHandlerProvider {
            logger.info("call JwkServiceImpl.taskProcessing.. ")
            runBlocking {
                taskProcessing()
            }
        }
        TaskLeadHandler.addTasks(task)
    }

    override suspend fun getRandomJwk(): JwkModel {
        return JwkHandler.getRandomActiveJwk().getOrElse {
            throw NoSuchElementException("Not found usable jwks.")
        }
    }

    override suspend fun findJwk(id: UUID): JwkModel {
        val jwk = jwkRepository.read(id)
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

    override suspend fun findAllJwk(): List<Map<String, String>> {
        val jwkSet = mutableListOf<Map<String, String>>()

        for (model in jwkRepository.findAll()) {
            if (model.deletedAt != null) continue
            val provider = getProvider(model)
            val jwk = getJwk(provider)
            jwkSet.add(Json.decodeFromString<Map<String, String>>(jwk.toJSONString()))
        }
        return jwkSet
    }

    override suspend fun findAllUsableJwk(): List<JwkModel> {
        return jwkRepository
            .getAllUsable(null, true, JwkHandler.jwkMaxCapacity(), false)
            .datas
    }

    override suspend fun deleteJwk(id: UUID) {
        jwkRepository.delete(id)
    }

    override suspend fun deleteJwks(jwks: List<JwkModel>) {
        jwks.filter { it.id != null }.forEach { deleteJwk(it.id!!) }
    }

    override suspend fun createJwk(): JwkModel {
        val generatedKeyPair = ECKeyPairHandler.generateKeyPair()
        return jwkRepository.create(
            JwkModel(
                privateKey = generatedKeyPair.private.encoded.encodeBase64(),
                publicKey = generatedKeyPair.public.encoded.encodeBase64(),
                createdAt = OffsetDateTime.now()
            )
        )
    }

    override suspend fun createJwks(size: Int): List<JwkModel> {
        val now = OffsetDateTime.now()
        val jwks = List<JwkModel>(size) {
            val generatedKeyPair = ECKeyPairHandler.generateKeyPair()
            JwkModel(
                privateKey = generatedKeyPair.private.encoded.encodeBase64(),
                publicKey = generatedKeyPair.public.encoded.encodeBase64(),
                createdAt = now
            )
        }
        return jwkRepository.batchCreate(jwks)
    }

}

