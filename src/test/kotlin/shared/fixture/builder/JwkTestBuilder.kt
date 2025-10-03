package shared.fixture.builder

import com.gatchii.domain.jwk.JwkModel
import io.ktor.util.encodeBase64
import java.time.OffsetDateTime
import java.util.UUID
import com.gatchii.common.utils.ECKeyPairHandler

/**
 * @author Devonshin
 * @date 2025-10-03
 */
class JwkTestBuilder {
    private var createdAt: OffsetDateTime = OffsetDateTime.now()
    private var deletedAt: OffsetDateTime? = null
    private var id: UUID? = null
    private var privateKeyBase64: String? = null
    private var publicKeyBase64: String? = null

    fun withCreatedAt(value: OffsetDateTime) = apply { this.createdAt = value }
    fun withDeletedAt(value: OffsetDateTime?) = apply { this.deletedAt = value }
    fun withId(value: UUID?) = apply { this.id = value }
    fun withPrivateKeyBase64(value: String) = apply { this.privateKeyBase64 = value }
    fun withPublicKeyBase64(value: String) = apply { this.publicKeyBase64 = value }

    /**
     * 기본은 새 키페어를 생성하여 base64로 저장.
     */
    fun build(): JwkModel {
        val priv = privateKeyBase64 ?: ECKeyPairHandler.generateKeyPair().private.encoded.encodeBase64()
        val pub = publicKeyBase64 ?: ECKeyPairHandler.generateKeyPair().public.encoded.encodeBase64()
        return JwkModel(
            privateKey = priv,
            publicKey = pub,
            createdAt = createdAt,
            deletedAt = deletedAt,
            id = id,
        )
    }
}