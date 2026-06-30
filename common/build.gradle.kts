dependencies {
    api("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.retry", "spring-retry", libs.versions.springRetry.get())
    implementation("org.springframework.boot", "spring-boot-starter-aspectj")
    runtimeOnly("com.h2database", "h2")

    testImplementation("org.springframework.boot", "spring-boot-starter-test")
    testImplementation("org.springframework.boot", "spring-boot-jdbc-test")
    testImplementation("org.springframework.boot", "spring-boot-data-jpa-test")
    testImplementation("org.junit.jupiter", "junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
    testImplementation("org.junit.platform", "junit-platform-engine")
    testImplementation("org.junit.platform", "junit-platform-launcher")
    testImplementation("org.mockito", "mockito-junit-jupiter", libs.versions.mockito.get())
    testImplementation("org.assertj", "assertj-core", libs.versions.assertj.get())
}

tasks.test {
    useJUnitPlatform()
}
