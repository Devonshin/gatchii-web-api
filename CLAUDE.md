# Claude Code Instructions

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md

## Project Overview

**Gatchii Web API** - A Kotlin/Ktor-based authentication and token management service for the Gatchii ecosystem.

### Tech Stack
- **Language:** Kotlin 2.1.0 (JDK 21)
- **Framework:** Ktor 2.3.13 (Netty engine)
- **DI:** Koin 4.1.0-Beta1
- **Database:** PostgreSQL (runtime), H2 (tests)
- **ORM:** Exposed 0.59.0 with HikariCP 5.1.0
- **Authentication:** JWT/JWK with nimbus-jose-jwt & jose4j
- **Testing:** JUnit 5, MockK 1.13.16, Ktor Test Engine
- **Build:** Gradle (Kotlin DSL)

### Architecture

4-Layer Clean Architecture:
```
Route Layer (HTTP endpoints)
    ↓
Service Layer (Business logic)
    ↓
Repository Layer (Data access)
    ↓
Database Layer (PostgreSQL/H2)
```

### Core Features
- User authentication (Login/Logout)
- JWT access/refresh token management
- Public JWK endpoint for token validation
- RSA key pair management
- Authenticated endpoint protection

### API Endpoints
- `GET /` - Hello World sample
- `POST /login/attempt` - User authentication
- `POST /refresh-token/renewal` - Token refresh
- `GET /.well-known/gatchii-jwks.json` - Public JWK keys
- `GET /authenticated` - Protected endpoint example

## Project Structure

### Main Source (`src/main/kotlin/com/gatchii/`)
```
├── Application.kt                          # Main entry point
├── config/
│   └── GlobalConfig.kt                     # Global configuration
├── plugins/                                # Ktor plugins
│   ├── Databases.kt                        # Database configuration
│   ├── Security.kt                         # JWT authentication setup
│   ├── Routing.kt                          # Route configuration
│   ├── Serialization.kt                   # JSON serialization
│   ├── StatusPages.kt                      # Error handling
│   ├── Validating.kt                       # Input validation
│   ├── Monitoring.kt                       # Logging & metrics
│   ├── HTTP.kt                             # HTTP features
│   └── Frameworks.kt                       # Framework setup
├── domain/                                 # Business domains
│   ├── login/                              # User authentication
│   │   ├── LoginModel.kt
│   │   ├── LoginRepository.kt
│   │   ├── LoginRepositoryImpl.kt
│   │   ├── LoginService.kt
│   │   ├── LoginServiceImpl.kt
│   │   ├── LoginRoute.kt
│   │   └── LoginMapping.kt
│   ├── jwt/                                # JWT management
│   │   ├── JwtModel.kt
│   │   ├── JwtService.kt
│   │   ├── JwtServiceImpl.kt
│   │   ├── RefreshTokenService.kt
│   │   ├── RefreshTokenServiceImpl.kt
│   │   ├── RefreshTokenRepository.kt
│   │   ├── RefreshTokenRepositoryImpl.kt
│   │   ├── RefreshTokenRoute.kt
│   │   ├── RefreshTokenMapping.kt
│   │   └── AuthResult.kt
│   ├── jwk/                                # JWK management
│   │   ├── JwkHandler.kt
│   │   ├── JwkService.kt
│   │   ├── JwkServiceImpl.kt
│   │   ├── JwkRepository.kt
│   │   ├── JwkRepositoryImpl.kt
│   │   ├── JwkRoute.kt
│   │   └── JwkMapping.kt
│   ├── rsa/                                # RSA key management
│   │   ├── RsaService.kt
│   │   ├── RsaServiceImpl.kt
│   │   ├── RsaRepository.kt
│   │   ├── RsaRepositoryImpl.kt
│   │   └── RsaMapping.kt
│   └── main/
│       └── MainRoute.kt                    # Main endpoints
├── common/                                 # Shared components
│   ├── model/
│   │   ├── BaseModel.kt                    # Base entity
│   │   └── ResultData.kt                   # API response wrapper
│   ├── repository/
│   │   ├── CrudRepository.kt               # Generic CRUD interface
│   │   ├── ExposedCrudRepository.kt        # Exposed implementation
│   │   ├── DatabaseFactory.kt              # Database factory interface
│   │   ├── DatabaseFactoryImpl.kt          # Database factory implementation
│   │   └── UUID7Table.kt                   # UUID7 table base
│   ├── exception/
│   │   ├── NotFoundUserException.kt
│   │   ├── NotSupportMethodException.kt
│   │   └── InvalidUsableJwkStatusException.kt
│   ├── serializer/
│   │   ├── UUIDSerializer.kt
│   │   └── OffsetDateTimeSerializer.kt
│   ├── const/
│   │   └── Constants.kt                    # Application constants
│   └── task/                               # Background tasks
│       ├── TaskLeadHandler.kt
│       ├── RoutineTaskHandler.kt
│       └── OnetimeTaskHandler.kt
└── utils/
    └── JwtHandler.kt                       # JWT utilities
```

### Test Structure (`src/test/kotlin/`)
```
├── shared/
│   ├── common/
│   │   ├── UnitTest.kt                     # @UnitTest annotation
│   │   └── IntegrationTest.kt              # @IntegrationTest annotation
│   └── repository/
│       └── DatabaseFactoryForTest.kt      # Test database factory
└── com/gatchii/                           # Mirror main structure
    ├── domain/
    │   ├── login/
    │   │   ├── LoginServiceImplUnitTest.kt
    │   │   ├── LoginRepositoryImplUnitTest.kt
    │   │   └── LoginRouteKtUnitTest.kt
    │   ├── jwt/
    │   │   ├── JwtServiceImplTest.kt
    │   │   ├── RefreshTokenServiceImplTest.kt
    │   │   ├── RefreshTokenRepositoryImplTest.kt
    │   │   └── RefreshTokenRouteTest.kt
    │   ├── jwk/
    │   │   ├── JwkServiceImplTest.kt
    │   │   ├── JwkRepositoryImplTest.kt
    │   │   └── JwkHandlerTest.kt
    │   ├── rsa/
    │   │   ├── RsaServiceImplTest.kt
    │   │   └── RsaRepositoryImplTest.kt
    │   └── main/
    │       └── MainRouteKtTest.kt
    ├── common/
    │   └── task/
    │       └── RoutineTaskHandlerTest.kt
    ├── utils/
    │   └── JwtHandlerTest.kt
    └── JwkServerTest.kt
```

## Development Guidelines

### Environment Profiles
- **dev**: Development (`application-dev.conf`, port 10000)
- **local**: Local (`application-local.conf`, port 80)
- **test**: Testing (`application-test.conf`, port 8880)
- **prod**: Production (`application.conf`, port 80)

### Build Commands
```bash
# Development
./gradlew run -Pdev

# Testing
./gradlew test                    # All tests
./gradlew unitTest               # Unit tests only (@UnitTest)
./gradlew integrationTest        # Integration tests only (@IntegrationTest)

# Production
./gradlew run                    # Requires DB_* environment variables
```

### Database Configuration
- **Development/Local**: Configured in application.conf
- **Production**: Environment variables (DB_URL, DB_USERNAME, DB_PASSWORD, DB_DRIVER)
- **Testing**: H2 in-memory database (automatic)

### Key Dependencies
- **Authentication**: nimbus-jose-jwt, jose4j, jbcrypt
- **Database**: Exposed ORM, HikariCP, PostgreSQL
- **Testing**: JUnit 5, MockK, AssertJ, Kotlin Test
- **Serialization**: kotlinx.serialization
- **Validation**: Ktor request validation

## Coding Standards

### Package Organization
- Follow domain-driven design principles
- Separate concerns: Route → Service → Repository → Database
- Common utilities in `com.gatchii.common`
- Domain-specific code in `com.gatchii.domain.{domain}`

### Dependency Injection
- Use Koin for DI configuration
- Service interfaces with implementation classes
- Repository pattern for data access

### Error Handling
- Custom exceptions in `com.gatchii.common.exception`
- StatusPages plugin for HTTP error mapping
- Consistent error response format

### Security
- JWT-based authentication with refresh tokens
- RSA key pair management for token signing
- Public JWK endpoint for token validation
- Input validation on all endpoints

## Testing Standards

### Test Categories
- **Unit Tests**: `@UnitTest` - Isolated component testing with mocks
- **Integration Tests**: `@IntegrationTest` - Multi-component interaction testing
- **End-to-End Tests**: Full application flow testing

### Test Naming Convention
Follow the pattern: `Should expected behavior when state under test`

**Examples:**
```kotlin
@Test
@DisplayName("Should return JWT tokens when valid credentials provided")
fun `Should return JWT tokens when valid credentials provided`() {
    // Test implementation
}

@Test
@DisplayName("Should throw NotFoundUserException when user does not exist")
fun `Should throw NotFoundUserException when user does not exist`() {
    // Test implementation
}

@Test
@DisplayName("Should validate token successfully when token is valid and not expired")
fun `Should validate token successfully when token is valid and not expired`() {
    // Test implementation
}
```

### Test Structure (AAA Pattern)
Always follow the **Arrange-Act-Assert** pattern with clear sections:

```kotlin
@Test
@DisplayName("Should create user successfully when all required data provided")
fun `Should create user successfully when all required data provided`() {
    // Arrange (Given)
    val userRequest = LoginUserRequest("prefix", "suffix", "password")
    coEvery { repository.create(any()) } returns expectedUser

    // Act (When)
    val result = service.createUser(userRequest)

    // Assert (Then)
    assertThat(result).isNotNull
    assertThat(result.prefixId).isEqualTo("prefix")
    coVerify(exactly = 1) { repository.create(any()) }
}
```

### Test Data Management
- Use **Builder Pattern** for test object creation
- Create **Test Fixtures** for common test data
- Use **Random Data Generation** for dynamic testing
- **Avoid hardcoded values** where possible

### Mock Usage Standards
- Use **MockK** for all mocking needs
- Prefer `coEvery`/`coVerify` for suspend functions
- Use `spyk` for partial mocking when needed
- Keep mocks minimal and focused

### Test Organization
- **Mirror main package structure** in test directory
- **Group related tests** in same class
- **Use descriptive class names**: `ClassNameTest` or `ClassNameUnitTest`
- **Separate unit and integration tests** clearly

### Integration Test Setup
- Use **TestContainers** for database integration tests
- Create **AbstractIntegrationTest** base class
- **Clean up test data** between tests
- Use **@DirtiesContext** when needed for Spring-based tests

### Performance Testing
- **Measure execution time** for performance-critical operations
- **Set reasonable timeouts** for async operations
- **Test memory usage** for large data operations
- **Validate thread safety** for concurrent operations

### Test Coverage Goals
- **Unit Test Coverage**: Minimum 80%
- **Integration Test Coverage**: All critical paths
- **E2E Test Coverage**: All API endpoints
- **Exception Scenarios**: All error conditions

### Assertion Guidelines
- Use **AssertJ** for fluent assertions
- **Prefer specific assertions** over generic ones
- **Test both positive and negative scenarios**
- **Verify interactions** with external dependencies

### Example Test Class Structure
```kotlin
@UnitTest
@DisplayName("LoginService Unit Tests")
class LoginServiceImplUnitTest {

    private lateinit var loginService: LoginService
    private lateinit var loginRepository: LoginRepository
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    @BeforeEach
    fun setUp() {
        loginRepository = mockk()
        passwordEncoder = mockk()
        loginService = LoginServiceImpl(loginRepository, passwordEncoder)
    }

    @Nested
    @DisplayName("Login Process Tests")
    inner class LoginProcessTests {

        @Test
        @DisplayName("Should return JWT tokens when valid credentials provided")
        fun `Should return JWT tokens when valid credentials provided`() {
            // Test implementation
        }
    }

    @Nested
    @DisplayName("User Creation Tests")
    inner class UserCreationTests {

        @Test
        @DisplayName("Should create user successfully when all required data provided")
        fun `Should create user successfully when all required data provided`() {
            // Test implementation
        }
    }
}

