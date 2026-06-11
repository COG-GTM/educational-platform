package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateEnrollmentCourseCommandHandlerTest {

    @Mock
    private EnrollCourseRepository courseRepository;

    private CreateEnrollmentCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateEnrollmentCourseCommandHandler(courseRepository);
    }

    @Test
    void handle_validCommand_savesCourse() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        sut.handle(new CreateCourseCommand(uuid));

        // then
        final ArgumentCaptor<EnrollCourse> captor = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().toReference()).isEqualTo(uuid);
    }
}
