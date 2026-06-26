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
            dependency("tools.jackson.core:jackson-databind:${rootProject.libs.versions.jackson3Databind.get()}")
            dependency("tools.jackson.core:jackson-core:${rootProject.libs.versions.jackson3Core.get()}")
            dependency("com.fasterxml.jackson.core:jackson-annotations:${rootProject.libs.versions.jacksonAnnotations.get()}")
        }
    }
}
