package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.Filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterConfigurerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpSecurity httpSecurity;

    @Test
    void configure_addsJwtTokenFilterBeforeUsernamePasswordFilter() throws Exception {
        // given
        final JwtTokenFilterConfigurer sut = new JwtTokenFilterConfigurer(jwtTokenProvider);
        when(httpSecurity.addFilterBefore(any(Filter.class), eq(UsernamePasswordAuthenticationFilter.class)))
                .thenReturn(httpSecurity);

        // when
        sut.configure(httpSecurity);

        // then
        final ArgumentCaptor<Filter> captor = ArgumentCaptor.forClass(Filter.class);
        verify(httpSecurity).addFilterBefore(captor.capture(), eq(UsernamePasswordAuthenticationFilter.class));
        assertThat(captor.getValue()).isInstanceOf(JwtTokenFilter.class);
    }
}
