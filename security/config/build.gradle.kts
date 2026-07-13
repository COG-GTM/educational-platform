dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-security")

    testImplementation("org.junit.jupiter", "junit-jupiter")
    testImplementation("org.assertj", "assertj-core", libs.versions.assertj.get())
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
