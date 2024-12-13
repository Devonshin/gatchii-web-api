package com.gatchii.domains.user

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

/**
 * Package: com.gatchii.domains.user
 * Created: Devonshin
 * Date: 26/09/2024
 */

object UserTable: UUIDTable("user") {

}

data class UserDetail(
    val id: UUID?,
    val name: String,
    val email: String,
    val emailSuffixIdx: UInt,
    val status: UserStatus
)

enum class UserStatus(val value: String) {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DELETED("DELETED")
}
