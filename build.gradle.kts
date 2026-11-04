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
}
