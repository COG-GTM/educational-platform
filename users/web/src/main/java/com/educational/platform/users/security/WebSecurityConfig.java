package com.educational.platform.users.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "com.educational.platform.security.enabled", havingValue = "true")
public class WebSecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;

    public WebSecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
	public SecurityFilterChain filterChain(HttpSecurity http) {
		http.csrf((csrf) -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
				.ignoringRequestMatchers(bearerTokenRequest())
				// JSON-only, unauthenticated endpoints that return the JWT in the response body (no cookie is set).
				.ignoringRequestMatchers("/users/sign-in", "/users/sign-up", "/h2-console/**"));
		http.sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http.authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests.requestMatchers(
				"/users/sign-in",
				"/users/sign-up",
				"/v2/api-docs/**",
				"/v3/api-docs/**",
				"/swagger-resources/**",
				"/swagger-ui.html",
				"/swagger-ui/**",
				"/configuration/**",
				"/webjars/**",
				"/h2-console/**",
				"/public"
		).permitAll().anyRequest().authenticated());

		http.addFilterBefore(new JwtTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * Requests authenticated with an explicit {@code Authorization: Bearer} header carry no ambient
	 * browser credentials and cannot be forged cross-site, so they are exempt from CSRF checks.
	 * Any other state-changing request (e.g. one relying on cookies) must present the XSRF token.
	 */
	private RequestMatcher bearerTokenRequest() {
		return (request) -> StringUtils.hasText(jwtTokenProvider.resolveToken(request));
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

}
