package shared.fixture

import com.auth0.jwt.algorithms.Algorithm
import com.gatchii.domain.jwk.JwkModel
import com.gatchii.domain.login.LoginModel
import com.gatchii.domain.login.LoginStatus
import com.gatchii.domain.login.UserRole
import com.gatchii.plugins.JwtConfig
import com.gatchii.utils.JwtHandler
import io.ktor.util.encodeBase64
import shared.common.UnitTest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.time.OffsetDateTime
import java.util.UUID
import com.gatchii.common.utils.ECKeyPairHandler

/**
 * @author Devonshin
 * @date 2025-10-03
 */

/**
 * 테스트 데이터 생성을 위한 공통 픽스처.
 * - 기존 테스트 패턴과 일관되게 Kotlin data class(프로덕션 DTO)를 재사용합니다.
 * - 메서드는 기본값(현실적인 합리값)을 제공하고, 필요 시 파라미터로 커스터마이징할 수 있습니다.
 */
object TestFixtures {

    /**
     * LoginModel 생성기.
     * AAA 패턴 보조: Given 단계에서 테스트 사용자를 손쉽게 준비하기 위해 사용합니다.
     */
    fun createLoginModel(
        prefixId: String = "user",
        suffixId: String = "0001",
        password: String = "pass",
        rsaUid: UUID = UUID.randomUUID(),
        status: LoginStatus = LoginStatus.ACTIVE,
        role: UserRole = UserRole.USER,
        lastLoginAt: OffsetDateTime = OffsetDateTime.now(),
        deletedAt: OffsetDateTime? = null,
        id: UUID? = null,
    ): LoginModel = LoginModel(
        prefixId = prefixId,
        suffixId = suffixId,
        password = password,
        rsaUid = rsaUid,
        status = status,
        role = role,
        lastLoginAt = lastLoginAt,
        deletedAt = deletedAt,
        id = id,
    )

    /**
     * JWT 설정 기본값 생성기.
     * - issuer/audience/expireSec 기본값을 제공하며, 테스트 목적에 맞게 재정의할 수 있습니다.
     */
    fun createJwtConfig(
        audience: String = "TestAudience",
        issuer: String = "TestIssuer",
        expireSec: Long = 60,
    ): JwtConfig = JwtConfig(
        audience = audience,
        issuer = issuer,
        expireSec = expireSec,
    )

    /**
     * ECDSA(ES256) 알고리즘을 위한 키페어와 Algorithm 인스턴스 생성.
     * - 기본값으로 새 키페어를 생성합니다.
     */
    fun createAlgorithmWithNewKeyPair(): Algorithm {
        val keyPair = ECKeyPairHandler.generateKeyPair()
        return Algorithm.ECDSA256(
            keyPair.public as ECPublicKey,
            keyPair.private as ECPrivateKey,
        )
    }

    /**
     * JWT 토큰 문자열 생성기.
     * - 기본 클레임 맵과 기본 JwtConfig, 새로 생성한 ECDSA 키로 서명합니다.
     * - 필요 시 algorithm/jwtConfig/claim을 덮어써 사용할 수 있습니다.
     */
    fun createJwtToken(
        claim: Map<String, Any> = mapOf(
            "username" to "testUser",
            "role" to "user"
        ),
        algorithm: Algorithm = createAlgorithmWithNewKeyPair(),
        jwtConfig: JwtConfig = createJwtConfig(),
        jwtId: String? = UUID.randomUUID().toString(),
    ): String = JwtHandler.generate(
        jwtId = jwtId,
        claim = claim,
        algorithm = algorithm,
        jwtConfig = jwtConfig,
    )

    /**
     * Access/Refresh 토큰 두 쌍을 받아 JwtModel 구성.
     * - JwtHandler.newJwtModel을 사용하여 프로젝트 DTO와 정합성 유지.
     */
    fun createJwtModel(
        accessToken: String,
        refreshToken: String,
        accessConfig: JwtConfig = createJwtConfig(),
        refreshConfig: JwtConfig = createJwtConfig(audience = "RefreshAudience", issuer = "RefreshIssuer", expireSec = 600),
    ) = JwtHandler.newJwtModel(accessToken, accessConfig, refreshToken, refreshConfig)

    /**
     * JwkModel 생성기.
     * - ECKeyPairHandler로 생성한 키를 base64로 인코딩하여 저장합니다.
     * - deletedAt은 기본 null, id는 기본 null로 두어 테스트 상황에 맞게 주입 가능.
     */
    fun createJwkModel(
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        deletedAt: OffsetDateTime? = null,
        id: UUID? = null,
    ): JwkModel {
        val keyPair = ECKeyPairHandler.generateKeyPair()
        val privateB64 = keyPair.private.encoded.encodeBase64()
        val publicB64 = keyPair.public.encoded.encodeBase64()
        return JwkModel(
            privateKey = privateB64,
            publicKey = publicB64,
            createdAt = createdAt,
            deletedAt = deletedAt,
            id = id,
        )
    }
}