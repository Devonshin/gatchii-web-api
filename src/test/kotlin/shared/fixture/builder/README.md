# Test Fixture Builders (Guide)

/**
 * @author Devonshin
 * @date 2025-10-03
 */

이 문서는 2.2(Builder), 2.3(TestDataGenerator) 단계 작업을 대비하여, TestFixtures와의 연계 규칙을 정의합니다.

- 네임스페이스
  - Builders: `shared.fixture.builder`
  - Generator: `shared.fixture.generator`

- 빌더 네이밍 규칙
  - `UserTestBuilder`, `JwtTestBuilder`, `JwkTestBuilder`
  - 기본값은 TestFixtures와 동일하게 유지, 체이닝으로 세밀 제어 제공

- 사용 예시(가상 코드)
```kotlin
val user = UserTestBuilder()
    .withPrefixId("tester")
    .withRole(UserRole.ADMIN)
    .build()

val jwt = JwtTestBuilder()
    .withIssuer("TestIssuer")
    .withAudience("TestAudience")
    .withClaim("username", "tester")
    .buildToken()
```

- Generator 연계 포인트
  - `TestDataGenerator`는 랜덤 문자열/UUID/숫자/이메일 등을 생성
  - 빌더 기본값에 주입하거나, 직접 테스트에서 사용

- 원칙
  - 프로덕션 DTO 재사용(맵 대신 DTO)
  - 패키지/네이밍/주석 스타일은 기존 테스트와 일관성 유지
  - 신규 의존성 추가 없이 테스트 범위 내 구현을 우선