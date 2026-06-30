dependencies {
    api("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.retry", "spring-retry", libs.versions.springRetry.get())
    implementation("org.springframework.boot", "spring-boot-starter-aspectj")
    runtimeOnly("com.h2database", "h2")
}
