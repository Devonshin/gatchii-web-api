package com.gatchii.domain.rsa

import com.gatchii.common.model.BaseModel
import com.gatchii.common.repository.UUID7Table
import com.gatchii.common.serializer.OffsetDateTimeSerializer
import com.gatchii.common.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.*

/**
 * Package: com.gatchii.domains.rsa
 * Created: Devonshin
 * Date: 03/11/2024
 */

@Serializable
data class RsaModel(
    val publicKey: String,
    val privateKey: String,
    val exponent: String,
    val modulus: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val deletedAt: OffsetDateTime? = null,
    @Serializable(with = UUIDSerializer::class)
    override var id: UUID? = null,
) : BaseModel<UUID>


object RsaTable : UUID7Table(
    name = "rsa_keys",
) {
    val publicKey = text("public_key")
    val privateKey = text("private_key")
    val exponent = text("exponent")
    val modulus = text("modulus")
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
}
