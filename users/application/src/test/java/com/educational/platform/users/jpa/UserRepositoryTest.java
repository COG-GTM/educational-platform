package com.educational.platform.users.jpa;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

@DataJpaTest
public class UserRepositoryTest {

    private static final PasswordEncoder PASSWORD_ENCODER = mock(PasswordEncoder.class);

    @Autowired
    private UserRepository sut;

    private static User user(String username, String email) {
        return new User(UserRegistrationCommand.builder()
                .username(username)
                .email(email)
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build(), PASSWORD_ENCODER);
    }

    @Test
    void saveAndFlush_duplicateUsername_dataIntegrityViolationException() {
        // given
        sut.saveAndFlush(user("duplicate", "first@gmail.com"));

        // when
        final ThrowableAssert.ThrowingCallable save = () -> sut.saveAndFlush(user("duplicate", "second@gmail.com"));

        // then
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(save)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("custom_user_username_uk"));
    }

    @Test
    void saveAndFlush_distinctUsernamesSameEmail_bothSaved() {
        // given/when
        sut.saveAndFlush(user("first_user", "shared@gmail.com"));
        sut.saveAndFlush(user("second_user", "shared@gmail.com"));

        // then
        assertThat(sut.existsByUsername("first_user")).isTrue();
        assertThat(sut.existsByUsername("second_user")).isTrue();
    }

    @Test
    void existsByUsername_unknownUsername_false() {
        // given
        sut.saveAndFlush(user("known_user", "known@gmail.com"));

        // when
        final boolean result = sut.existsByUsername("unknown_user");

        // then
        assertThat(result).isFalse();
    }
}
