package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that key HTTP testing utilities are available and functional
 * when the embedded server is running via the Spring Boot plugin.
 * In Spring Boot 4.x, {@code TestRestTemplate} was removed; this test
 * validates that {@code RestClient} (the replacement) is on the classpath,
 * that {@code LocalTestWebServer} is available, and that the embedded
 * server is reachable — confirming the full bootRun-equivalent HTTP
 * stack works end-to-end.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class TestRestClientAvailabilityTest {

    @LocalServerPort
    private int port;

    @Test
    void restClient_shouldBeAvailableOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.web.client.RestClient"))
                .as("RestClient must be available for HTTP testing in Spring Boot 4.x")
                .doesNotThrowAnyException();
    }

    @Test
    void embeddedServer_shouldBeReachableViaHttp() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("Embedded server must respond to HTTP requests (any status is valid — "
                        + "we verify the server is listening and the HTTP stack is functional)")
                .isGreaterThan(0);
    }

    @Test
    void localTestWebServer_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.http.server.LocalTestWebServer"))
                .as("LocalTestWebServer (Spring Boot 4.x) must be available from spring-boot-test")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvcTester_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.test.web.servlet.assertj.MockMvcTester"))
                .as("MockMvcTester must be available for fluent MockMvc assertions")
                .doesNotThrowAnyException();
    }

    @Test
    void restClientBuilder_shouldBeCreatable() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        assertThat(client)
                .as("RestClient.Builder must be usable to create a client pointing to the embedded server")
                .isNotNull();
    }
}
