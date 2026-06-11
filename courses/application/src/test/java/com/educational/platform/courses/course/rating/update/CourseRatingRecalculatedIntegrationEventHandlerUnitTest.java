package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
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
public class CourseRatingRecalculatedIntegrationEventHandlerUnitTest {

    @Mock
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    private CourseRatingRecalculatedIntegrationEventHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CourseRatingRecalculatedIntegrationEventHandler(updateCourseRatingCommandHandler);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_delegatesToCommandHandler() {
        // given
        final UUID courseId = UUID.randomUUID();
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.2);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(courseId);
        assertThat(captor.getValue().rating()).isEqualTo(4.2);
    }
}
