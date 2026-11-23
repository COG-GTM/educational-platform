plugins {
    java
    alias(libs.plugins.springdependencies)
}

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

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.get()}")
        }
    }

    dependencies {
        constraints {
            "implementation"("org.springframework.boot:spring-boot-actuator-autoconfigure") {
                version {
                    reject("[0,3.1.5)")
                }
                because("CVE-2023-20883: versions before 3.1.5 are vulnerable")
            }
        }
    }

    configurations.configureEach {
        resolutionStrategy.componentSelection.withModule("org.springframework.boot:spring-boot-actuator-autoconfigure") {
            // Spring dependency management can override the rich-version constraint via useVersion.
            val parts = candidate.version.split('.', '-', '+', limit = 4)
            val release = (0..2).map { parts.getOrNull(it)?.toIntOrNull() ?: 0 }
            val minimum = listOf(3, 1, 5)
            val difference = release.zip(minimum).firstOrNull { (actual, required) -> actual != required }
            val isOlder = difference?.let { (actual, required) -> actual < required } ?: false
            val isPrerelease = release == minimum && parts.getOrNull(3)?.toIntOrNull() == null && parts.size > 3
            if (isOlder || isPrerelease) {
                reject("CVE-2023-20883: versions before 3.1.5 are vulnerable")
            }
        }
    }
}
