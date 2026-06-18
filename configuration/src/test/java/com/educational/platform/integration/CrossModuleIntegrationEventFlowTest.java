package com.educational.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
import com.educational.platform.administration.course.approve.ApproveCourseProposalCommand;
import com.educational.platform.administration.course.approve.ApproveCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.course.ApprovalStatus;
import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.approve.SendCourseToApproveCommand;
import com.educational.platform.courses.course.approve.SendCourseToApproveCommandHandler;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.registration.UserRegistrationCommandHandler;

/**
 * Verifies that integration events published in one module are routed, within a
 * live Spring context, to the asynchronous {@code @EventListener} handlers in
 * the consuming module and produce the expected database side-effect.
 *
 * <p>Because every handler is annotated with {@code @Async}, each test publishes
 * the event and then uses Awaitility to poll the consumer module's repository
 * until the side-effect is committed (or the timeout elapses).
 *
 * <p>Security is disabled ({@code com.educational.platform.security.enabled=false})
 * since these tests exercise event routing, not authorization. Disabling
 * security removes the {@link PasswordEncoder} and {@link AuthenticationManager}
 * beans that the users module wires unconditionally, so {@link TestSecurityConfig}
 * supplies inert replacements purely to keep the context bootable. It also
 * disables method security, so the {@code @PreAuthorize} guards on the producer
 * command handlers exercised below are not enforced.
 *
 * <p>Most tests publish the integration event directly to verify the consumer in
 * isolation. The {@code ...PublishesEventAnd...} tests instead invoke the real
 * producer command handler so the full producer -> event -> consumer chain is
 * covered end-to-end: a regression in either the producer's publish call or the
 * event's cross-module payload contract would fail them.
 *
 * <p>TODO: {@code CourseDeclinedByAdminIntegrationEvent} (published by the
 * administration module's {@code DeclineCourseProposalCommandHandler}) has no
 * {@code @EventListener} in the courses module — {@code Course.decline()} is
 * never invoked by any handler. Once a consumer is implemented, add a flow test
 * here asserting the course's approvalStatus becomes {@code DECLINED}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "com.educational.platform.security.enabled=false")
@Sql(scripts = "classpath:integration-event-test-data.sql")
class CrossModuleIntegrationEventFlowTest {

    private static final UUID SEND_TO_APPROVE_COURSE_UUID =
            UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID APPROVE_COURSE_UUID =
            UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
    private static final UUID ENROLL_COURSE_UUID =
            UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
    private static final UUID RATING_COURSE_UUID =
            UUID.fromString("123e4567-e89b-12d3-a456-426655440004");

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        AuthenticationManager authenticationManager() {
            return authentication -> authentication;
        }
    }

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseProposalRepository courseProposalRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRegistrationCommandHandler userRegistrationCommandHandler;

    @Autowired
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    @Autowired
    private ApproveCourseProposalCommandHandler approveCourseProposalCommandHandler;

    @Autowired
    private SendCourseToApproveCommandHandler sendCourseToApproveCommandHandler;

    @Test
    void sendCourseToApproveIntegrationEventCreatesCourseProposal() {
        applicationEventPublisher.publishEvent(
                new SendCourseToApproveIntegrationEvent(SEND_TO_APPROVE_COURSE_UUID));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Optional<CourseProposal> proposal =
                    courseProposalRepository.findByUuid(SEND_TO_APPROVE_COURSE_UUID);
            assertThat(proposal).isPresent();
            assertThat(proposal.get())
                    .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
        });
    }

    @Test
    void courseApprovedByAdminIntegrationEventApprovesCourse() {
        applicationEventPublisher.publishEvent(
                new CourseApprovedByAdminIntegrationEvent(APPROVE_COURSE_UUID));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Optional<Course> course = courseRepository.findByUuid(APPROVE_COURSE_UUID);
            assertThat(course).isPresent();
            assertThat(course.get())
                    .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
        });
    }

    @Test
    void studentEnrolledToCourseIntegrationEventIncreasesNumberOfStudents() {
        applicationEventPublisher.publishEvent(
                new StudentEnrolledToCourseIntegrationEvent(ENROLL_COURSE_UUID, "student"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Optional<Course> course = courseRepository.findByUuid(ENROLL_COURSE_UUID);
            assertThat(course).isPresent();
            assertThat(course.get())
                    .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
        });
    }

    @Test
    void courseRatingRecalculatedIntegrationEventUpdatesRating() {
        applicationEventPublisher.publishEvent(
                new CourseRatingRecalculatedIntegrationEvent(RATING_COURSE_UUID, 4.5));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Optional<Course> course = courseRepository.findByUuid(RATING_COURSE_UUID);
            assertThat(course).isPresent();
            assertThat(course.get())
                    .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
        });
    }

    @Test
    void userCreatedIntegrationEventCreatesTeacher() {
        applicationEventPublisher.publishEvent(
                new UserCreatedIntegrationEvent("newteacher", "newteacher@test.com"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Teacher teacher = teacherRepository.findByUsername("newteacher");
            assertThat(teacher).isNotNull();
            assertThat(teacher).hasFieldOrPropertyWithValue("username", "newteacher");
        });
    }

    @Test
    void userRegistrationCommandPublishesEventAndCreatesTeacher() {
        // the producer side of flow 5: registering a user must publish a
        // UserCreatedIntegrationEvent carrying the registered username, which the
        // courses module turns into a Teacher projection.
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("registered-teacher")
                .email("registered-teacher@test.com")
                .password("password")
                .build();

        userRegistrationCommandHandler.handle(command);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Teacher teacher = teacherRepository.findByUsername("registered-teacher");
            assertThat(teacher).isNotNull();
            assertThat(teacher).hasFieldOrPropertyWithValue("username", "registered-teacher");
        });
    }

    @Test
    void sendCourseToApproveCommandPublishesEventAndCreatesProposal() {
        // the producer side of flow 1: sending a course to approve must publish a
        // SendCourseToApproveIntegrationEvent carrying the course UUID, which the
        // administration module turns into a CourseProposal awaiting approval.
        sendCourseToApproveCommandHandler.handle(new SendCourseToApproveCommand(SEND_TO_APPROVE_COURSE_UUID));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Optional<CourseProposal> proposal =
                    courseProposalRepository.findByUuid(SEND_TO_APPROVE_COURSE_UUID);
            assertThat(proposal).isPresent();
            assertThat(proposal.get())
                    .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
        });
    }

    @Test
    void approveCourseProposalCommandPublishesEventAndApprovesCourse() {
        // the producer side of flow 2: approving a proposal must publish a
        // CourseApprovedByAdminIntegrationEvent carrying the course UUID, which the
        // courses module uses to move the corresponding Course to APPROVED.
        createCourseProposalCommandHandler.handle(new CreateCourseProposalCommand(APPROVE_COURSE_UUID));

        approveCourseProposalCommandHandler.handle(new ApproveCourseProposalCommand(APPROVE_COURSE_UUID));

        final Optional<CourseProposal> proposal = courseProposalRepository.findByUuid(APPROVE_COURSE_UUID);
        assertThat(proposal).isPresent();
        assertThat(proposal.get())
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final Optional<Course> course = courseRepository.findByUuid(APPROVE_COURSE_UUID);
            assertThat(course).isPresent();
            assertThat(course.get())
                    .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
        });
    }
}
