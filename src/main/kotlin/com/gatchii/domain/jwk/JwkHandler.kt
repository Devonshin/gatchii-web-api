package com.gatchii.domain.jwk

import com.gatchii.common.exception.InvalidUsableJwkStatusException
import com.gatchii.common.utils.DateUtil
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.util.logging.*
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.collections.ArrayDeque
import kotlin.random.Random
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Package: com.gatchii.domain.jwk
 * Created: Devonshin
 * Date: 30/01/2025
 */

class JwkHandler(
  private var jwkConfig: Config = ConfigFactory.load()
) {
  private val logger: Logger = KtorSimpleLogger("com.gatchii.domain.jwk.JwkHandler")
  private val lock = ReentrantReadWriteLock()
  
  // 설정값
  private var maxCapacity = getConfigValueInt("maxCapacity") ?: 30
  private var expireTimeSec = getConfigValueLong("expireTimeSec") ?: (365 * 24 * 60 * 60).toLong() // 1 year
  
  // 단일 저장소: 모든 JWK(ACTIVE/INACTIVE/DELETED)를 하나의 리스트에서 관리
  private val jwkStore: MutableList<JwkModel> = mutableListOf()

  companion object {
    // 싱글톤 인스턴스
    private var instance: JwkHandler = JwkHandler()
    
    fun getInstance(): JwkHandler = instance
    
    // 테스트용: 새로운 인스턴스로 초기화
    fun resetForTest(config: Config = ConfigFactory.load()): JwkHandler {
      instance = JwkHandler(config)
      return instance
    }
  }

  fun printConfig() {
    logger.info("maxCapacity = $maxCapacity, expireTimeSec = ${DateUtil.toReaderbleTimeFromSeconds(expireTimeSec)}")
  }

  // application*.conf 를 override 한다
  fun setConfig(config: Config) {
    lock.writeLock().lock()
    try {
      jwkConfig = config
      maxCapacity = getConfigValueInt("maxCapacity") ?: maxCapacity
      expireTimeSec = getConfigValueLong("expireTimeSec") ?: expireTimeSec
    } finally {
      lock.writeLock().unlock()
    }
  }

  private fun getConfigValueInt(key: String): Int? {
    return if (jwkConfig.hasPath(key)) jwkConfig.getInt(key) else null
  }
  
  private fun getConfigValueLong(key: String): Long? {
    return if (jwkConfig.hasPath(key)) jwkConfig.getLong(key) else null
  }
  
  fun getConfigValue(key: String): String? {
    return if (jwkConfig.hasPath(key)) jwkConfig.getString(key) else null
  }

  // 모든 JWK 저장소 초기화
  fun clearAll() {
    lock.writeLock().lock()
    try {
      jwkStore.clear()
      logger.info("Cleared all JWKs")
    } finally {
      lock.writeLock().unlock()
    }
  }

  // 새로운 ACTIVE JWK 추가
  fun addJwk(jwk: JwkModel) {
    require(jwk.status == JwkStatus.ACTIVE) { "Only ACTIVE JWK can be added to store" }
    
    lock.writeLock().lock()
    try {
      // ACTIVE JWK 용량 관리
      val activeCount = jwkStore.count { it.status == JwkStatus.ACTIVE }
      logger.debug("Before addJwk: activeCount=$activeCount, maxCapacity=$maxCapacity")
      
      if (activeCount >= maxCapacity) {
        // 가장 오래된 ACTIVE → INACTIVE로 전환
        val oldest = jwkStore
          .filter { it.status == JwkStatus.ACTIVE }
          .minByOrNull { it.createdAt ?: OffsetDateTime.now() }
        
        if (oldest != null) {
          val index = jwkStore.indexOf(oldest)
          logger.debug("Converting oldest ACTIVE to INACTIVE: ${oldest.id}")
          jwkStore[index] = oldest.copy(status = JwkStatus.INACTIVE)
        }
      }
      
      // 맨 앞에 추가
      jwkStore.add(0, jwk)
      val newActiveCount = jwkStore.count { it.status == JwkStatus.ACTIVE }
      logger.debug("After addJwk: activeCount=$newActiveCount, totalCount=${jwkStore.size}, maxCapacity=$maxCapacity, addJwk: ${jwk.id}")
    } finally {
      lock.writeLock().unlock()
    }
  }

  // 비활성 JWK 추가 (기존 발행 JWT 검증용)
  fun addInactiveJwk(jwk: JwkModel) {
    require(jwk.status != JwkStatus.DELETED) { "DELETED 상태의 JWK는 추가할 수 없음" }
    lock.writeLock().lock()
    try {
      val inactiveJwk = jwk.copy(status = JwkStatus.INACTIVE)
      // 이미 존재하는지 확인
      val exists = jwkStore.any { it.id == inactiveJwk.id }
      if (!exists) {
        jwkStore.add(inactiveJwk)
        logger.info("Added inactive JWK: ${inactiveJwk.id}")
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

  // 삭제 대상 JWK 추가 (이후 목록에서 완전히 제거됨)
  fun addDiscardJwk(jwk: JwkModel) {
    val discardJwk = jwk.copy(status = JwkStatus.DELETED, deletedAt = OffsetDateTime.now())
    // 저장소에서 제거
    jwkStore.removeIf { it.id == discardJwk.id }
    logger.info("Discarded JWK: ${discardJwk.id}")
  }

  // 모든 JWK 반환 (ACTIVE + INACTIVE, DELETED는 제외)
  fun getJwks(): List<JwkModel> {
    lock.readLock().lock()
    try {
      return jwkStore.filter { it.status != JwkStatus.DELETED }.toList()
    } finally {
      lock.readLock().unlock()
    }
  }

  // ACTIVE 상태의 JWK만 반환
  fun getActiveJwks(): List<JwkModel> {
    lock.readLock().lock()
    try {
      return jwkStore.filter { it.status == JwkStatus.ACTIVE }.toList()
    } finally {
      lock.readLock().unlock()
    }
  }

  // INACTIVE 상태의 JWK만 반환
  fun getInactiveJwks(): List<JwkModel> {
    lock.readLock().lock()
    try {
      return jwkStore.filter { it.status == JwkStatus.INACTIVE }.toList()
    } finally {
      lock.readLock().unlock()
    }
  }

  // JWT 생성용 랜덤 ACTIVE JWK 반환
  fun getRandomActiveJwk(): Optional<JwkModel> {
    lock.readLock().lock()
    try {
      val activeJwks = jwkStore.filter { it.status == JwkStatus.ACTIVE }
      return if (activeJwks.isEmpty()) Optional.empty<JwkModel>()
      else Optional.of<JwkModel>(activeJwks[Random.nextInt(0, activeJwks.size)])
    } finally {
      lock.readLock().unlock()
    }
  }

  // 삭제된 JWK 목록 반환
  fun getDiscardJwks(): List<JwkModel> {
    lock.readLock().lock()
    try {
      return jwkStore.filter { it.status == JwkStatus.DELETED }.toList()
    } finally {
      lock.readLock().unlock()
    }
  }

  // 만료된 INACTIVE JWK 추출 (expireTimeSec 이상 경과)
  fun getRemovalJwks(): List<JwkModel> {
    val expireDate = DateUtil.getCurrentDate().minusSeconds(expireTimeSec)
    logger.debug("ExpireDate = $expireDate, expireTimeSec = ${DateUtil.toReaderbleTimeFromSeconds(expireTimeSec)}")
    lock.readLock().lock()
    val removalJwks = try {
      jwkStore.filter {
        it.status == JwkStatus.INACTIVE && it.createdAt != null && it.createdAt.isBefore(expireDate)
      }.toList()
    } finally {
      lock.readLock().unlock()
    }
    removeJwks(removalJwks)
    return removalJwks
  }

  // JWK 목록 제거 (저장소에서 완전히 삭제)
  fun removeJwks(deleteJwks: List<JwkModel>) {
    if (deleteJwks.isEmpty()) return
    lock.writeLock().lock()
    try {
      // 삭제할 ID 리스트 생성
      val idsToRemove = deleteJwks.map { it.id }.toSet()
      // 저장소에서 제거 (복사본으로 안전하게 처리)
      val beforeSize = jwkStore.size
      jwkStore.removeAll { it.id in idsToRemove }
      val removedCount = beforeSize - jwkStore.size
      logger.debug("Removed $removedCount JWK(s), remaining: ${jwkStore.size}")
    } finally {
      lock.writeLock().unlock()
    }
  }

  fun jwkMaxCapacity(): Int {
    return maxCapacity
  }
}
