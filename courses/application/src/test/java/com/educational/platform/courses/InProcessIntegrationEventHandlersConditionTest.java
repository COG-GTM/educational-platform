package com.educational.platform.courses;

import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.create.CreateTeacherCommandHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@code courses.in-process-handlers.enabled} lets the Java integration-event handlers be switched off once
 * {@code courses-py} consumes the same events from the broker (strangler-fig cutover).
 */
class InProcessIntegrationEventHandlersConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    CommandHandlerStubs.class,
                    CourseApprovedByAdminIntegrationEventHandler.class,
                    StudentEnrolledToCourseIntegrationEventHandler.class,
                    UserCreatedIntegrationEventHandler.class,
                    CourseRatingRecalculatedIntegrationEventHandler.class);

    @Test
    void propertyMissing_handlersEnabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CourseApprovedByAdminIntegrationEventHandler.class);
            assertThat(context).hasSingleBean(StudentEnrolledToCourseIntegrationEventHandler.class);
            assertThat(context).hasSingleBean(UserCreatedIntegrationEventHandler.class);
            assertThat(context).hasSingleBean(CourseRatingRecalculatedIntegrationEventHandler.class);
        });
    }

    @Test
    void propertyTrue_handlersEnabled() {
        runner.withPropertyValues("courses.in-process-handlers.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(CourseApprovedByAdminIntegrationEventHandler.class);
            assertThat(context).hasSingleBean(StudentEnrolledToCourseIntegrationEventHandler.class);
            assertThat(context).hasSingleBean(UserCreatedIntegrationEventHandler.class);
            assertThat(context).hasSingleBean(CourseRatingRecalculatedIntegrationEventHandler.class);
        });
    }

    @Test
    void propertyFalse_allFourHandlersDisabled() {
        runner.withPropertyValues("courses.in-process-handlers.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(CourseApprovedByAdminIntegrationEventHandler.class);
            assertThat(context).doesNotHaveBean(StudentEnrolledToCourseIntegrationEventHandler.class);
            assertThat(context).doesNotHaveBean(UserCreatedIntegrationEventHandler.class);
            assertThat(context).doesNotHaveBean(CourseRatingRecalculatedIntegrationEventHandler.class);
            // the command handlers themselves stay available to the REST layer / other callers
            assertThat(context).hasSingleBean(ApproveCourseCommandHandler.class);
        });
    }

    @Test
    void propertyOtherValue_handlersDisabled() {
        runner.withPropertyValues("courses.in-process-handlers.enabled=off").run(context ->
                assertThat(context).doesNotHaveBean(CourseApprovedByAdminIntegrationEventHandler.class));
    }

    @Configuration
    static class CommandHandlerStubs {

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return mock(ApproveCourseCommandHandler.class);
        }

        @Bean
        IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler() {
            return mock(IncreaseNumberOfStudentsCommandHandler.class);
        }

        @Bean
        CreateTeacherCommandHandler createTeacherCommandHandler() {
            return mock(CreateTeacherCommandHandler.class);
        }

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler() {
            return mock(UpdateCourseRatingCommandHandler.class);
        }
    }
}
