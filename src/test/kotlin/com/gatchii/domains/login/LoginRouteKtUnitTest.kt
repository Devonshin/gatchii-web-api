package com.gatchii.domains.login

import com.gatchii.domains.jwt.AccessToken
import com.gatchii.domains.jwt.JwtModel
import com.gatchii.domains.jwt.RefreshToken
import com.gatchii.plugins.ErrorResponse
import com.gatchii.plugins.JwtResponse
import com.gatchii.shared.repository.DatabaseFactoryForTest
import com.gatchii.utils.BCryptPasswordEncoder
import com.typesafe.config.ConfigFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import shared.common.UnitTest
import shared.repository.dummyLoginQueryList
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

@UnitTest
class LoginRouteKtUnitTest {

    companion object {

        private val databaseFactory: DatabaseFactoryForTest = DatabaseFactoryForTest()
        private lateinit var loginRepository: LoginRepository
        //private lateinit var jwtService: JwtService
        //private lateinit var refreshTockenService: RefreshTokenService
        private lateinit var bCryptPasswordEncoder: BCryptPasswordEncoder
        lateinit var loginService: LoginService

        @BeforeAll
        @JvmStatic
        fun init() {
            println("init..")
            databaseFactory.connect()
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(LoginTable)
                SchemaUtils.createMissingTablesAndColumns(LoginTable)
                execInBatch(dummyLoginQueryList)
            }
        }

        @AfterAll
        @JvmStatic
        fun destroy() {
            databaseFactory.close()
        }
    }

    @BeforeTest
    fun setup() {
        bCryptPasswordEncoder = mockk()
        loginRepository = mockk()
        //jwtService = mockk()
        //refreshTockenService = mockk()
        loginService = spyk(LoginServiceImpl(loginRepository, bCryptPasswordEncoder, mockk(), mockk(), mockk()))
    }

    inline fun setupLoginRouteTest(crossinline block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment {
            config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
        }
        application {
            routing {
                route("/login") {
                    loginRoute(loginService) // loginRoute에 loginService 전달
                }
            }
        }
        block()
    }

    @Test
    fun `Login page should return 404`() = setupLoginRouteTest {
        val bodyAsText = client.get("/login").bodyAsText()
        assert(bodyAsText.isNotEmpty())
        assert(bodyAsText == "404: Page Not Found")
    }

    @Test
    fun `Invalid login suffixId request should return BadRequest `() = setupLoginRouteTest {
        //when
        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            val loginUserRequest = LoginUserRequest(prefixId = "test", suffixId = "", password = "<PASSWORD>")
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()
        val errorResponse = Json.decodeFromString<ErrorResponse>(response)
        //then
        assert(errorResponse.code == HttpStatusCode.BadRequest.value)
        assert(errorResponse.message == "Invalid login parameter")
        assert(errorResponse.path == "/login/attempt")
    }

    @Test
    fun `Invalid login prefixId request should return BadRequest `() = setupLoginRouteTest {
        //when
        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            val loginUserRequest = LoginUserRequest(prefixId = "", suffixId = "test", password = "<PASSWORD>")
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()
        val errorResponse = Json.decodeFromString<ErrorResponse>(response)
        //then
        assert(errorResponse.code == HttpStatusCode.BadRequest.value)
        assert(errorResponse.message == "Invalid login parameter")
        assert(errorResponse.path == "/login/attempt")
    }

    @Test
    fun `Invalid login password request should return BadRequest`() = setupLoginRouteTest {
        //when
        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            val loginUserRequest = LoginUserRequest(prefixId = "test", suffixId = "suffixId", password = "")
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()
        val errorResponse = Json.decodeFromString<ErrorResponse>(response)
        //then
        assert(errorResponse.code == HttpStatusCode.BadRequest.value)
        assert(errorResponse.message == "Invalid login parameter")
        assert(errorResponse.path == "/login/attempt")
    }

    @Test
    fun `Invalid login id request should return BadRequest `() = setupLoginRouteTest {
        //when
        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            val loginUserRequest = LoginUserRequest(prefixId = "test", suffixId = "suffixId", password = "")
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()
        val errorResponse = Json.decodeFromString<ErrorResponse>(response)
        //then
        assert(errorResponse.code == HttpStatusCode.BadRequest.value)
        assert(errorResponse.message == "Invalid login parameter")
        assert(errorResponse.path == "/login/attempt")
    }

    @Test
    fun `Invalid login password request should return BadRequest `() = setupLoginRouteTest {
        //when
        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            val loginUserRequest = LoginUserRequest(prefixId = "test", suffixId = "suffixId", password = "")
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()
        val errorResponse = Json.decodeFromString<ErrorResponse>(response)
        //then
        assert(errorResponse.code == HttpStatusCode.BadRequest.value)
        assert(errorResponse.message == "Invalid login parameter")
        assert(errorResponse.path == "/login/attempt")
    }

    @Test
    fun `LoginAttempt request when user not found throw NotfoundUserException`() = setupLoginRouteTest {
        //given
        val loginUserRequest = LoginUserRequest("01922d5e-9721-77f0-8093-55f799339493", "loginId", "wrongPassword")

        coEvery { loginService.attemptAuthenticate(loginUserRequest) } returns null
        //when
        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()
        //then
        val errorResponse = Json.decodeFromString<ErrorResponse>(response)
        println("errorResponse: $errorResponse")
        assert(errorResponse.code == HttpStatusCode.NotFound.value)
        assert(errorResponse.message == "Not found user: 01922d5e-9721-77f0-8093-55f799339493:loginId")
        assert(errorResponse.path == "/login/attempt")

        coVerify(exactly = 1) { loginService.attemptAuthenticate(loginUserRequest) }
    }

    @Test
    fun `LoginAttempt when password not matched then throw NotFoundUser Exception`() = setupLoginRouteTest {
        //given
        val bCryptor = BCryptPasswordEncoder()
        val password = bCryptor.encode("wrongPassword")
        val loginUserRequest = LoginUserRequest("01922d5e-9721-77f0-8093-55f799339493", "loginId", "wrongPassword")
        val loginModel = LoginModel(
            "01922d5e-9721-77f0-8093-55f799339493", "loginId", password, rsaUid = UUID.randomUUID(),
            LoginStatus.ACTIVE, UserRole.USER, OffsetDateTime.now(), null, UUID.randomUUID()
        )

        coEvery { bCryptPasswordEncoder.matches(any(), any()) } returns false
        coEvery { loginRepository.findUser(any(), any()) } returns loginModel
        //coEvery { loginService.attemptAuthenticate(loginUserRequest) } returns null
        //coEvery { loginService.loginFailAction(loginUserRequest) } throws NotFoundUserException("mock throw message")
        //when
        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()
        //then
        val errorResponse = Json.decodeFromString<ErrorResponse>(response)
        println("errorResponse: $errorResponse")
        assert(errorResponse.code == HttpStatusCode.NotFound.value)
        assert(errorResponse.message == "Not found user: 01922d5e-9721-77f0-8093-55f799339493:loginId")
        assert(errorResponse.path == "/login/attempt")

        coVerify(exactly = 1) { bCryptPasswordEncoder.matches(any(), any()) }
        coVerify(exactly = 1) { loginRepository.findUser(any(), any()) }
        //coVerify(exactly = 1) { loginService.attemptAuthenticate(loginUserRequest) }
        //coVerify(exactly = 1) { loginService.loginFailAction(loginUserRequest) }
    }

    @Test
    fun `LoginAttempt then return JwtResponse`() = setupLoginRouteTest {
        //given

        val loginUserRequest = LoginUserRequest("01922d5e-9721-77f0-8093-55f799339493", "loginId", "wrongPassword")
        val jwtModel = JwtModel(
            accessToken = AccessToken(
                token = "eyJraWQiOiI4OTMyMDRiNy1lZjg4LTQ2MDAtOWViZi1iMDhlZTVkMjA4ZTYiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJUZXN0QXVkaWVuY2UiLCJpc3MiOiJUZXN0SXNzdWVyIiwiY2xhaW0iOnsidXNlcm5hbWUiOiJ0ZXN0VXNlciIsInVzZXJJZCI6InRlc3RVc2VySWQiLCJyb2xlIjoidXNlciJ9LCJleHAiOjE3MzEyMzQ3NDh9.Q4Ekt5Ho0xOE849cPDh2zT--335y3Whe50jDxvyJwuPO4HZZYBC2HItqStzXX319E3AcVu065RjpNjTZ061azg",
                expiresIn = 1731234748
            ),
            refreshToken = RefreshToken(token = "<KEY>", expiresIn = 1731234748)
        )
        coEvery { loginService.loginProcess(loginUserRequest) } returns jwtModel
        //when

        val response = client.post("/login/attempt") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(loginUserRequest))
        }.bodyAsText()

        //then
        val errorResponse = Json.decodeFromString<JwtResponse>(response)
        println("errorResponse: $errorResponse")
        assert(errorResponse.code == HttpStatusCode.OK.value)
        assert(errorResponse.message == "Success")
        coVerify(exactly = 1) { loginService.loginProcess(loginUserRequest) }

    }



}