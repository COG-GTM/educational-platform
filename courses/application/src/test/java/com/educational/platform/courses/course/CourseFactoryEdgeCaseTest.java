package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.teacher.Teacher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseFactoryEdgeCaseTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    private CourseFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_validCommand_courseHasCorrectDefaults() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Java Fundamentals")
                .description("Learn Java from scratch")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Java Fundamentals")
                .hasFieldOrPropertyWithValue("description", "Learn Java from scratch")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0))
                .hasFieldOrPropertyWithValue("teacher", 15);
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void createFrom_commandWithEmptyCurriculumItems_courseCreatedWithEmptyList() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(Collections.emptyList())
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course).extracting("curriculumItems").asList().isEmpty();
    }

    @Test
    void createFrom_commandWithCurriculumItems_courseCreatedWithItems() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Intro")
                .description("Introduction lecture")
                .serialNumber(1)
                .text("Content")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(lectureCommand))
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course).extracting("curriculumItems").asList().hasSize(1);
    }

    @Test
    void createFrom_twoCalls_differentUuids() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
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
