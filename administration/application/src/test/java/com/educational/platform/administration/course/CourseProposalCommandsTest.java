package com.educational.platform.administration.course;

import com.educational.platform.administration.course.approve.ApproveCourseProposalCommand;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommand;
import com.educational.platform.administration.course.query.ListCourseProposalsQuery;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalCommandsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void approveCourseProposalCommand_exposesUuid() {
        assertThat(new ApproveCourseProposalCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void createCourseProposalCommand_exposesUuid() {
        assertThat(new CreateCourseProposalCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void declineCourseProposalCommand_exposesUuid() {
        assertThat(new DeclineCourseProposalCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void listCourseProposalsQuery_equalInstances() {
        assertThat(new ListCourseProposalsQuery()).isEqualTo(new ListCourseProposalsQuery());
    }

    @Test
    void courseApprovedByAdminIntegrationEvent_exposesCourseId() {
        assertThat(new CourseApprovedByAdminIntegrationEvent(UUID_VALUE).courseId()).isEqualTo(UUID_VALUE);
    }

    @Test
    void courseDeclinedByAdminIntegrationEvent_exposesCourseId() {
        assertThat(new CourseDeclinedByAdminIntegrationEvent(UUID_VALUE).courseId()).isEqualTo(UUID_VALUE);
    }
}
