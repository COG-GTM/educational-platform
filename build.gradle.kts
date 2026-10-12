import java.security.SecureRandom
import java.util.Base64

plugins {
    java
    alias(libs.plugins.springdependencies)
}

val testJwtSecretKey: String = Base64.getEncoder().encodeToString(
    ByteArray(48).also { SecureRandom().nextBytes(it) }
)

allprojects {
    group = "com.educational.platform"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("java")
        plugin("io.spring.dependency-management")
        plugin("java-library")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    tasks.withType<Test>().configureEach {
        jvmArgumentProviders.add(object : CommandLineArgumentProvider {
            override fun asArguments() = listOf("-Dsecurity.jwt.token.secret-key=$testJwtSecretKey")
        })
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.get()}")
        }
    }
}
