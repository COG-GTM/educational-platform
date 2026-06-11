package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDTOTest {

    @Test
    void builder_buildsAllFields() {
        // when
        final UserDTO dto = UserDTO.builder()
                .username("username")
                .email("email@gmail.com")
                .role(RoleDTO.ROLE_TEACHER)
                .build();

        // then
        assertThat(dto.username()).isEqualTo("username");
        assertThat(dto.email()).isEqualTo("email@gmail.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void equals_sameValues_areEqual() {
        final UserDTO first = UserDTO.builder().username("u").email("e@gmail.com").role(RoleDTO.ROLE_STUDENT).build();
        final UserDTO second = UserDTO.builder().username("u").email("e@gmail.com").role(RoleDTO.ROLE_STUDENT).build();
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }
}
