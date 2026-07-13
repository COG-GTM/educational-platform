dependencies {
    runtimeOnly("com.h2database", "h2")

    testImplementation("org.junit.jupiter", "junit-jupiter")
    testImplementation("org.assertj", "assertj-core", libs.versions.assertj.get())
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
