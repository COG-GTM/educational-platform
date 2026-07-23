package com.educational.platform.messaging;

import java.util.UUID;

import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.approve.ApproveCourseProposalCommand;
import com.educational.platform.administration.course.approve.ApproveCourseProposalCommandHandler;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.courses.course.ApprovalStatus;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the messaging flow of the course approval process between the courses and
 * administration bounded contexts: {@link SendCourseToApproveIntegrationEvent} creates a course
 * proposal in the administration context, and approving the proposal publishes
 * {@link CourseApprovedByAdminIntegrationEvent} which approves the course in the courses context.
 */
@Sql(scripts = "classpath:messaging/course_approval_flow.sql")
public class CourseApprovalIntegrationEventFlowTest extends MessagingIntegrationTestSupport {

	private final UUID courseId = UUID.fromString("223e4567-e89b-12d3-a456-426655440101");

	@Autowired
	private CourseProposalRepository courseProposalRepository;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private ApproveCourseProposalCommandHandler approveCourseProposalCommandHandler;

	@Test
	void sendCourseToApproveEvent_courseProposalCreatedInAdministrationContext() {
		// when
		eventPublisher.publishEvent(new SendCourseToApproveIntegrationEvent(courseId));

		// then
		awaitAsyncListeners(() ->
				assertThat(courseProposalRepository.findByUuid(courseId)).isPresent());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void approveCourseProposal_courseApprovedEventPublished_courseApprovedInCoursesContext() {
		// given
		eventPublisher.publishEvent(new SendCourseToApproveIntegrationEvent(courseId));
		awaitAsyncListeners(() ->
				assertThat(courseProposalRepository.findByUuid(courseId)).isPresent());

		// when
		approveCourseProposalCommandHandler.handle(new ApproveCourseProposalCommand(courseId));

		// then
		assertThat(integrationEvents.eventsOfType(CourseApprovedByAdminIntegrationEvent.class))
				.contains(new CourseApprovedByAdminIntegrationEvent(courseId));

		awaitAsyncListeners(() ->
				assertThat(courseRepository.findByUuid(courseId).orElseThrow())
						.hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED));
	}
}
