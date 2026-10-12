package com.educational.platform.users.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "com.educational.platform.security.enabled", havingValue = "true")
public class WebSecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			"/users/sign-in",
			"/users/sign-up",
			"/webjars/**",
			"/h2-console/**",
			"/public"
	};

	private static final String[] API_DOCS_PATHS = {
			"/v3/api-docs/**",
			"/v3/api-docs.yaml",
			"/swagger-ui.html",
			"/swagger-ui/**"
	};

	private final JwtTokenProvider jwtTokenProvider;
	private final boolean apiDocsPublic;

    public WebSecurityConfig(JwtTokenProvider jwtTokenProvider,
                             @Value("${com.educational.platform.security.api-docs.public:false}") boolean apiDocsPublic) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.apiDocsPublic = apiDocsPublic;
    }

    @Bean
	public SecurityFilterChain filterChain(HttpSecurity http) {
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http.authorizeHttpRequests((authorizeHttpRequests) -> {
			authorizeHttpRequests.requestMatchers(PUBLIC_PATHS).permitAll();
			if (apiDocsPublic) {
				authorizeHttpRequests.requestMatchers(API_DOCS_PATHS).permitAll();
			}
			authorizeHttpRequests.anyRequest().authenticated();
		});

		http.addFilterBefore(new JwtTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

}
