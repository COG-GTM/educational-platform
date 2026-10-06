package com.educational.platform.course.enrollments.archive.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.enrollments.archive.ArchiveCourseEnrollmentCommand;
import com.educational.platform.course.enrollments.archive.ArchiveCourseEnrollmentCommandHandler;
import com.educational.platform.course.enrollments.archive.RestoreCourseEnrollmentCommand;
import com.educational.platform.course.enrollments.archive.RestoreCourseEnrollmentCommandHandler;

@Sql(scripts = "classpath:course.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class ArchiveCourseEnrollmentCommandHandlerSecurityTest {

	@MockitoSpyBean
	private ArchiveCourseEnrollmentCommandHandler archive;

	@MockitoSpyBean
	private RestoreCourseEnrollmentCommandHandler restore;

	@Test
	@WithMockUser(roles = "TEACHER")
	void archive_userIsTeacher_accessDeniedException() {
		assertThatThrownBy(() -> archive.handle(new ArchiveCourseEnrollmentCommand(UUID.randomUUID())))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(roles = "TEACHER")
	void restore_userIsTeacher_accessDeniedException() {
		assertThatThrownBy(() -> restore.handle(new RestoreCourseEnrollmentCommand(UUID.randomUUID())))
				.isInstanceOf(AccessDeniedException.class);
	}
}
