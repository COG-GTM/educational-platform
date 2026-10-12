package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.hamcrest.Matchers.oneOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the authorization rules built by {@link WebSecurityConfig#filterChain}.
 * <p>
 * Only {@link WebSecurityConfig} itself is bootstrapped (with a mocked {@link JwtTokenProvider} and a catch-all
 * controller), so the tests pin the request-matcher contract without the full application context.
 * A permitted request reaches the catch-all controller and returns 200; a denied one never does.
 */
@SpringJUnitWebConfig(classes = {WebSecurityConfig.class, WebSecurityConfigTest.Fixture.class})
@TestPropertySource(properties = "com.educational.platform.security.enabled=true")
class WebSecurityConfigTest {

    private static final String API_DOCS_PUBLIC = "com.educational.platform.security.api-docs.public=";

    @TestConfiguration
    @EnableWebMvc
    static class Fixture {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        CatchAllController catchAllController() {
            return new CatchAllController();
        }
    }

    @RestController
    static class CatchAllController {

        @RequestMapping("/**")
        String ok() {
            return "ok";
        }
    }

    abstract static class SecurityRulesTestSupport {

        @Autowired
        WebApplicationContext context;

        @Autowired
        JwtTokenProvider jwtTokenProvider;

        MockMvc sut;

        @BeforeEach
        void setUp() {
            reset(jwtTokenProvider);
            sut = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        }

        ResultActions anonymousGet(String path) throws Exception {
            return sut.perform(get(path));
        }

        void assertDenied(ResultActions result) throws Exception {
            result.andExpect(status().is(oneOf(401, 403)));
        }
    }

    @Nested
    class WhenApiDocsFlagIsAbsent extends SecurityRulesTestSupport {

        @ParameterizedTest
        @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html"})
        void apiDocs_anonymous_denied(String path) throws Exception {
            assertDenied(anonymousGet(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/v2/api-docs", "/swagger-resources", "/swagger-resources/configuration/ui", "/configuration/ui", "/configuration/security"})
        void legacySpringfoxPaths_anonymous_denied(String path) throws Exception {
            assertDenied(anonymousGet(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/users/sign-in", "/users/sign-up", "/webjars/swagger-ui/index.js", "/h2-console/login.do", "/public"})
        void publicPaths_anonymous_permitted(String path) throws Exception {
            anonymousGet(path).andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/public/other", "/publicx", "/courses", "/users/me"})
        void nonPublicPaths_anonymous_denied(String path) throws Exception {
            assertDenied(anonymousGet(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html"})
        void apiDocs_authenticatedUser_permitted(String path) throws Exception {
            sut.perform(get(path).with(user("docs-user").roles("STUDENT")))
                    .andExpect(status().isOk());
        }

        @Test
        void apiDocs_validBearerToken_permitted() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(any())).thenReturn("valid-token");
            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(
                    new UsernamePasswordAuthenticationToken("docs-user", "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

            // when / then
            sut.perform(get("/v3/api-docs").header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk());
        }

        @Test
        void apiDocs_invalidBearerToken_rejectedByJwtFilter() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(any())).thenReturn("bad-token");
            when(jwtTokenProvider.validateToken("bad-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

            // when / then
            sut.perform(get("/v3/api-docs").header("Authorization", "Bearer bad-token"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @TestPropertySource(properties = API_DOCS_PUBLIC + "false")
    class WhenApiDocsFlagIsExplicitlyFalse extends SecurityRulesTestSupport {

        @ParameterizedTest
        @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html"})
        void apiDocs_anonymous_denied(String path) throws Exception {
            assertDenied(anonymousGet(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/users/sign-in", "/users/sign-up", "/webjars/swagger-ui/index.js", "/h2-console/login.do", "/public"})
        void publicPaths_anonymous_permitted(String path) throws Exception {
            anonymousGet(path).andExpect(status().isOk());
        }
    }

    @Nested
    @TestPropertySource(properties = API_DOCS_PUBLIC + "true")
    class WhenApiDocsFlagIsTrue extends SecurityRulesTestSupport {

        @ParameterizedTest
        @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html"})
        void apiDocs_anonymous_permitted(String path) throws Exception {
            anonymousGet(path).andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/v2/api-docs", "/swagger-resources", "/swagger-resources/configuration/ui", "/configuration/ui", "/configuration/security"})
        void legacySpringfoxPaths_anonymous_stillDenied(String path) throws Exception {
            assertDenied(anonymousGet(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/v3/api-docs.json", "/v3/api-docsx", "/swagger-ui.htm", "/swagger-uix/index.html"})
        void nearMissApiDocsPaths_anonymous_denied(String path) throws Exception {
            assertDenied(anonymousGet(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/courses", "/users/me", "/course-enrollments"})
        void businessEndpoints_anonymous_stillDenied(String path) throws Exception {
            assertDenied(anonymousGet(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/users/sign-in", "/users/sign-up", "/webjars/swagger-ui/index.js", "/h2-console/login.do", "/public"})
        void publicPaths_anonymous_permitted(String path) throws Exception {
            anonymousGet(path).andExpect(status().isOk());
        }

        @Test
        void apiDocs_invalidBearerToken_stillRejectedByJwtFilter() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(any())).thenReturn("bad-token");
            when(jwtTokenProvider.validateToken("bad-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

            // when / then
            sut.perform(get("/v3/api-docs").header("Authorization", "Bearer bad-token"))
                    .andExpect(status().isBadRequest());
        }
    }
}
