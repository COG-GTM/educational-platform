package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that consecutive handler-level calls to {@link IncreaseNumberOfStudentsCommandHandler}
 * correctly accumulate the student count when the repository returns the same (mutated) instance.
 */
@ExtendWith(MockitoExtension.class)
public class IncreaseNumberOfStudentsCommandHandlerConsecutiveTest {

    @Mock
    private CourseRepository repository;

    private IncreaseNumberOfStudentsCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new IncreaseNumberOfStudentsCommandHandler(repository);
    }

    @Test
    void handle_calledThreeTimes_numberOfStudentsIsThree() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new IncreaseNumberOfStudentsCommand(uuid));
        sut.handle(new IncreaseNumberOfStudentsCommand(uuid));
        sut.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(3)).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3));
    }
}
