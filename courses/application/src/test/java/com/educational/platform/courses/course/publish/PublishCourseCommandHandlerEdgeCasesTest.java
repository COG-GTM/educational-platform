package com.educational.platform.courses.course.publish;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PublishCourseCommandHandlerEdgeCasesTest {

    @Mock
    private CourseRepository repository;

    private PublishCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new PublishCourseCommandHandler(repository);
    }

    @Test
    void handle_courseNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(new PublishCourseCommand(uuid));

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_courseNotApproved_courseCannotBePublishedException() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(new PublishCourseCommand(uuid));

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(handle);
    }
}
