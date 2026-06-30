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
            dependency("com.fasterxml.jackson.core:jackson-databind:2.21.4")
            dependency("com.fasterxml.jackson.core:jackson-core:2.21.4")
            dependency("com.fasterxml.jackson.core:jackson-annotations:2.21")
            dependency("tools.jackson.core:jackson-databind:3.1.4")
            dependency("tools.jackson.core:jackson-core:3.1.4")
        }
    }
}
