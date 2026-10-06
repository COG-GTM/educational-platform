package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CompletionStatus;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentPageDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseEnrollmentsQueryHandlerTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseEnrollmentRepository repository;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    private Student student;
    private CourseEnrollment enrollment;

    private ListCourseEnrollmentsQueryHandler sut;

    @BeforeEach
    void setUp() {
        student = new Student(new CreateStudentCommand("username"));
        enrollment = new CourseEnrollment(new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(LECTURE, "Intro", 1)))), student);
        sut = new ListCourseEnrollmentsQueryHandler(repository, currentUserAsStudent);
    }

    @Test
    void handle_noStatusFilter_everyEnrollmentSortedByMostRecentActivity() {
        // given
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findByStudent(eq(student), pageable.capture()))
                .thenReturn(new PageImpl<>(List.of(enrollment), PageRequest.of(1, 5), 6));
        when(repository.countByStudentAndArchivedFalseAndCompletionStatus(student, CompletionStatus.IN_PROGRESS)).thenReturn(4L);
        when(repository.countByStudentAndArchivedFalseAndCompletionStatus(student, CompletionStatus.COMPLETED)).thenReturn(2L);
        when(repository.countByStudentAndArchivedTrue(student)).thenReturn(1L);

        // when
        final CourseEnrollmentPageDTO result = sut.handle(new ListCourseEnrollmentsQuery(null, 1, 5));

        // then
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("lastActivityAt"), Sort.Order.desc("id")));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).uuid()).isEqualTo(enrollment.getUuid());
        assertThat(result.items().get(0).courseName()).isEqualTo("Java Basics");
        assertThat(result.items().get(0).completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(6);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.counts().inProgress()).isEqualTo(4);
        assertThat(result.counts().completed()).isEqualTo(2);
        assertThat(result.counts().archived()).isEqualTo(1);
    }

    @Test
    void handle_inProgressFilter_activeInProgressEnrollmentsQueried() {
        // given
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByStudentAndArchivedFalseAndCompletionStatus(eq(student), eq(CompletionStatus.IN_PROGRESS), any(Pageable.class)))
                .thenReturn(page(enrollment));

        // when
        final CourseEnrollmentPageDTO result = sut.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.IN_PROGRESS, 0, 10));

        // then
        assertThat(result.items()).extracting("uuid").containsExactly(enrollment.getUuid());
        verify(repository, never()).findByStudent(any(), any());
        verify(repository, never()).findByStudentAndArchivedTrue(any(), any());
    }

    @Test
    void handle_completedFilter_activeCompletedEnrollmentsQueried() {
        // given
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByStudentAndArchivedFalseAndCompletionStatus(eq(student), eq(CompletionStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(page(enrollment));

        // when
        final CourseEnrollmentPageDTO result = sut.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.COMPLETED, 0, 10));

        // then
        assertThat(result.items()).extracting("uuid").containsExactly(enrollment.getUuid());
        verify(repository, never()).findByStudentAndArchivedFalseAndCompletionStatus(any(), eq(CompletionStatus.IN_PROGRESS), any());
    }

    @Test
    void handle_archivedFilter_archivedEnrollmentsQueried() {
        // given
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByStudentAndArchivedTrue(eq(student), any(Pageable.class))).thenReturn(page(enrollment));

        // when
        final CourseEnrollmentPageDTO result = sut.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.ARCHIVED, 0, 10));

        // then
        assertThat(result.items()).extracting("uuid").containsExactly(enrollment.getUuid());
        verify(repository, never()).findByStudent(any(), any());
        verify(repository, never()).findByStudentAndArchivedFalseAndCompletionStatus(any(), any(), any());
    }

    @Test
    void handle_emptyPage_emptyItemsAndZeroTotals() {
        // given
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByStudentAndArchivedTrue(eq(student), any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0, 12)));

        // when
        final CourseEnrollmentPageDTO result = sut.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.ARCHIVED, 0, 12));

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(12);
    }

    @Test
    void handle_currentUserHasNoStudent_emptyPageWithoutQueryingRepository() {
        // given
        when(currentUserAsStudent.userAsStudent()).thenReturn(null);

        // when
        final CourseEnrollmentPageDTO result = sut.handle(new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.IN_PROGRESS, 2, 10));

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.counts().inProgress()).isZero();
        assertThat(result.counts().completed()).isZero();
        assertThat(result.counts().archived()).isZero();
        verifyNoInteractions(repository);
    }

    private static Page<CourseEnrollment> page(CourseEnrollment... enrollments) {
        return new PageImpl<>(List.of(enrollments), PageRequest.of(0, 10), enrollments.length);
    }
}
