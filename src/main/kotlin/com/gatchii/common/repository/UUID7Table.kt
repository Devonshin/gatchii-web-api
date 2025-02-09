package com.gatchii.common.repository

import com.github.f4b6a3.uuid.UuidCreator
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table.Dual.clientDefault
import java.util.*

open class UUID7Table(name: String = "", columnName: String = "id") : IdTable<UUID>(name) {
    /** The identity column of this [UUIDTable], for storing UUIDs wrapped as [EntityID] instances. */
    final override val id: Column<EntityID<UUID>> = uuid(columnName).autoGenerate7().entityId()
    override val primaryKey = PrimaryKey(id)
}

fun Column<UUID>.autoGenerate7(): Column<UUID> = clientDefault { UuidCreator.getTimeOrderedEpoch() }
