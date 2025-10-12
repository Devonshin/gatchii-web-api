import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mockkVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val koinVersion: String by project
val exposedVersion: String by project
val h2Version: String by project
val postgresVersion: String by project
val hikaricpVersion: String by project
val jbcryptVersion: String by project
val ktorVersion: String by project
val kotlinCoroutines: String by project
val jmhVersion = "1.37"

java.setTargetCompatibility(21)

plugins {
  kotlin("jvm") version "2.1.20"
  id("application")
  id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
  id("idea")
  kotlin("plugin.power-assert") version "2.0.0"
  id("me.champeau.jmh") version "0.7.2"
}

kotlin {
  compilerOptions {
    jvmTarget = JVM_21
  }
}

group = "com.gatchii"
version = "0.0.2"

application {
  mainClass.set("io.ktor.server.netty.EngineMain")
  val isDevelopment: Boolean = project.ext.has("dev") || project.ext.has("development")
  val isTest: Boolean = project.ext.has("test")
  val isLocal: Boolean = project.ext.has("local")
  if (isDevelopment) {
    applicationDefaultJvmArgs = listOf(
      "-Dio.ktor.development=$isDevelopment",
      "-Dconfig.resource=application-dev.conf"
    )
  } else if (isTest) {
    applicationDefaultJvmArgs = listOf(
      "-Dio.ktor.development=$isDevelopment",
      "-Dconfig.resource=application-test.conf"
    )
  } else if (isLocal) {
    applicationDefaultJvmArgs = listOf(
      "-Dio.ktor.development=$isDevelopment",
      "-Dconfig.resource=application-local.conf"
    )
  }
}

repositories {
  mavenCentral()
  maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
  maven {
    url = uri("https://maven.pkg.github.com/Devonshin/gatchii-common-util")
    credentials {
        username = System.getenv("GITHUB_USERNAME") ?: "your-github-username"
        password = System.getenv("GITHUB_TOKEN") ?: "your-personal-access-token"
    }
  }

}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("org.flywaydb:flyway-core:10.16.0")
  // Flyway 10+는 DB별 지원 모듈이 분리됨. PostgreSQL 지원 모듈 추가
  implementation("org.flywaydb:flyway-database-postgresql:10.16.0")
  implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-double-receive-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-caching-headers-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-conditional-headers-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-forwarded-header-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
  implementation("com.ucasoft.ktor:ktor-simple-cache-jvm:0.4.3")
  implementation("com.ucasoft.ktor:ktor-simple-redis-cache-jvm:0.4.3")
  implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
  implementation("io.insert-koin:koin-ktor:$koinVersion")
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
  implementation("com.h2database:h2:$h2Version")
  implementation("org.postgresql:postgresql:$postgresVersion")
  implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
  implementation("org.bouncycastle:bcprov-jdk18on:1.79")
  implementation("com.nimbusds:nimbus-jose-jwt:9.45")
  // https://mvnrepository.com/artifact/org.bitbucket.b_c/jose4j
  implementation("org.bitbucket.b_c:jose4j:0.9.6")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
  implementation("com.zaxxer:HikariCP:$hikaricpVersion")
  implementation("org.mindrot:jbcrypt:$jbcryptVersion")
  implementation("com.github.f4b6a3:uuid-creator:6.0.0")
  implementation("io.ktor:ktor-server-request-validation:$ktorVersion")
  // https://mvnrepository.com/artifact/io.fabric8/kubernetes-client-api
  //implementation("io.fabric8:kubernetes-client-api:6.13.4")

  //implementation("com.github.Devonshin:com.gatchii:gatchii-common-util:0.0.1")
  implementation("com.gatchii:gatchii-common-util:0.0.9")
  // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test
  implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

  testImplementation(kotlin("test"))
  testImplementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
  testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
  //testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
  testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
  //testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.10")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutines")
  testImplementation("io.mockk:mockk:${mockkVersion}")
  testImplementation("org.assertj:assertj-core:3.11.1")
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")

  // Testcontainers for integration tests (PostgreSQL + JUnit 5)
  testImplementation("org.testcontainers:junit-jupiter:1.19.7")
  testImplementation("org.testcontainers:postgresql:1.19.7")

  // JMH for performance benchmarking
  jmh("org.openjdk.jmh:jmh-core:$jmhVersion")
  jmh("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
  jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
}

tasks.test {
  useJUnitPlatform {
    // 기본 test에서는 통합 테스트(@integrationTest) 제외
    excludeTags("integrationTest")
  }
  systemProperty("config.resource", "application-test.conf")
  
  // 테스트 병렬 실행 설정
  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
  
  // JUnit 5 병렬 실행 설정
  systemProperty("junit.jupiter.execution.parallel.enabled", "true")
  systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
  systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
  systemProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic")
  systemProperty("junit.jupiter.execution.parallel.config.dynamic.factor", "0.5")
  
  // 테스트 메모리 설정
  minHeapSize = "512m"
  maxHeapSize = "2048m"
  
  // 테스트 리포트 향상
  reports {
    html.required.set(true)
    junitXml.required.set(true)
  }
  
  // 테스트 실행 상세 로그
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStackTraces = true
    showCauses = true
    showStandardStreams = false
  }
}
tasks.withType<KotlinCompile>().configureEach {
  compilerOptions.jvmTarget = JVM_21
}
tasks.register<Test>("unitTest") {
  group = "verification"
  description = "Run tests annotated with @UnitTest"
  // Ensure this task uses the default test source set
  testClassesDirs = sourceSets.test.get().output.classesDirs
  classpath = sourceSets.test.get().runtimeClasspath
  useJUnitPlatform {
    includeTags("unitTest")
  }
  systemProperty("config.resource", "application-test.conf")
  
  // 유닛 테스트는 더 공격적인 병렬화 가능
  maxParallelForks = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
  
  systemProperty("junit.jupiter.execution.parallel.enabled", "true")
  systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
  systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
  systemProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic")
  systemProperty("junit.jupiter.execution.parallel.config.dynamic.factor", "1.0")
  
  minHeapSize = "256m"
  maxHeapSize = "1024m"
  
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStackTraces = true
  }
}

tasks.register<Test>("integrationTest") {
  group = "verification"
  description = "Run tests annotated with @IntegrationTest"
  // Ensure this task uses the default test source set
  testClassesDirs = sourceSets.test.get().output.classesDirs
  classpath = sourceSets.test.get().runtimeClasspath
  useJUnitPlatform {
    includeTags("integrationTest")
  }
  systemProperty("config.resource", "application-test.conf")
  
  // 통합 테스트는 DB 등 외부 리소스 고려하여 보수적인 병렬화
  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 4).coerceAtLeast(1)
  
  // 통합 테스트는 클래스 단위로 순차 실행 (메서드는 병렬 가능)
  systemProperty("junit.jupiter.execution.parallel.enabled", "true")
  systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
  systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "same_thread")
  
  minHeapSize = "512m"
  maxHeapSize = "2048m"
  
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStackTraces = true
    showStandardStreams = true  // 통합 테스트는 로그 출력 유지
  }
  
  // 통합 테스트 타임아웃 설정 (Testcontainers 시작 시간 고려)
  timeout.set(java.time.Duration.ofMinutes(15))
}
tasks.processResources {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// check 시 통합 테스트까지 수행 (CI에서만 integrationTest 포함)
if (System.getenv("CI") == "true" || project.hasProperty("ci")) {
  tasks.named("check") {
    dependsOn("integrationTest")
  }
}

sourceSets {
  main {
    resources {
      srcDirs("src/main/resources")
    }
    kotlin {
      srcDirs("src/main/kotlin")
    }
  }
  test {
    resources {
      srcDirs("src/test/resources")
    }
    kotlin {
      srcDirs("src/test/kotlin")
    }
  }
}

// JMH 벤치마크 설정
jmh {
  iterations.set(3)  // 워밍업 후 실제 측정 반복 횟수
  warmupIterations.set(2)  // 워밍업 반복 횟수
  fork.set(1)  // JVM 포크 수
  benchmarkMode.set(listOf("thrpt", "avgt"))  // 처리량(throughput)과 평균 시간(average time) 측정
  timeUnit.set("ms")  // 시간 단위
  resultFormat.set("JSON")  // 결과 출력 형식
  profilers.set(listOf("gc"))  // GC 프로파일러 사용
  
  // 특정 벤치마크만 실행하려면 includes 설정 (정규식 패턴)
  includes.set(listOf(".*LoginConcurrencyBenchmark.*"))  // 주석 해제 시 해당 벤치마크만 실행
  
  // JMH 컴파일 시 Java 버전 명시
  jvmArgs.add("-Djava.version=21")
}

// JMH 생성 클래스 컴파일 시 Java 21 사용
tasks.named<JavaCompile>("jmhCompileGeneratedClasses") {
  sourceCompatibility = "21"
  targetCompatibility = "21"
  options.release.set(21)
}
