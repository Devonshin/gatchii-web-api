package com.gatchii

import com.gatchii.plugins.*
import com.gatchii.shared.common.TaskLeadHandler
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun onApplicationLoaded() {
    TaskLeadHandler.runTasks()
}

fun Application.module() {
    configureDatabases()
    configureFrameworks()
    configureStatusPages()
    configureSecurity()
    //configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRouting()
    onApplicationLoaded()
}
