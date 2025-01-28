package com.gatchii.domains.jwk

import com.gatchii.shared.model.BaseModel
import com.gatchii.shared.repository.UUID7Table
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
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val deletedAt: Column<OffsetDateTime?> = timestampWithTimeZone("deleted_at").nullable()
}

data class JwkModel(
    val privateKey: String,
    val publicKey: String,
    val createdAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
    override var id: UUID? = null,
) : BaseModel<UUID>
