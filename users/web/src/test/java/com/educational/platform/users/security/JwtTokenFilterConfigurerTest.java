package com.educational.platform.users.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterConfigurerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpSecurity httpSecurity;

    private JwtTokenFilterConfigurer sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenFilterConfigurer(jwtTokenProvider);
    }

    @Test
    void configure_addsJwtTokenFilterBeforeUsernamePasswordAuthenticationFilter() {
        // when
        sut.configure(httpSecurity);

        // then
        verify(httpSecurity).addFilterBefore(any(JwtTokenFilter.class), eq(UsernamePasswordAuthenticationFilter.class));
    }
}
