package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseRatingRecalculatedIntegrationEventHandlerEdgeCaseTest {

    @Mock
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @InjectMocks
    private CourseRatingRecalculatedIntegrationEventHandler sut;

    @Test
    void handleCourseRatingRecalculatedEvent_zeroRating_commandPassedWithZeroRating() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 0.0);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> argument = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 0.0);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_maxRating_commandPassedWithMaxRating() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 5.0);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> argument = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 5.0);
    }
}
