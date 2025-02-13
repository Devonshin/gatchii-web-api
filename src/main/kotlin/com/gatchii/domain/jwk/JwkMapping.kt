package com.gatchii.domain.jwk

import com.gatchii.common.model.BaseModel
import com.gatchii.common.repository.UUID7Table
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.jwk
 * Created: Devonshin
 * Date: 16/09/2024
 */

object JwkTable : UUID7Table(
    name = "jwks",
) {
    val privateKey = varchar("private_key", length = 255)
    val publicKey = text("public_key")
    val status = varchar("status", length = 10).nullable()
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val deletedAt: Column<OffsetDateTime?> = timestampWithTimeZone("deleted_at").nullable()
}

data class JwkModel(
    val privateKey: String,
    val publicKey: String,
    val status: JwkStatus = JwkStatus.ACTIVE,
    val createdAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
    override var id: UUID? = null,
) : BaseModel<UUID> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JwkModel) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

enum class JwkStatus {
    ACTIVE,
    INACTIVE,
    DELETED;

    companion object {
        fun fromValue(value: String?): JwkStatus {
            if (value == null) return INACTIVE
            return JwkStatus.valueOf(value.uppercase())
        }
    }
}