dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-security")

    testImplementation("org.junit.jupiter", "junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
    testImplementation("org.junit.platform", "junit-platform-engine")
    testImplementation("org.junit.platform", "junit-platform-launcher")
    testImplementation("org.assertj", "assertj-core", libs.versions.assertj.get())
}

tasks.test {
    useJUnitPlatform()
}

