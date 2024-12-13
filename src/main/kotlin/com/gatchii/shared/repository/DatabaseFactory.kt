package com.gatchii.shared.repository

import javax.sql.DataSource

/**
 * Package: com.gatchii.shared.repository
 * Created: Devonshin
 * Date: 17/09/2024
 */

interface DatabaseFactory {
    fun connect()
    fun close()
    fun dataSource(): DataSource
}