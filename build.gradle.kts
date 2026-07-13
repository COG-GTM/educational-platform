plugins {
    java
    jacoco
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
        plugin("jacoco")
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

    tasks.withType<Test>().configureEach {
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Generates an aggregate JaCoCo coverage report across all modules."

    val testTasks = subprojects.map { it.tasks.withType<Test>() }
    dependsOn(testTasks)

    val mainSourceDirs = subprojects.map { it.file("src/main/java") }
    sourceDirectories.setFrom(files(mainSourceDirs))
    additionalSourceDirs.setFrom(files(mainSourceDirs))

    classDirectories.setFrom(files(subprojects.map { sp ->
        sp.fileTree(sp.layout.buildDirectory.dir("classes/java/main"))
    }))

    executionData.setFrom(files(subprojects.map { sp ->
        sp.fileTree(sp.layout.buildDirectory) { include("jacoco/*.exec") }
    }))

    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregate/jacoco.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate/html"))
    }
}
