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
        dependencies {
            // CVE-2023-51074: json-path < 2.9.0
            dependency("com.jayway.jsonpath:json-path:${rootProject.libs.versions.jsonPath.get()}")
            dependency("com.jayway.jsonpath:json-path-assert:${rootProject.libs.versions.jsonPath.get()}")
        }
    }
}
