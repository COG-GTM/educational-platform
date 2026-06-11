package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    }

    @Test
    void constructor_validStudentCommand_userCreated() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final User user = new User(command, passwordEncoder);

        // then
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_STUDENT);
    }

    @Test
    void constructor_validTeacherCommand_userCreated() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();

        // when
        final User user = new User(command, passwordEncoder);

        // then
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "teacher")
                .hasFieldOrPropertyWithValue("email", "teacher@gmail.com")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_TEACHER);
    }

    @Test
    void toDTO_studentUser_correctDTO() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherUser_correctDTO() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "teacher")
                .hasFieldOrPropertyWithValue("email", "teacher@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toUserDetails_studentUser_correctUserDetails() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("username");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .first()
                .satisfies(authority -> assertThat(authority.getAuthority()).isEqualTo("ROLE_STUDENT"));
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void toUserDetails_teacherUser_correctUserDetails() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("teacher");
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .first()
                .satisfies(authority -> assertThat(authority.getAuthority()).isEqualTo("ROLE_TEACHER"));
    }

    @Test
    void constructor_validCommand_passwordEncoderCalledWithRawPassword() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("my-raw-password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        new User(command, passwordEncoder);

        // then
        verify(passwordEncoder).encode("my-raw-password");
    }

    @Test
    void toUserDetails_teacherUser_accountFlagsAllTrue() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void toDTO_studentUser_dtoFieldsMatchCommand() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("user1")
                .email("user1@example.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("user1");
        assertThat(dto.email()).isEqualTo("user1@example.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void constructor_validCommand_passwordStoredAsEncoded() {
        // given
        when(passwordEncoder.encode("my-raw-password")).thenReturn("my-encoded-password");
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("my-raw-password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final User user = new User(command, passwordEncoder);

        // then
        final UserDetails userDetails = user.toUserDetails();
        assertThat(userDetails.getPassword()).isEqualTo("my-encoded-password");
    }

    @Test
    void toDTO_studentUser_doesNotContainPassword() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();
        final String dtoStr = dto.toString();

        // then — DTO should not expose password
        assertThat(dtoStr).doesNotContain("password");
        assertThat(dtoStr).doesNotContain("encoded-password");
    }

    @Test
    void toUserDetails_studentUser_usernameMatchesCommand() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("specific-user")
                .email("specific@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("specific-user");
        assertThat(userDetails.getUsername()).isNotEqualTo("different-user");
    }

    @Test
    void toDTO_studentUser_roleIsNotNull() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then — important: ROLE_ADMIN.toDTO() returns null, so verify student/teacher always returns non-null
        assertThat(dto.role()).isNotNull();
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherUser_roleIsNotNull() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.role()).isNotNull();
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toUserDetails_studentUser_singleAuthority() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getAuthorities()).hasSize(1);
    }

    @Test
    void toDTO_calledTwice_returnsEqualInstances() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto1 = user.toDTO();
        final UserDTO dto2 = user.toDTO();

        // then
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    void toUserDetails_calledTwice_returnsConsistentValues() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails details1 = user.toUserDetails();
        final UserDetails details2 = user.toUserDetails();

        // then
        assertThat(details1.getUsername()).isEqualTo(details2.getUsername());
        assertThat(details1.getPassword()).isEqualTo(details2.getPassword());
        assertThat(details1.getAuthorities())
                .extracting(auth -> auth.getAuthority())
                .containsExactlyElementsOf(
                        details2.getAuthorities().stream()
                                .map(auth -> auth.getAuthority())
                                .toList()
                );
    }
}
