package com.gatchii.shared.repository

import com.gatchii.shared.model.BaseModel

/**
 * Package: com.gatchii.shared.repository
 * Created: Devonshin
 * Date: 16/09/2024
 */

interface CrudRepository<D : BaseModel<T>, T> {
    suspend fun create(domain: D): D
    suspend fun batchCreate(
        domains: List<D>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true
    ): List<D>
    suspend fun findAll(): List<D>
    suspend fun read(id: T): D?
    suspend fun update(domain: D): D
    suspend fun delete(domain: D)
    suspend fun delete(id: T)
}