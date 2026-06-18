package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateEnrollmentCourseCommandHandlerTest {

    @Mock
    private EnrollCourseRepository repository;

    @InjectMocks
    private CreateEnrollmentCourseCommandHandler sut;

    @Test
    void handle_validCommand_courseSaved() {
        // given - the enroll-course projection is the enrollments-context copy of a course keyed by the shared uuid
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseCommand command = new CreateCourseCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<EnrollCourse> argument = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(repository).save(argument.capture());
        final EnrollCourse saved = argument.getValue();
        assertThat(saved)
                .hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(saved.toReference()).isEqualTo(uuid);
    }

    @Test
    void handle_nullUuid_courseSavedWithNullUuid() {
        // given - the handler performs no validation; a null uuid is forwarded verbatim to the persisted projection
        final CreateCourseCommand command = new CreateCourseCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<EnrollCourse> argument = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", null);
    }

    @Test
    void handle_repositoryThrows_exceptionPropagated() {
        // given - a persistence failure must propagate rather than be swallowed
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateCourseCommand command = new CreateCourseCommand(uuid);
        doThrow(new RuntimeException("course could not be saved"))
                .when(repository).save(any(EnrollCourse.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("course could not be saved");
    }
}
