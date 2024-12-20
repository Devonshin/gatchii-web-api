package com.gatchii.domains.login

import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll

/**
 * Package: com.gatchii.domains.login
 * Created: Devonshin
 * Date: 24/09/2024
 */

class LoginRepositoryImpl(override val table: LoginTable): LoginRepository {

    override suspend fun findUser(prefixId: String, suffixId: String): LoginModel? = dbQuery {
        table.selectAll()
            .where {
                table.prefixId eq prefixId
            }.andWhere {
                table.suffixId eq suffixId
            }.limit(1)
            .map { toDomain(it) }
            .singleOrNull()
    }

}
