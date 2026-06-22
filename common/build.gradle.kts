dependencies {
    api("org.springframework.retry", "spring-retry", libs.versions.springRetry.get())
    api("org.springframework", "spring-aspects")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core", "jackson-databind")
    runtimeOnly("com.h2database", "h2")

    testImplementation("org.junit.jupiter", "junit-jupiter-api")
    testImplementation("org.junit.jupiter", "junit-jupiter-engine")
    testImplementation("org.junit.platform", "junit-platform-engine")
    testImplementation("org.junit.platform", "junit-platform-launcher")
    testImplementation("org.mockito", "mockito-junit-jupiter", libs.versions.mockito.get())
    testImplementation("org.assertj", "assertj-core", libs.versions.assertj.get())
}

tasks.test {
    useJUnitPlatform()
}