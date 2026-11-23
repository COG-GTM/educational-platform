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
                bomProperty("slf4j.version", rootProject.libs.versions.slf4j.get())
            }
        }
    }

    dependencies {
        constraints {
            add("implementation", "org.slf4j:slf4j-api:${rootProject.libs.versions.slf4j.get()}") {
                because("SNYK-JAVA-ORGSLF4J-5942503 (CWE-117) is fixed in slf4j-api 2.0.9")
            }
        }
    }
}
