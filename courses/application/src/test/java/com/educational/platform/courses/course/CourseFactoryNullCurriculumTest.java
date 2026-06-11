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
import static org.mockito.Mockito.when;

/**
 * Tests {@link CourseFactory#createFrom} when curriculum items list is null or empty.
 * Ensures Course construction handles both cases without NPE.
 */
@ExtendWith(MockitoExtension.class)
public class CourseFactoryNullCurriculumTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private Teacher teacher;

    private CourseFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_nullCurriculumItems_courseCreatedWithNullItems() {
        // given
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(1);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(null)
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course).isNotNull();
        assertThat(course.toIdentity()).isNotNull();
        assertThat(course).hasFieldOrPropertyWithValue("curriculumItems", null);
    }

    @Test
    void createFrom_noCurriculumItemsSet_courseCreatedSuccessfully() {
        // given — curriculumItems not set at all (defaults to null)
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(1);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Minimal Course")
                .description("No items")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course).isNotNull();
        assertThat(course).hasFieldOrPropertyWithValue("name", "Minimal Course");
    }

    @Test
    void createFrom_validCommand_generatesUniqueUuid() {
        // given
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(1);
        final CreateCourseCommand command1 = CreateCourseCommand.builder()
                .name("Course 1").description("desc").build();
        final CreateCourseCommand command2 = CreateCourseCommand.builder()
                .name("Course 2").description("desc").build();

        // when
        final Course course1 = sut.createFrom(command1);
        final Course course2 = sut.createFrom(command2);

        // then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }
}
