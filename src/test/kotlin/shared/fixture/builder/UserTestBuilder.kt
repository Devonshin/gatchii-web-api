package shared.fixture.builder

import com.gatchii.domain.login.LoginModel
import com.gatchii.domain.login.LoginStatus
import com.gatchii.domain.login.UserRole
import java.time.OffsetDateTime
import java.util.UUID

/**
 * @author Devonshin
 * @date 2025-10-03
 */
class UserTestBuilder {
    private var prefixId: String = "user"
    private var suffixId: String = "0001"
    private var password: String = "pass"
    private var rsaUid: UUID = UUID.randomUUID()
    private var status: LoginStatus = LoginStatus.ACTIVE
    private var role: UserRole = UserRole.USER
    private var lastLoginAt: OffsetDateTime = OffsetDateTime.now()
    private var deletedAt: OffsetDateTime? = null
    private var id: UUID? = null

    fun withPrefixId(value: String) = apply { this.prefixId = value }
    fun withSuffixId(value: String) = apply { this.suffixId = value }
    fun withPassword(value: String) = apply { this.password = value }
    fun withRsaUid(value: UUID) = apply { this.rsaUid = value }
    fun withStatus(value: LoginStatus) = apply { this.status = value }
    fun withRole(value: UserRole) = apply { this.role = value }
    fun withLastLoginAt(value: OffsetDateTime) = apply { this.lastLoginAt = value }
    fun withDeletedAt(value: OffsetDateTime?) = apply { this.deletedAt = value }
    fun withId(value: UUID?) = apply { this.id = value }

    /**
     * AAA - Given 단계에서 편리하게 사용자 모델을 생성.
     */
    fun build(): LoginModel = LoginModel(
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
}