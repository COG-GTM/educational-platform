dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-security")

    testImplementation("org.junit.jupiter", "junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
