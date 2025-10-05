package com.gatchii

import com.gatchii.common.task.TaskLeadHandler
import com.gatchii.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

fun onApplicationLoaded() {
  TaskLeadHandler.runTasks()
}

fun Application.module() {
  val env = environment.config.propertyOrNull("ktor.environment")?.getString()
  println("Application started in $env environment")
  configureDatabases()
  configureFrameworks()
  configureStatusPages()
  //configureHTTP()
  if (env != "test") { //테스트 환경에서는 route에 주입되는 서비스를 분기하기 위해
    println("Configure routing...")
    configureSecurity()
    configureRouting()
  }
  configureMonitoring()
  configureSerialization()
  configureValidation()
  onApplicationLoaded()
}
