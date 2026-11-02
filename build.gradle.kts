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

    // Overrides the Spring Boot BOM's Tomcat 11.0.15 (CVE-2026-53434, fixed in 11.0.23).
    ext["tomcat.version"] = "11.0.23"

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.get()}")
        }
    }
}
