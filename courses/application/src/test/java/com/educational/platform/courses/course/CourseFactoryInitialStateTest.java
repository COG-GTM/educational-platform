package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseFactoryInitialStateTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    private CourseFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_validCommand_allInitialFieldsSetCorrectly() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0))
                .hasFieldOrPropertyWithValue("teacher", 15);
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void createFrom_differentTeacher_usesCorrectTeacherId() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(42);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("another course")
                .description("another description")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("teacher", 42);
    }

    @Test
    void createFrom_twoInvocations_generateDistinctUuids() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(1);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course1 = sut.createFrom(command);
        final Course course2 = sut.createFrom(command);

        // then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }
}
