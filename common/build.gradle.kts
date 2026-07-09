dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core", "jackson-databind")

    runtimeOnly("com.h2database", "h2")

    testImplementation("org.springframework.boot", "spring-boot-starter-test")
    testImplementation("org.junit.jupiter", "junit-jupiter-api")
    testImplementation("org.junit.platform", "junit-platform-engine")
    testImplementation("org.junit.platform", "junit-platform-launcher")
    testImplementation("org.mockito", "mockito-junit-jupiter", libs.versions.mockito.get())
}

tasks.test {
    useJUnitPlatform()
}
