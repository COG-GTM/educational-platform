package com.educational.platform.course.enrollments.query.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;

@Sql(scripts = "classpath:course.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class ListCourseEnrollmentsQueryHandlerSecurityTest {

	@MockitoSpyBean
	private ListCourseEnrollmentsQueryHandler sut;

	@Test
	@WithMockUser(roles = "TEACHER")
	void handle_userIsTeacher_accessDeniedException() {
		// given
		var query = new ListCourseEnrollmentsQuery();

		// when
		final ThrowingCallable queryAction = () -> sut.handle(query);

		// then
		assertThatThrownBy(queryAction).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(username = "student", roles = "STUDENT")
	void handle_userIsStudent_accessAllowed() {
		// given
		var query = new ListCourseEnrollmentsQuery();

		// when
		final List<CourseEnrollmentDTO> result = sut.handle(query);

		// then
		assertThat(result).isNotNull();
	}

	@Test
	@WithMockUser(roles = {})
	void handle_userHasNoRoles_accessDeniedException() {
		// given
		var query = new ListCourseEnrollmentsQuery();

		// when
		final ThrowingCallable queryAction = () -> sut.handle(query);

		// then
		assertThatThrownBy(queryAction).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void handle_userIsAdmin_accessDeniedException() {
		// given
		var query = new ListCourseEnrollmentsQuery();

		// when
		final ThrowingCallable queryAction = () -> sut.handle(query);

		// then
		assertThatThrownBy(queryAction).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(username = "student", roles = {"STUDENT", "TEACHER"})
	void handle_userHasMultipleRolesIncludingStudent_accessAllowed() {
		// given
		var query = new ListCourseEnrollmentsQuery();

		// when
		final List<CourseEnrollmentDTO> result = sut.handle(query);

		// then — user with STUDENT + TEACHER roles passes hasRole('STUDENT') check
		assertThat(result).isNotNull();
	}

	@Test
	@WithMockUser(username = "student", roles = "STUDENT")
	void handle_noEnrollments_returnsEmptyList() {
		// given
		var query = new ListCourseEnrollmentsQuery();

		// when
		final List<CourseEnrollmentDTO> result = sut.handle(query);

		// then
		assertThat(result).isEmpty();
	}
}
