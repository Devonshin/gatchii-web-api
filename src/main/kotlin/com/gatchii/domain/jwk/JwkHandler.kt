package com.gatchii.domain.jwk

import com.gatchii.common.exception.InvalidUsableJwkStatusException
import com.gatchii.utils.DateUtil
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.util.logging.*
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.collections.ArrayDeque
import kotlin.random.Random

/**
 * Package: com.gatchii.domain.jwk
 * Created: Devonshin
 * Date: 30/01/2025
 */

class JwkHandler {
    companion object {
        private val logger: Logger = KtorSimpleLogger("com.gatchii.domain.jwk.JwkHandler")
        private var jwkConfig = ConfigFactory.load()
        private var maxCapacity = getConfigValue("maxCapacity")?.toInt() ?: 30
        private var expireTimeSec = getConfigValue("expireTimeSec")?.toLong() ?: (365 * 24 * 60 * 60).toLong() // 1 year
        private var activeJwks: ArrayDeque<JwkModel> = ArrayDeque(maxCapacity)
        private var inactiveJwks: MutableList<JwkModel> = mutableListOf()
        private var discardJwks: MutableList<JwkModel> = mutableListOf()

        //application*.conf 를 override 한다
        fun setConfig(config: Config) {
            jwkConfig = config
            maxCapacity = getConfigValue("maxCapacity")?.toInt() ?: maxCapacity
            expireTimeSec = getConfigValue("expireTimeSec")?.toLong() ?: expireTimeSec
            activeJwks = ArrayDeque(maxCapacity)
        }

        fun getConfigValue(key: String): String? {
            return if (jwkConfig.hasPath(key)) jwkConfig.getString(key) else null
        }

        fun clearAll() {
            activeJwks.clear()
            inactiveJwks.clear()
            discardJwks.clear()
        }

        fun addJwk(jwk: JwkModel) {
            if(jwk.status == JwkStatus.ACTIVE) {
                if (activeJwks.size >= maxCapacity) { //활성 jwk는 ${maxCapacity}개만 유지
                    addInactiveJwk(activeJwks.removeLast())
                }
                activeJwks.addFirst(jwk)
            } else if(jwk.status == JwkStatus.INACTIVE) {
                addInactiveJwk(jwk)
            } else {
                throw InvalidUsableJwkStatusException("Invalid JwkStatus")
            }
            logger.info("activeJwks.size = ${activeJwks.size}, inactiveJwks.size = ${inactiveJwks.size}, maxCapacity = $maxCapacity, addJwk: ${jwk.id}")
        }

        fun addInactiveJwk(jwk: JwkModel) { // 비활성 상태는 기존 발행한 jwt들의 검증용으로만 사용
            inactiveJwks.add(jwk.copy(status = JwkStatus.INACTIVE))
        }

        fun addDiscardJwk(jwk: JwkModel) { // 삭제 대상: 삭제하면 목록 비노출 및 검증용으로 사용 불가
            discardJwks.add(jwk.copy(status = JwkStatus.DELETED, deletedAt = OffsetDateTime.now()))
        }

        fun getJwks(): List<JwkModel> {
            return activeJwks.toList().plus(inactiveJwks)
        }

        fun getActiveJwks(): List<JwkModel> {
            return activeJwks.toList()
        }

        fun getInactiveJwks(): List<JwkModel> {
            return inactiveJwks.toList()
        }

        //jwt를 생성할 때는 반드시 이 함수로 jwk를 불러와야 한다.
        fun getRandomActiveJwk(): Optional<JwkModel> {
            return if (activeJwks.isEmpty()) Optional.empty<JwkModel>()
            else {
                val size = activeJwks.size
                Optional.of<JwkModel>(activeJwks[Random.nextInt(0, size)])
            }
        }

        /*
        * 삭제 jkw 목록
        * */
        fun getDiscardJwks(): List<JwkModel> {
            return discardJwks.toList()
        }
        /*
        * 비활성 jkw 중에서 {expireTimeSec}년 이상된 것들 추출
        * */
        fun getRemovalJwks(): List<JwkModel> {
            val expireDate =
                DateUtil.getCurrentDate().minusSeconds(expireTimeSec)
            logger.debug("ExpireDate = $expireDate,  expireTimeSec = ${DateUtil.formatSecondsToNaturalTime(expireTimeSec)}")
            return inactiveJwks.filter {
                it.createdAt!!.isBefore(expireDate)
            }.apply {
                removeJwks(this)
                discardJwks
            }
        }

        fun removeJwks(deleteJwks: List<JwkModel>) {
            deleteJwks.forEach { jwk ->
                activeJwks.removeIf { it.id == jwk.id }
                inactiveJwks.removeIf { it.id == jwk.id }
                addDiscardJwk(jwk)
                logger.debug("activeJwks.size = ${activeJwks.size}, inactiveJwks.size = ${inactiveJwks.size}, maxCapacity = $maxCapacity, addJwk: ${jwk.id}, removeJwk: ${jwk.id}")
            }
        }

        fun jwkMaxCapacity(): Int {
            return maxCapacity
        }
    }
}