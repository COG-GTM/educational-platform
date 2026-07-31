package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDTOTest {

    @Test
    void builder_populatesAllFieldsIncludingUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");

        // when
        final UserDTO dto = UserDTO.builder()
                .uuid(uuid)
                .username("username")
                .email("email@gmail.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // then
        assertThat(dto).isEqualTo(new UserDTO(uuid, "username", "email@gmail.com", RoleDTO.ROLE_STUDENT));
    }

}
