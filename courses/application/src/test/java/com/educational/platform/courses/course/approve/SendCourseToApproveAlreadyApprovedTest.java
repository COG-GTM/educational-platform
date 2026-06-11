package com.educational.platform.courses.course.approve;

import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that sending an already-approved course for approval throws {@link CourseAlreadyApprovedException}.
 */
@ExtendWith(MockitoExtension.class)
public class SendCourseToApproveAlreadyApprovedTest {

    private CourseFactory courseFactory;

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SendCourseToApproveCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseFactory = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void handle_alreadyApprovedCourse_throwsCourseAlreadyApprovedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final Course course = courseFactory.createFrom(
                CreateCourseCommand.builder().name("name").description("desc").build());
        course.approve();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when / then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class)
                .isThrownBy(() -> sut.handle(new SendCourseToApproveCommand(uuid)))
                .satisfies(ex -> assertThat(ex.getMessage()).contains("cannot be sent for approval"));
    }
}
