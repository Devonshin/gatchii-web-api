package com.gatchii.domain.login

import com.gatchii.common.const.Constants.Companion.SUCCESS
import com.gatchii.plugins.JwtResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

/** Package: com.gatchii.domains.login Created: Devonshin Date: 23/09/2024 */

fun Route.loginRoute(
  loginService: LoginService
) {

  val logger: Logger = KtorSimpleLogger(this::class.simpleName ?: "LoginRoute")

  post<LoginUserRequest> ("/attempt") {
    val receive = call.receive<LoginUserRequest>()
    try {
      val result = loginService.loginProcess(receive)
      // 민감 정보(토큰) 로그 노출 방지
      logger.info("Attempt authenticate success for user: ${receive.prefixId}:${receive.suffixId}")
      // Ktor Serialization 사용: 객체로 응답
      call.respond(
        status = HttpStatusCode.OK,
        message = JwtResponse(
          message = SUCCESS,
          code = HttpStatusCode.OK.value,
          jwt = result!!
        )
      )
    } catch (e: Throwable) {
      // 일관된 401 응답 (StatusPages가 매핑되어 있더라도 안전하게 처리)
      logger.warn("Attempt authenticate failed for user: ${receive.prefixId}:${receive.suffixId}")
      call.respond(
        status = HttpStatusCode.Unauthorized,
        message = mapOf(
          "message" to "Unauthorized",
          "code" to HttpStatusCode.Unauthorized.value
        )
      )
    }
  }

  authenticate("auth-jwt") {
    get("/logout") {

    }
  }

}