package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterConfigurerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpSecurity httpSecurity;

    @Test
    void configure_registersJwtTokenFilterAheadOfUsernamePasswordAuthenticationFilter() {
        // given - the configurer is the only place the stateless JWT filter is wired into the chain;
        // it must run before the form-login filter so a valid token authenticates the request first
        final JwtTokenFilterConfigurer sut = new JwtTokenFilterConfigurer(jwtTokenProvider);

        // when
        sut.configure(httpSecurity);

        // then - exactly one JwtTokenFilter is registered before UsernamePasswordAuthenticationFilter
        final ArgumentCaptor<JwtTokenFilter> filter = ArgumentCaptor.forClass(JwtTokenFilter.class);
        verify(httpSecurity).addFilterBefore(filter.capture(), eq(UsernamePasswordAuthenticationFilter.class));
        assertThat(filter.getValue()).isInstanceOf(JwtTokenFilter.class);
    }
}
