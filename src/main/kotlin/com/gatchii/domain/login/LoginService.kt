package com.gatchii.domain.login

import com.gatchii.domain.jwt.JwtModel

/**
 * Package: com.gatchii.domains.login
 * Created: Devonshin
 * Date: 23/09/2024
 */

interface LoginService {

    suspend fun loginProcess(loginUserRequest: LoginUserRequest): JwtModel?

    suspend fun createLoginUser(loginUser: LoginModel): LoginModel

    suspend fun deleteLoginUser(loginUser: LoginModel)

    suspend fun updateLoginUser(loginUser: LoginModel)

    suspend fun attemptAuthenticate(loginUserRequest: LoginUserRequest): LoginModel?

    suspend fun loginSuccessAction(loginModel: LoginModel): JwtModel

    suspend fun loginFailAction(loginUserRequest: LoginUserRequest): Unit

}