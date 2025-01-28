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

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "2.3.13"
    //id("io.ktor.plugin") version "3.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("idea")
    kotlin("plugin.power-assert") version "2.0.0"
}

group = "com.gatchii.webapi"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-auto-head-response-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-double-receive-jvm")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-caching-headers-jvm")
    implementation("io.ktor:ktor-server-compression-jvm")
    implementation("io.ktor:ktor-server-conditional-headers-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-forwarded-header-jvm")
    implementation("io.ktor:ktor-server-default-headers-jvm")
    implementation("com.ucasoft.ktor:ktor-simple-cache-jvm:0.4.3")
    implementation("com.ucasoft.ktor:ktor-simple-redis-cache-jvm:0.4.3")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.h2database:h2:$h2Version")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-config-yaml")
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

    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC.2")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("unitTest", Test::class) {
    group = "test"
    description = "Run unit tests annotated with @UnitTest"
    useJUnitPlatform {
        includeTags("unitTest")
    }
}

tasks.register("integrationTest", Test::class) {
    group = "test"
    description = "Run unit tests annotated with @IntegrationTest"
    useJUnitPlatform {
        includeTags("integrationTest")
    }
}
tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

sourceSets {
    main {
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
        kotlin {
            setSrcDirs(listOf("src/main/kotlin"))
        }
    }
    test {
        resources {
            setSrcDirs(listOf("src/test/resources"))
        }
        kotlin {
            setSrcDirs(listOf("src/test/kotlin"))
        }
    }
}