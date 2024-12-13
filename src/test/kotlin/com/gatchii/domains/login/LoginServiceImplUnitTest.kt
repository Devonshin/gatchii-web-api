package com.gatchii.domains.login

import com.gatchii.domains.jwt.JwtModel
import com.gatchii.domains.jwt.JwtService
import com.gatchii.domains.jwt.RefreshTokenService
import com.gatchii.shared.exception.NotFoundUserException
import com.gatchii.utils.BCryptPasswordEncoder
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.common.UnitTest
import java.time.OffsetDateTime
import java.util.*

/** Package: com.gatchii.domains.login Created: Devonshin Date: 24/09/2024 */

@UnitTest
class LoginServiceImplUnitTest {

    private lateinit var loginService: LoginServiceImpl
    private lateinit var loginRepository: LoginRepository
    private lateinit var jwtService: JwtService
    private lateinit var bCryptPasswordEncoder: BCryptPasswordEncoder
    private lateinit var refreshTockenService: RefreshTokenService
    private val mockLoginModel = LoginModel("prefix123", "suffix456", "encodedPassword", LoginStatus.ACTIVE, OffsetDateTime.now(), null, UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        loginRepository = mockk<LoginRepository>()
        jwtService = mockk<JwtService>()
        bCryptPasswordEncoder = mockk<BCryptPasswordEncoder>()
        refreshTockenService = mockk<RefreshTokenService>()
        loginService = LoginServiceImpl(loginRepository, bCryptPasswordEncoder, jwtService, refreshTockenService)
    }

    @Test
    fun `test login process when valid credentials`() = runBlocking {
        //given
        val loginUserRequest = LoginUserRequest("prefix123", "suffix456", "password123")

        val mockJwtModel = JwtModel(mockk(), mockk())

        coEvery { loginRepository.findUser("prefix123", "suffix456") } returns mockLoginModel
        coEvery { bCryptPasswordEncoder.matches("password123", "encodedPassword") } returns true
        coEvery { jwtService.generate(any(), any(), any()) } returns "jwtToken"

        //when
        val result = loginService.loginProcess(loginUserRequest)

        //then
        assertNotNull(result)
        assertEquals(mockJwtModel, result)
        coVerify { loginRepository.findUser("prefix123", "suffix456") }
        coVerify { bCryptPasswordEncoder.matches("password123", "encodedPassword") }
        coVerify(exactly = 1) { jwtService.generate(any(), any(), any()) }
    }

    @Test
    fun `test login process when invalid password should throw NotFoundUserException`() = runBlocking {
        //given
        val loginUserRequest = LoginUserRequest("prefix123", "suffix456", "wrongPassword")

        coEvery { loginRepository.findUser("prefix123", "suffix456") } returns mockLoginModel
        coEvery { bCryptPasswordEncoder.matches("wrongPassword", "encodedPassword") } returns false
        //when
        assertThrows<NotFoundUserException> {
            val result = loginService.loginProcess(loginUserRequest)
        }
        //then
        coVerify { loginRepository.findUser("prefix123", "suffix456") }
        coVerify { bCryptPasswordEncoder.matches("wrongPassword", "encodedPassword") }
    }

    @Test
    fun `test login process when user not found should thrown NotFoundUserException`() = runBlocking {
        //given
        val loginUserRequest = LoginUserRequest("prefix123", "suffix456", "password123")

        coEvery { loginRepository.findUser("prefix123", "suffix456") } returns null

        //when
        assertThrows<NotFoundUserException> {
            loginService.loginProcess(loginUserRequest)
        }
        //then
        coVerify { loginRepository.findUser("prefix123", "suffix456") }
    }

    @Test
    fun `test login fail action throws exception`() = runBlocking {
        //given
        val loginUserRequest = LoginUserRequest("prefix123", "suffix456", "password123")

        //when //then
        val exception = assertThrows(NotFoundUserException::class.java) {
            runBlocking { loginService.loginFailAction(loginUserRequest) }
        }
        assertEquals("prefix123:suffix456", exception.message)
    }

    @Test
    fun `test create login user calls repository`() = runBlocking {
        //given
        val loginModel = LoginModel(
            "prefix123", "suffix456", "encodedPassword", LoginStatus.ACTIVE, OffsetDateTime.now(), null, UUID.randomUUID()
        )
        coEvery { loginRepository.create(loginModel) } returns loginModel

        //when
        val result = loginService.createLoginUser(loginModel)

        //then
        assertEquals(loginModel, result)
        coVerify { loginRepository.create(loginModel) }
    }

    @Test
    fun `test delete login user calls repository`() = runBlocking {
        //given
        val uuid = UUID.randomUUID()
        val loginModel = LoginModel(
            "prefix123", "suffix456", "encodedPassword",
            LoginStatus.ACTIVE, OffsetDateTime.now(), null, uuid
        )

        coEvery { loginRepository.read(uuid) } returns loginModel
        coEvery { loginRepository.delete(loginModel) } just Runs

        //when
        loginService.deleteLoginUser(loginModel)

        //then
        coVerify { loginRepository.read(uuid) }
        coVerify { loginRepository.delete(loginModel) }
    }

    @Test
    fun `test delete login user throws exception when not found`() = runBlocking {
        //given
        val uuid = UUID.randomUUID()
        val loginModel = LoginModel(
            "prefix123", "suffix456", "encodedPassword",
            LoginStatus.ACTIVE, OffsetDateTime.now(), null, uuid
        )
        coEvery { loginRepository.read(uuid) } returns null

        //when //then
        val exception = assertThrows(NotFoundUserException::class.java) {
            runBlocking { loginService.deleteLoginUser(loginModel) }
        }
        assertEquals("", exception.message)
        coVerify { loginRepository.read(uuid) }
    }


    @Test
    fun `loginSuccessAction test`() = runTest {
        //given
        val loginModel = LoginModel(
            prefixId = "oporteat",
            suffixId = "0u",
            password = "regione",
            status = LoginStatus.ACTIVE,
            lastLoginAt = OffsetDateTime.now(),
            deletedAt = null,
            id = null
        )

        //when
        //then
        val jwtModel = loginService.loginSuccessAction(loginModel)

    }


    @Test
    fun `attemptAuthenticate if user return null then throw NotFoundUser test`() = runTest {
        //given
        val loginReq = LoginUserRequest(suffixId = "0u", prefixId = "dicam", password = "solet")
        coEvery {
            loginRepository.findUser(any(), any())
        } returns null
        //when
        //then
        assertThrows<NotFoundUserException> {
            loginService.attemptAuthenticate(loginReq)
        }
        coVerify(exactly = 1) { loginRepository.findUser(any(), any()) }
    }

    @Test
    fun `attemptAuthentication test `() = runTest {
        //given
        val loginReq = LoginUserRequest(suffixId = "0u", prefixId = "dicam", password = "solet")
        coEvery {
            bCryptPasswordEncoder.matches(any(), any())
        } returns true
        coEvery {
            loginRepository.findUser(any(), any())
        } answers {
            LoginModel(
                suffixId = "0u",
                prefixId = "dicam",
                password = "solet",
                status = LoginStatus.ACTIVE,
                lastLoginAt = OffsetDateTime.now(),
                deletedAt = null,
                id = UUID.randomUUID()
            )
        }
        //when
        val attemptAuthenticate = loginService.attemptAuthenticate(loginReq)
        //then
        assertThat(attemptAuthenticate?.prefixId).isEqualTo(loginReq.prefixId)
        assertThat(attemptAuthenticate?.status).isEqualTo(LoginStatus.ACTIVE)

        coVerify(exactly = 1) { loginRepository.findUser(any(), any()) }
        coVerify(exactly = 1) { bCryptPasswordEncoder.matches(any(), any()) }
    }

    @Test
    fun `attemptAuthentication fail test`() = runTest {
        //given
        val loginReq = LoginUserRequest(suffixId = "0u", prefixId = "dicam", password = "solet")
        coEvery { bCryptPasswordEncoder.matches(any(), any()) } returns false
        coEvery { loginRepository.findUser(any(), any()) } answers {
            LoginModel(
                suffixId = "0u",
                prefixId = "dicam",
                password = "solet",
                status = LoginStatus.ACTIVE,
                lastLoginAt = OffsetDateTime.now(),
                deletedAt = null,
                id = UUID.randomUUID()
            )
        }
        //when
        //then
        assertThrows<NotFoundUserException> {
            loginService.attemptAuthenticate(loginReq)
        }
        coVerify(exactly = 1) { loginRepository.findUser(any(), any()) }
        coVerify(exactly = 1) { bCryptPasswordEncoder.matches(any(), any()) }
    }


}
