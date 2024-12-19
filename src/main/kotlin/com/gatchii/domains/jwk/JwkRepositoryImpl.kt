package com.gatchii.domains.jwk

import com.gatchii.domains.jwk.JwkTable.deletedAt
import com.gatchii.domains.jwk.JwkTable.id
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll

/** Package: com.gatchii.domains.jwk Created: Devonshin Date: 16/09/2024 */

class JwkRepositoryImpl(override val table: JwkTable) : JwkRepository {
    override fun getUsableOne(idx: Int): JwkModel? = dbQuery {
        val results = table.selectAll()
            .where { deletedAt.isNull() }
            .limit(20)
            .orderBy(id to SortOrder.DESC)
            .toList()
        if (results.isEmpty()) return@dbQuery null
        toDomain(results[idx])
    }
}