package com.educational.platform.course.enrollments.progress.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentArchivedException;
import com.educational.platform.course.enrollments.archive.ArchiveCourseEnrollmentCommand;
import com.educational.platform.course.enrollments.archive.ArchiveCourseEnrollmentCommandHandler;
import com.educational.platform.course.enrollments.archive.RestoreCourseEnrollmentCommand;
import com.educational.platform.course.enrollments.archive.RestoreCourseEnrollmentCommandHandler;
import com.educational.platform.course.enrollments.progress.UpdateLectureProgressCommand;
import com.educational.platform.course.enrollments.progress.UpdateLectureProgressCommandHandler;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQuery;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQueryHandler;
import com.educational.platform.course.enrollments.query.EnrollmentStatusFilter;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;

@Sql(scripts = "classpath:course.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class UpdateLectureProgressCommandHandlerSecurityTest {

	private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

	@Autowired
	private RegisterStudentToCourseCommandHandler registerStudentToCourseCommandHandler;

	@Autowired
	private CourseEnrollmentByUUIDQueryHandler courseEnrollmentByUUIDQueryHandler;

	@Autowired
	private ListCourseEnrollmentsQueryHandler listCourseEnrollmentsQueryHandler;

	@Autowired
	private ArchiveCourseEnrollmentCommandHandler archiveCourseEnrollmentCommandHandler;

	@Autowired
	private RestoreCourseEnrollmentCommandHandler restoreCourseEnrollmentCommandHandler;

	@MockitoSpyBean
	private UpdateLectureProgressCommandHandler sut;

	@Test
	@WithMockUser(username = "student", roles = "STUDENT")
	void handle_userIsStudent_progressTrackedUntilCompletion() {
		// given
		var enrollment = registerStudentToCourseCommandHandler.handle(new RegisterStudentToCourseCommand(COURSE));

		// when
		var afterFirst = sut.handle(new UpdateLectureProgressCommand(enrollment, FIRST_LECTURE, true));

		// then
		assertThat(afterFirst.enrollment().progressPercent()).isEqualTo(50);
		assertThat(afterFirst.enrollment().completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
		assertThat(afterFirst.lectures()).extracting("completed").containsExactly(true, false);

		// when
		var afterSecond = sut.handle(new UpdateLectureProgressCommand(enrollment, SECOND_LECTURE, true));

		// then
		assertThat(afterSecond.enrollment().completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(afterSecond.enrollment().progressPercent()).isEqualTo(100);

		// progress persisted and visible through queries
		var details = courseEnrollmentByUUIDQueryHandler.handle(new CourseEnrollmentByUUIDQuery(enrollment)).orElseThrow();
		assertThat(details.enrollment().completedLectures()).isEqualTo(2);
		var page = listCourseEnrollmentsQueryHandler.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.COMPLETED, 0, 10));
		assertThat(page.items()).extracting("uuid").containsExactly(enrollment);
		assertThat(page.counts().completed()).isEqualTo(1);
		assertThat(page.counts().inProgress()).isZero();

		// when
		var reverted = sut.handle(new UpdateLectureProgressCommand(enrollment, SECOND_LECTURE, false));

		// then
		assertThat(reverted.enrollment().completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
		assertThat(reverted.enrollment().completedAt()).isNull();
	}

	@Test
	@WithMockUser(username = "student", roles = "STUDENT")
	void handle_archivedEnrollment_archivedExceptionUntilRestored() {
		// given
		var enrollment = registerStudentToCourseCommandHandler.handle(new RegisterStudentToCourseCommand(COURSE));
		var archived = archiveCourseEnrollmentCommandHandler.handle(new ArchiveCourseEnrollmentCommand(enrollment));
		assertThat(archived.archived()).isTrue();
		assertThat(listCourseEnrollmentsQueryHandler.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.IN_PROGRESS, 0, 10)).items()).isEmpty();
		assertThat(listCourseEnrollmentsQueryHandler.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.ARCHIVED, 0, 10)).items()).hasSize(1);

		// when
		final ThrowingCallable completeAction = () -> sut.handle(new UpdateLectureProgressCommand(enrollment, FIRST_LECTURE, true));

		// then
		assertThatThrownBy(completeAction).isInstanceOf(CourseEnrollmentArchivedException.class);

		// when
		var restored = restoreCourseEnrollmentCommandHandler.handle(new RestoreCourseEnrollmentCommand(enrollment));

		// then
		assertThat(restored.archived()).isFalse();
		assertThat(sut.handle(new UpdateLectureProgressCommand(enrollment, FIRST_LECTURE, true)).enrollment().completedLectures()).isEqualTo(1);
	}

	@Test
	@WithMockUser(username = "student", roles = "STUDENT")
	void handle_unknownEnrollment_resourceNotFoundException() {
		assertThatThrownBy(() -> sut.handle(new UpdateLectureProgressCommand(UUID.randomUUID(), FIRST_LECTURE, true)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@WithMockUser(roles = "TEACHER")
	void handle_userIsTeacher_accessDeniedException() {
		// when
		final ThrowingCallable action = () -> sut.handle(new UpdateLectureProgressCommand(UUID.randomUUID(), FIRST_LECTURE, true));

		// then
		assertThatThrownBy(action).isInstanceOf(AccessDeniedException.class);
	}
}
