dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.retry", "spring-retry", "2.0.11")
    implementation("org.springframework", "spring-aspects")
    runtimeOnly("com.h2database", "h2")
}