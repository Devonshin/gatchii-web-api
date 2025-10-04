package com.gatchii.domain.login

import com.gatchii.common.model.BaseModel
import com.gatchii.common.repository.UUID7Table
import com.gatchii.common.serializer.OffsetDateTimeSerializer
import com.gatchii.common.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.NotNull
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.login Created: Devonshin Date: 23/09/2024 */

object LoginTable : UUID7Table(name = "login_users") {
    val prefixId: Column<String> = varchar("prefix_id", length = 50)
    val suffixId: Column<String> = varchar("suffix_id", length = 50)
    val password: Column<String> = varchar("password", length = 255)
    val rsaUid: Column<UUID> = uuid("rsa_uid")
    val status: Column<LoginStatus> = enumerationByName(name = "status", klass = LoginStatus::class, length = 10)
    val role: Column<UserRole> = enumerationByName(name = "role", klass = UserRole::class, length = 10)
    val lastLoginAt: Column<OffsetDateTime> =
        timestampWithTimeZone(name = "last_login_at").clientDefault { OffsetDateTime.now() }
    val deletedAt: Column<OffsetDateTime?> =
        timestampWithTimeZone(name = "deleted_at").nullable()

    init {
        index(
            columns = arrayOf(
                prefixId, suffixId
            ),
            indexType = "BTREE",
            isUnique = true,
        )
    }
}

@Serializable
data class LoginModel(
    val prefixId: String,
    val suffixId: String,
    val password: String,
    @Serializable(with = UUIDSerializer::class)
    val rsaUid: UUID,
    val status: LoginStatus,
    val role: UserRole,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val lastLoginAt: OffsetDateTime,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val deletedAt: OffsetDateTime? = null,
    @Serializable(with = UUIDSerializer::class)
    override var id: UUID? = null,
) : BaseModel<UUID>

@Serializable
data class LoginUserRequest(
    val prefixId: String,
    val suffixId: String,
    val password: String,
)

enum class LoginStatus(val value: String) {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DELETED("DELETED"),
}

enum class UserRole(val value: String) {
    USER("USER"),
    PROFESSIONAL("PRO"),
    ADMIN("ADMIN"),
    DELETED("DELETED"),
}
