package com.educational.platform.users.login;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignInCommandTest {

    @Test
    void builder_allFieldsSet_commandCreated() {
        // when
        final SignInCommand sut = SignInCommand.builder()
                .username("user")
                .password("password123")
                .build();

        // then
        assertThat(sut.username()).isEqualTo("user");
        assertThat(sut.password()).isEqualTo("password123");
    }

    @Test
    void builder_nullFields_commandCreatedWithNulls() {
        // when
        final SignInCommand sut = SignInCommand.builder().build();

        // then
        assertThat(sut.username()).isNull();
        assertThat(sut.password()).isNull();
    }

    @Test
    void record_directConstructor_fieldsAccessible() {
        // when
        final SignInCommand sut = new SignInCommand("admin", "secret");

        // then
        assertThat(sut.username()).isEqualTo("admin");
        assertThat(sut.password()).isEqualTo("secret");
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final SignInCommand cmd1 = new SignInCommand("user", "pass");
        final SignInCommand cmd2 = new SignInCommand("user", "pass");

        // then
        assertThat(cmd1).isEqualTo(cmd2);
        assertThat(cmd1.hashCode()).isEqualTo(cmd2.hashCode());
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final SignInCommand cmd1 = new SignInCommand("user1", "pass");
        final SignInCommand cmd2 = new SignInCommand("user2", "pass");

        // then
        assertThat(cmd1).isNotEqualTo(cmd2);
    }
}
