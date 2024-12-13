package com.gatchii.domains.jwt

import com.gatchii.shared.model.BaseModel
import com.gatchii.shared.repository.UUID7Table
import com.gatchii.shared.serializer.OffsetDateTimeSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 14/11/2024
 */
//todo 주기적으로 디비 정리 필요
object RefreshTokenTable : UUID7Table(
    name = "jwt_refresh_tokens",
) {
    val isValid = bool("is_valid")
    val userId = uuid("user_id")
    val expireAt = timestampWithTimeZone("expire_at")
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    init {
        index(
            columns = arrayOf(
                userId
            ),
            indexType = "HASH",
            isUnique = false,
        )
    }
}

data class RefreshTokenModel(
    val isValid: Boolean,
    val userId: UUID,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val expireAt: OffsetDateTime? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null,
    override var id: UUID? = null
) : BaseModel<UUID>