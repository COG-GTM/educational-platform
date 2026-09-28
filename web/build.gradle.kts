dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot", "spring-boot-starter-web")
    implementation("org.springframework.boot", "spring-boot-starter-validation")
    implementation("org.springframework.security", "spring-security-core")
    implementation("org.springdoc", "springdoc-openapi-starter-webmvc-ui", libs.versions.springDoc.get())
}
