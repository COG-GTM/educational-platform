package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateEnrollmentCourseCommandHandlerTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
    private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

    @Mock
    private EnrollCourseRepository courseRepository;

    @InjectMocks
    private CreateEnrollmentCourseCommandHandler sut;

    @Test
    void handle_unknownCourse_newCourseWithLecturesSaved() {
        // given
        when(courseRepository.findByUuid(COURSE)).thenReturn(Optional.empty());

        // when
        sut.handle(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1))));

        // then
        final ArgumentCaptor<EnrollCourse> saved = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(courseRepository).save(saved.capture());
        assertThat(saved.getValue().toReference()).isEqualTo(COURSE);
        assertThat(saved.getValue().getName()).isEqualTo("Java Basics");
        assertThat(saved.getValue().getLectures()).extracting("uuid").containsExactly(FIRST_LECTURE);
    }

    @Test
    void handle_knownCourse_existingInstanceRefreshedAndSaved() {
        // given
        final EnrollCourse existing = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1))));
        when(courseRepository.findByUuid(COURSE)).thenReturn(Optional.of(existing));

        // when
        sut.handle(new CreateCourseCommand(COURSE, "Java Basics v2", List.of(
                new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
                new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2))));

        // then
        verify(courseRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("Java Basics v2");
        assertThat(existing.getLectures()).extracting("uuid").containsExactly(FIRST_LECTURE, SECOND_LECTURE);
    }
}
