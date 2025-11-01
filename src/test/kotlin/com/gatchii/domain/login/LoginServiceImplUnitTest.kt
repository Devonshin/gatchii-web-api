package com.gatchii.domain.login

import com.gatchii.common.exception.NotFoundUserException
import com.gatchii.common.utils.BCryptPasswordEncoder
import com.gatchii.common.utils.RsaPairHandler
import com.gatchii.domain.jwt.JwtService
import com.gatchii.domain.jwt.RefreshTokenService
import com.gatchii.domain.rsa.RsaModel
import com.gatchii.domain.rsa.RsaService
import com.gatchii.plugins.JwtConfig
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
  private lateinit var rsaService: RsaService
  private val jwtConfig = JwtConfig(
    audience = "", issuer = "", expireSec = 10
  )
  private val refreshJwtConfig = JwtConfig(
    audience = "", issuer = "", expireSec = 100
  )
  private val loginModelStub =
    LoginModel(
      "prefix123",
      "suffix456",
      "encodedPassword",
      UUID.randomUUID(),
      LoginStatus.ACTIVE,
      role = UserRole.USER,
      OffsetDateTime.now(),
      null,
      UUID.randomUUID()
    )

  @BeforeEach
  fun setUp() {
    loginRepository = mockk<LoginRepository>()
    jwtService = mockk<JwtService>()
    bCryptPasswordEncoder = mockk<BCryptPasswordEncoder>()
    refreshTockenService = mockk<RefreshTokenService>()
    rsaService = mockk<RsaService>()
    loginService =
      LoginServiceImpl(loginRepository, bCryptPasswordEncoder, jwtService, refreshTockenService, rsaService)
  }

  @Test
  fun `success login process then return JwtModel`() = runTest {
    //given
    val loginUserRequest = LoginUserRequest("prefix123", "suffix456", "password123")

    coEvery { loginRepository.findUser("prefix123", "suffix456") } returns loginModelStub
    coEvery { loginRepository.update(any()) } returns loginModelStub
    coEvery { bCryptPasswordEncoder.matches("password123", "encodedPassword") } returns true
    coEvery { jwtService.generate(any()) } returns "jwtToken"
    coEvery { jwtService.config() } returns jwtConfig
    coEvery { refreshTockenService.config() } returns refreshJwtConfig
    coEvery { refreshTockenService.generate(any()) } returns "refreshJwtToken"
    coEvery { rsaService.getRsa(any()) } returns RsaModel(
      publicKey = "",
      privateKey = "",
      exponent = "",
      modulus = "",
      createdAt = OffsetDateTime.now(),
      id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
    )
    coEvery { rsaService.encrypt(any(), any()) } returns "encrypted"
    //when
    val result = loginService.loginProcess(loginUserRequest)

    //then
    assertNotNull(result)
    assertThat(result?.accessToken?.token).isEqualTo("jwtToken")
    assertThat(result?.refreshToken?.token).isEqualTo("refreshJwtToken")

    coVerify { loginRepository.findUser("prefix123", "suffix456") }
    coVerify(exactly = 1) { loginRepository.update(any()) }
    coVerify { bCryptPasswordEncoder.matches("password123", "encodedPassword") }
    coVerify(exactly = 1) { jwtService.generate(any()) }
    coVerify(exactly = 1) { jwtService.config() }
    coVerify(exactly = 1) { refreshTockenService.generate(any()) }
    coVerify(exactly = 1) { refreshTockenService.config() }
    coVerify(exactly = 1) { rsaService.getRsa(any()) }
    coVerify(exactly = 1) { rsaService.encrypt(any(), any()) }
  }

  @Test
  fun `invalid password login process should throw NotFoundUserException`() = runTest {
    //given
    val loginUserRequest = LoginUserRequest("prefix123", "suffix456", "wrongPassword")

    coEvery { loginRepository.findUser("prefix123", "suffix456") } returns loginModelStub
    coEvery { bCryptPasswordEncoder.matches("wrongPassword", "encodedPassword") } returns false
    //when
    assertThrows<NotFoundUserException> {
      loginService.loginProcess(loginUserRequest)
    }
    //then
    coVerify { loginRepository.findUser("prefix123", "suffix456") }
    coVerify { bCryptPasswordEncoder.matches("wrongPassword", "encodedPassword") }
  }

  @Test
  fun `test login process when user not found should thrown NotFoundUserException`() = runTest {
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
  fun `test login fail action throws exception`() = runTest {
    //given
    val loginUserRequest = LoginUserRequest("prefix123", "suffix456", "password123")

    //when //then
    val exception = assertThrows<NotFoundUserException> {
      loginService.loginFailAction(loginUserRequest)
    }
    assertEquals("Not found user: prefix123:suffix456", exception.message)
  }

  @Test
  fun `test create login user calls repository`() = runTest {
    //given
    val loginModel = LoginModel(
      "prefix123",
      "suffix456",
      "encodedPassword",
      UUID.randomUUID(),
      LoginStatus.ACTIVE,
      role = UserRole.USER,
      OffsetDateTime.now(),
      null,
      UUID.randomUUID()
    )
    coEvery { loginRepository.create(loginModel) } returns loginModel

    //when
    val result = loginService.createLoginUser(loginModel)

    //then
    assertEquals(loginModel, result)
    coVerify { loginRepository.create(loginModel) }
  }

  @Test
  fun `test delete login user calls repository`() = runTest {
    //given
    val uuid = UUID.randomUUID()
    val loginModel = LoginModel(
      "prefix123", "suffix456", "encodedPassword", UUID.randomUUID(),
      LoginStatus.ACTIVE, role = UserRole.USER, OffsetDateTime.now(), null, uuid
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
  fun `test delete login user throws exception when not found`() = runTest {
    //given
    val uuid = UUID.randomUUID()
    val loginModel = LoginModel(
      "prefix123", "suffix456", "encodedPassword", UUID.randomUUID(),
      LoginStatus.ACTIVE, role = UserRole.USER, OffsetDateTime.now(), null, uuid
    )
    coEvery { loginRepository.read(uuid) } returns null

    //when //then
    val exception = assertThrows<NotFoundUserException> {
      loginService.deleteLoginUser(loginModel)
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
      rsaUid = UUID.randomUUID(),
      status = LoginStatus.ACTIVE,
      role = UserRole.USER,
      lastLoginAt = OffsetDateTime.now(),
      deletedAt = null,
      id = null
    )
    val rsaKeyPair = RsaPairHandler.generateRsaDataPair()
    val privateKey = rsaKeyPair.privateKey
    val publicKey = rsaKeyPair.publicKey
    coEvery { loginRepository.update(any()) } returns loginModel
    coEvery { jwtService.generate(any()) } returns "jwtToken"
    coEvery { jwtService.config() } returns jwtConfig
    coEvery { refreshTockenService.config() } returns refreshJwtConfig
    coEvery { rsaService.encrypt(any(), any()) } returns "jwtToken"
    coEvery { rsaService.getRsa(any()) } returns RsaModel(
      publicKey = publicKey.publicKey,
      privateKey = privateKey.privateKey,
      exponent = publicKey.e,
      modulus = publicKey.n,
      createdAt = OffsetDateTime.now(),
      id = loginModel.rsaUid,
    )
    coEvery { refreshTockenService.generate(any()) } returns "refreshjwtToken"
    val now = OffsetDateTime.now()
    val jwtExpireSecond1 = now.plusSeconds(jwtConfig.expireSec.toLong() - 1).toEpochSecond()
    val jwtExpireSecond2 = now.plusSeconds(jwtConfig.expireSec.toLong() + 1).toEpochSecond()
    val refreshExpireSeconds1 = now.plusSeconds(refreshJwtConfig.expireSec.toLong() - 1).toEpochSecond()
    val refreshExpireSeconds2 = now.plusSeconds(refreshJwtConfig.expireSec.toLong() + 1).toEpochSecond()
    //when
    val jwtModel = loginService.loginSuccessAction(loginModel)
    //then
    assertThat(jwtModel).isNotNull
    assertThat(jwtModel.accessToken.token).isEqualTo("jwtToken")
    assertThat(jwtModel.accessToken.expiresIn).isBetween(jwtExpireSecond1, jwtExpireSecond2)
    assertThat(jwtModel.refreshToken.token).isEqualTo("refreshjwtToken")
    assertThat(jwtModel.refreshToken.expiresIn).isBetween(refreshExpireSeconds1, refreshExpireSeconds2)

    coVerify(exactly = 1) { loginRepository.update(any()) }
    coVerify(exactly = 1) { jwtService.generate(any()) }
    coVerify(exactly = 1) { refreshTockenService.generate(any()) }
    coVerify(exactly = 1) { jwtService.config() }
    coVerify(exactly = 1) { refreshTockenService.config() }
    coVerify(exactly = 1) { rsaService.encrypt(any(), any()) }
    coVerify(exactly = 1) { rsaService.getRsa(any()) }

  }

  @Test
  fun `attemptAuthenticate if user return null`() = runTest {
    //given
    val loginReq = LoginUserRequest(suffixId = "0u", prefixId = "dicam", password = "solet")
    coEvery {
      loginRepository.findUser(any(), any())
    } returns null
    //when
    val attemptAuthenticate = loginService.attemptAuthenticate(loginReq)
    //then
    assertThat(attemptAuthenticate).isNull()

    coVerify(exactly = 1) { loginRepository.findUser(any(), any()) }
  }

  @Test
  fun `when user found and password matched then return LoginModel`() = runTest {
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
        rsaUid = UUID.randomUUID(),
        status = LoginStatus.ACTIVE,
        role = UserRole.USER,
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
  fun `when matched password then return null`() = runTest {
    //given
    val loginReq = LoginUserRequest(suffixId = "0u", prefixId = "dicam", password = "solet")
    coEvery { bCryptPasswordEncoder.matches(any(), any()) } returns false
    coEvery { loginRepository.findUser(any(), any()) } answers {
      LoginModel(
        suffixId = "0u",
        prefixId = "dicam",
        password = "solet",
        rsaUid = UUID.randomUUID(),
        status = LoginStatus.ACTIVE,
        role = UserRole.USER,
        lastLoginAt = OffsetDateTime.now(),
        deletedAt = null,
        id = UUID.randomUUID()
      )
    }
    //when
    val attemptAuthenticate = loginService.attemptAuthenticate(loginReq)
    //then
    assertThat(attemptAuthenticate).isNull()
    coVerify(exactly = 1) { loginRepository.findUser(any(), any()) }
    coVerify(exactly = 1) { bCryptPasswordEncoder.matches(any(), any()) }
  }


}
