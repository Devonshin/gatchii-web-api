package com.gatchii.domain.login

import com.gatchii.common.const.Constants.Companion.EMPTY_STR
import com.gatchii.common.exception.NotFoundUserException
import com.gatchii.common.utils.BCryptPasswordEncoder
import com.gatchii.domain.jwt.JwtModel
import com.gatchii.domain.jwt.JwtService
import com.gatchii.domain.jwt.RefreshTokenService
import com.gatchii.domain.rsa.RsaService
import com.gatchii.utils.JwtHandler.Companion.newJwtModel
import io.ktor.util.logging.*

/** Package: com.gatchii.domains.login Created: Devonshin Date: 23/09/2024 */

class LoginServiceImpl(
  private val loginRepository: LoginRepository,
  private val bCryptPasswordEncoder: BCryptPasswordEncoder,
  private val jwtService: JwtService,
  private val refreshTockenService: RefreshTokenService,
  private val rsaService: RsaService,
) : LoginService {

  val logger: Logger = KtorSimpleLogger(this::class.simpleName ?: "LoginService")

  /**
   * Handles the login process by attempting authentication and performing
   * actions based on the outcome. If authentication is successful, it
   * generates JWT tokens for the user. Otherwise, it handles the failure
   * scenario.
   *
   * @param loginUserRequest The login request containing user credentials,
   *    including prefixId, suffixId, and password.
   * @return JwtModel containing access and refresh tokens if login is
   *    successful, or null if the credentials are invalid.
   */
  override suspend fun loginProcess(loginUserRequest: LoginUserRequest): JwtModel? {
    val loginModel = attemptAuthenticate(loginUserRequest)
      ?: return loginFailAction(loginUserRequest).let { null }
    logger.info("Login successful for user: $loginModel")
    return loginSuccessAction(loginModel)
  }

  /**
   * Attempts to authenticate a user based on the provided login request.
   *
   * @param loginUserRequest The login request containing user credentials.
   * @return The authenticated LoginModel if credentials match.
   * @throws NotFoundUserException if user cannot be found.
   */
  override suspend fun attemptAuthenticate(loginUserRequest: LoginUserRequest): LoginModel? {
    val loginModel =
      loginRepository.findUser(loginUserRequest.prefixId, loginUserRequest.suffixId)
    if (loginModel != null) {
      if (bCryptPasswordEncoder.matches(loginUserRequest.password, loginModel.password))
        return loginModel
    }
    return null
  }

  /**
   * Executes actions on successful login and generates JWT tokens for the
   * user.
   *
   * @param loginModel The model containing user login details.
   * @return JwtModel containing access and refresh tokens.
   */
  override suspend fun loginSuccessAction(loginModel: LoginModel): JwtModel {
    // B안: 로그인 성공 시 last_login_at 갱신
    val updated = loginModel.copy(lastLoginAt = java.time.OffsetDateTime.now())
    loginRepository.update(updated)

    val rsa = rsaService.getRsa(loginModel.rsaUid)
    // RefreshTokenService가 userUid를 UUID.fromString()으로 파싱하므로 암호화하지 않은 UUID 문자열 사용
    val claim: Map<String, String> = mapOf(
      "userUid" to loginModel.id.toString(),  // UUID 문자열 (암호화하지 않음)
      "userIdEncrypted" to rsaService.encrypt(rsa, loginModel.prefixId),  // 암호화된 userId
      "role" to loginModel.role.name,
    )
    val refreshClaim: Map<String, String> = mapOf(
      "userUid" to loginModel.id.toString()  // UUID 문자열 (암호화하지 않음)
    )
    val jwtConfig = jwtService.config()
    val refreshJwtConfig = refreshTockenService.config()
    return newJwtModel(
      accessToken = jwtService.generate(claim),
      jwtConfig = jwtConfig,
      refreshToken = refreshTockenService.generate(refreshClaim),
      refreshJwtConfig = refreshJwtConfig
    )
  }

  /** Handles actions to be performed on login failure. */
  override suspend fun loginFailAction(loginUserRequest: LoginUserRequest) {
    logger.error("Login failed for user: ${loginUserRequest.prefixId}:${loginUserRequest.suffixId}")
    throw NotFoundUserException("Not found user: ${loginUserRequest.prefixId}:${loginUserRequest.suffixId}")
  }

  /**
   * Creates a new login user in the repository.
   *
   * @param loginUser The model representing the user to be created.
   * @return The created LoginModel with assigned details.
   */
  override suspend fun createLoginUser(loginUser: LoginModel): LoginModel {
    val created = loginRepository.create(loginUser)
    return created
  }

  /**
   * Deletes a login user from the repository.
   *
   * @param loginUser The model representing the user to be deleted.
   * @throws NotFoundUserException if the user is not found in the
   *    repository.
   */
  override suspend fun deleteLoginUser(loginUser: LoginModel) {
    val loggedinUser = loginRepository.read(loginUser.id!!) ?: throw NotFoundUserException(EMPTY_STR)
    loginRepository.delete(loggedinUser)
  }

  /**
   * Updates a login user's details in the repository.
   *
   * @param loginUser The model representing the user with updated details.
   */
  override suspend fun updateLoginUser(loginUser: LoginModel) {
    TODO("Not yet implemented")
  }

}