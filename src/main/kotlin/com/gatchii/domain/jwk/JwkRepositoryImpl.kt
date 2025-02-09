package com.gatchii.domain.jwk

import com.gatchii.domain.jwk.JwkTable.createdAt
import com.gatchii.domain.jwk.JwkTable.deletedAt
import com.gatchii.domain.jwk.JwkTable.id
import com.gatchii.domain.jwk.JwkTable.privateKey
import com.gatchii.domain.jwk.JwkTable.publicKey
import com.gatchii.common.model.ResultData
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import java.util.*

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

    override fun getAllUsable(lastId: UUID?, forward: Boolean, limit: Int, withDeleted: Boolean): ResultData<JwkModel> = dbQuery {
        val query = table.select(
            id, publicKey, privateKey, createdAt, deletedAt
        )

        applyWhereConditions(query, lastId, forward, withDeleted)

        val maxListSize = limit + 1
        val maxResult = query.orderBy(id to SortOrder.DESC)
            .limit(maxListSize)
            .toList()
        val maxResultSize = maxResult.size
        val finalResultSize = limit.coerceAtMost(maxResultSize)
        val results = List(finalResultSize) { toDomain(maxResult[it]) }

        ResultData(
            datas = results,
            hasMoreData = maxListSize == maxResultSize
        )
    }


    private fun applyWhereConditions(query: Query, lastId: UUID?, forward: Boolean, withDeleted: Boolean) {
        if (!withDeleted) {
            query.where { deletedAt.isNull() }
        }
        if (lastId != null) {
            query.andWhere {
                if (forward) id less lastId else id greater lastId
            }
        }
    }

}