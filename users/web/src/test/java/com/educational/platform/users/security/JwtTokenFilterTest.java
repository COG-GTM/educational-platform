package com.educational.platform.users.security;

import com.educational.platform.users.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins how {@link JwtTokenFilter} surfaces the jjwt 0.12 {@link JwtTokenProvider} verdicts at the HTTP boundary.
 */
@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "fedcba9876543210fedcba9876543210";
    private static final long VALIDITY = 3600000L;

    @Mock
    private MyUserDetails myUserDetails;

    private JwtTokenProvider jwtTokenProvider;
    private JwtTokenFilter sut;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        sut = new JwtTokenFilter(jwtTokenProvider);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validBearerToken_authenticationSetAndChainContinues() throws Exception {
        // given
        final String token = jwtTokenProvider.createToken("username", List.of(Role.ROLE_STUDENT));
        when(myUserDetails.loadUserByUsername("username"))
                .thenReturn(User.withUsername("username").password("").authorities(Role.ROLE_STUDENT).build());

        // when
        sut.doFilter(requestWithAuthorization("Bearer " + token), response, chain);

        // then
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("username");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_noAuthorizationHeader_chainContinuesUnauthenticated() throws Exception {
        // when
        sut.doFilter(new MockHttpServletRequest(), response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(myUserDetails);
    }

    @Test
    void doFilter_tokenSignedWithOtherKey_badRequestAndChainStopped() throws Exception {
        // given
        final String token = new JwtTokenProvider(myUserDetails, VALIDITY, OTHER_SECRET)
                .createToken("username", List.of(Role.ROLE_ADMIN));

        // when
        sut.doFilter(requestWithAuthorization("Bearer " + token), response, chain);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getErrorMessage()).isEqualTo("Expired or invalid JWT token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(myUserDetails);
    }

    @Test
    void doFilter_expiredToken_badRequestAndChainStopped() throws Exception {
        // given
        final String expired = new JwtTokenProvider(myUserDetails, -60000L, SECRET)
                .createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        sut.doFilter(requestWithAuthorization("Bearer " + expired), response, chain);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(myUserDetails);
    }

    @Test
    void doFilter_malformedToken_badRequestAndChainStopped() throws Exception {
        // when
        sut.doFilter(requestWithAuthorization("Bearer not-a-jwt"), response, chain);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(myUserDetails);
    }

    @Test
    void doFilter_nonBearerAuthorization_chainContinuesUnauthenticated() throws Exception {
        // when
        sut.doFilter(requestWithAuthorization("Basic dXNlcjpwYXNz"), response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(myUserDetails);
    }

    private static MockHttpServletRequest requestWithAuthorization(String value) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", value);
        return request;
    }
}
