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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterConfigurerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpSecurity httpSecurity;

    @Test
    void configure_addsJwtTokenFilterBeforeUsernamePasswordAuthenticationFilter() throws Exception {
        // given
        final JwtTokenFilterConfigurer sut = new JwtTokenFilterConfigurer(jwtTokenProvider);

        // when
        sut.configure(httpSecurity);

        // then
        final ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        verify(httpSecurity).addFilterBefore(filterCaptor.capture(), eq(UsernamePasswordAuthenticationFilter.class));
        assertThat(filterCaptor.getValue()).isInstanceOf(JwtTokenFilter.class);
    }

    @Test
    void constructor_withProvider_createsConfigurer() {
        // when
        final JwtTokenFilterConfigurer configurer = new JwtTokenFilterConfigurer(jwtTokenProvider);

        // then
        assertThat(configurer).isNotNull();
    }
}
