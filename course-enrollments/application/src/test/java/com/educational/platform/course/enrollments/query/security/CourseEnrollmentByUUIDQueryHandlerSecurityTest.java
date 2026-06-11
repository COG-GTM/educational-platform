package com.educational.platform.course.enrollments.query.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQuery;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQueryHandler;

@Sql(scripts = "classpath:course.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class CourseEnrollmentByUUIDQueryHandlerSecurityTest {

	@MockitoSpyBean
	private CourseEnrollmentByUUIDQueryHandler sut;

	@Test
	@WithMockUser(roles = "TEACHER")
	void handle_userIsTeacher_accessDeniedException() {
		// given
		var query = new CourseEnrollmentByUUIDQuery(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));

		// when
		final ThrowingCallable queryAction = () -> sut.handle(query);

		// then
		assertThatThrownBy(queryAction).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(username = "student", roles = "STUDENT")
	void handle_userIsStudent_accessAllowed() {
		// given
		var query = new CourseEnrollmentByUUIDQuery(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));

		// when
		final Optional<CourseEnrollmentDTO> result = sut.handle(query);

		// then
		assertThat(result).isNotNull();
	}

	@Test
	@WithMockUser(roles = {})
	void handle_userHasNoRoles_accessDeniedException() {
		// given
		var query = new CourseEnrollmentByUUIDQuery(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));

		// when
		final ThrowingCallable queryAction = () -> sut.handle(query);

		// then
		assertThatThrownBy(queryAction).isInstanceOf(AccessDeniedException.class);
	}
}
