dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.boot", "spring-boot-starter-json")
    implementation("org.springframework.retry", "spring-retry", libs.versions.springRetry.get())
    implementation("org.springframework", "spring-aspects")

    runtimeOnly("com.h2database", "h2")

    testImplementation("org.junit.jupiter", "junit-jupiter-api")
    testImplementation("org.junit.jupiter", "junit-jupiter-engine")
    testImplementation("org.junit.platform", "junit-platform-engine")
    testImplementation("org.junit.platform", "junit-platform-launcher")
    testImplementation("org.assertj", "assertj-core", libs.versions.assertj.get())
}

tasks.test {
    useJUnitPlatform()
}
