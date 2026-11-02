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
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.get()}") {
                // CVE-2026-41721 (SNYK-JAVA-ORGSPRINGFRAMEWORKDATA-17279540): Boot 4.0.1 manages spring-data-commons 4.0.1
                bomProperty("spring-data-bom.version", rootProject.libs.versions.springData.get())
            }
        }
    }
}
