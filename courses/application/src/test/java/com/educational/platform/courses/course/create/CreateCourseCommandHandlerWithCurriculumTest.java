package com.educational.platform.courses.course.create;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseFactory;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.CurrentUserAsTeacher;
import com.educational.platform.courses.teacher.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link CreateCourseCommandHandler} when curriculum items (lectures) are provided.
 */
@ExtendWith(MockitoExtension.class)
public class CreateCourseCommandHandlerWithCurriculumTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    private CreateCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final CourseFactory courseFactory = new CourseFactory(validator, currentUserAsTeacher);
        sut = new CreateCourseCommandHandler(repository, courseFactory);

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(1);
    }

    @Test
    void handle_commandWithLectureCurriculumItem_savesCourseWithCurriculum() {
        // given
        final CreateLectureCommand lectureItem = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("Introduction")
                .serialNumber(1)
                .text("Lecture content")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course With Curriculum")
                .description("Has items")
                .curriculumItems(List.of(lectureItem))
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("name", "Course With Curriculum");
    }

    @Test
    void handle_commandWithEmptyCurriculumItems_savesCourseWithEmptyList() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Empty Curriculum")
                .description("No items")
                .curriculumItems(List.of())
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("name", "Empty Curriculum");
    }
}
