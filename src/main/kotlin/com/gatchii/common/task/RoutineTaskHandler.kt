package com.gatchii.common.task

import com.gatchii.utils.DateUtil
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/** Package: com.gatchii.shared.common Created: Devonshin Date: 01/12/2024 */

data class RoutineScheduleExpression(
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
)

class RoutineTaskHandler(
    taskName: String,
    private val scheduleExpression: RoutineScheduleExpression,
    private val task: () -> Unit,
    private val period: Long = 24 * 60 * 60L,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default), // 기본값 설정
) : TaskLeadHandler(taskName) {
    private val logger = KtorSimpleLogger(this::class.simpleName ?: "RoutineTaskHandler")
    private val oneDaySec: Long = 24 * 60 * 60L
    var delayTimeSec = 0L
    var isExecuteImmediate = false;
    val isTest = taskName.startsWith("test")

    init {
        logger.info("ZoneId.systemDefault : ${ZoneId.systemDefault()}")
    }

    override fun startTask() {
        this.job = scope.launch {
            while (isActive) jobProcessing()
        }
    }

    suspend fun jobProcessing() {
        val currentTime = getTime(taskName, delayTimeSec.toLong())
        val currentTimeSec = currentTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000
        val scheduleTime = getScheduleTime(currentTime)
        val scheduleTimeSec = scheduleTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000
        logger.info("ScheduleTime : ${scheduleTime}, CurrentTime : ${currentTime} = ${DateUtil.toReaderbleTimeFromSeconds(abs(scheduleTimeSec - currentTimeSec))}")
        if (abs(currentTimeSec - scheduleTimeSec) < 30) { // 설정 시간이 현재 시간 이전이면 바로 작업 수행, 30초 차이는 용인
            task()
            logger.info("Task execute. currentTimeSec - scheduleTimeSec = ${currentTimeSec - scheduleTimeSec}")
            delayTimeSec = period
        } else {
            val remainSec = abs(scheduleTimeSec - currentTimeSec)  // 설정 시간까지 남은 시간 계산
            delayTimeSec = if (scheduleTimeSec < currentTimeSec) {
                if (!isExecuteImmediate) {
                    task()
                    isExecuteImmediate = true
                    logger.info("Task execute immediately.")
                }
                oneDaySec - remainSec // 1시 설정이고 현재 2시라면 24시간에서 1시간을 뺀 값을 다음 실행을 위해 대기: 23시간 후
            } else {
                remainSec //2시 설정이고 현재 1시라면 1시간 더 대기
            }
        }
        logger.info("Task remain : ${DateUtil.toReaderbleTimeFromSeconds(delayTimeSec)}")
        delay(delayTimeSec * 1000) // 남은 시간 추가 대기
    }

    private fun getScheduleTime(now: LocalDateTime): LocalDateTime = now
        .withHour(scheduleExpression.hour)
        .withMinute(scheduleExpression.minute)
        .withSecond(scheduleExpression.second)

    private fun getTime(taskName: String, plusSec: Long): LocalDateTime {
        val now = LocalDateTime.now()
        if (isTest) {
            logger.info("getTime for Test will return now plus ${DateUtil.toReaderbleTimeFromSeconds(plusSec)}")
            return LocalDateTime.now(DateUtil.applyTestDateCount("RoutineTaskHandler", plusSec * 1000L))
        }
        return now
    }

}