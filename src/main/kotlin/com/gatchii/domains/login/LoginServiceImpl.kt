package com.gatchii.domains.login

import com.gatchii.domains.jwt.*
import com.gatchii.shared.common.Constants.Companion.EMPTY_STR
import com.gatchii.shared.exception.NotFoundUserException
import com.gatchii.utils.BCryptPasswordEncoder
import io.ktor.util.logging.*

/** Package: com.gatchii.domains.login Created: Devonshin Date: 23/09/2024 */

class LoginServiceImpl(
    private val loginRepository: LoginRepository,
    private val bCryptPasswordEncoder: BCryptPasswordEncoder,
    private val jwtService: JwtService,
    private val refreshTockenService: RefreshTokenService,
) : LoginService {

    val logger: Logger = KtorSimpleLogger("com.gatchii.domains.LoginService")

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
        //todo 로그인 아이디로 유저 조회
        //jwkProvider

        val claim: Map<String, String> = mapOf(
            "userId" to loginModel.prefixId,
            "suffixIdx" to loginModel.suffixId,
            "role" to loginModel.role.name,
        )

        val expiresIn = System.currentTimeMillis() + (1000L * 60 * 30)

        return JwtModel(
            accessToken = AccessToken(
                token = jwtService.generate(claim),
                expiresIn = expiresIn,
            ),
            refreshToken = RefreshToken(
                token = refreshTockenService.generate(mutableMapOf()),
                expiresIn = expiresIn * 4,
            ),
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