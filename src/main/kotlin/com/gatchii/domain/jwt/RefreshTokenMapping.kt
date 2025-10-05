package com.gatchii.domain.jwt

import com.gatchii.common.model.BaseModel
import com.gatchii.common.repository.UUID7Table
import com.gatchii.common.serializer.OffsetDateTimeSerializer
import com.gatchii.common.serializer.UUIDSerializer
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
  val userUid = uuid("user_uid")
  val expireAt = timestampWithTimeZone("expire_at")
  val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
  val deletedAt = timestampWithTimeZone("deleted_at").nullable()

  init {
    index(
      columns = arrayOf(
        userUid
      ),
      indexType = "HASH",
      isUnique = false,
    )
  }
}

@Serializable
data class RefreshTokenModel(
  val isValid: Boolean,
  @Serializable(with = UUIDSerializer::class)
  val userUid: UUID,
  @Serializable(with = OffsetDateTimeSerializer::class)
  val expireAt: OffsetDateTime? = null,
  @Serializable(with = OffsetDateTimeSerializer::class)
  val createdAt: OffsetDateTime? = null,
  @Serializable(with = OffsetDateTimeSerializer::class)
  val deletedAt: OffsetDateTime? = null,
  @Serializable(with = UUIDSerializer::class)
  override var id: UUID? = null
) : BaseModel<UUID>
