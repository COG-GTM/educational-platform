dependencies {
    api("org.springframework.retry", "spring-retry", libs.versions.springRetry.get())
    api("org.springframework", "spring-aspects")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core", "jackson-databind")
    runtimeOnly("com.h2database", "h2")
}