package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code WebSecurityConfig} permits {@code /h2-console/**} without authentication, so the only thing
 * standing between an anonymous caller and the database is whether the console servlet is registered.
 * These tests pin that, with the default configuration, it is neither registered nor reachable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class H2ConsoleExposureTest {

    @Autowired
    private ApplicationContext context;

    @Value("${local.server.port}")
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void h2ConsoleServlet_isNotRegisteredByDefault() {
        // when
        final Map<String, ServletRegistrationBean> registrations = context.getBeansOfType(ServletRegistrationBean.class);

        // then
        assertThat(context.containsBean("h2Console")).isFalse();
        assertThat(registrations.values())
                .extracting(registration -> registration.getServlet().getClass().getName())
                .noneMatch(servletClass -> servletClass.startsWith("org.h2."));
    }

    @Test
    void h2ConsoleRequest_isPermittedBySecurityButNothingServesIt() throws IOException, InterruptedException {
        // when
        final HttpResponse<String> root = get("/h2-console");
        final HttpResponse<String> login = get("/h2-console/login.do");

        // then: a live console answers 302 (redirect to login) / 200; here no servlet handles the path
        assertThat(root.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(login.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(root.body()).doesNotContainIgnoringCase("h2 console");
        assertThat(login.body()).doesNotContainIgnoringCase("h2 console");
    }

    @Test
    void securityFilterChain_stillProtectsOtherEndpoints() throws IOException, InterruptedException {
        // sanity check: the 404 above is not explained by security being absent
        final HttpResponse<String> response = get("/courses");

        assertThat(response.statusCode()).isIn(401, 403);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
