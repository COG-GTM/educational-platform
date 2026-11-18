package com.educational.platform.course.reviews.enrollment.create;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.enrollment.ReviewerEnrollmentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Sql(scripts = "classpath:course_review.sql")
@SpringBootTest
public class StudentEnrolledToCourseIntegrationEventHandlerIntegrationTest {

    private final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ReviewerEnrollmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void handle_eventPublishedInCommittedTransaction_reviewerEnrollmentCreated() {
        // when
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new StudentEnrolledToCourseIntegrationEvent(courseId, "committed-student")));

        // then
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(repository.existsByCourseIdAndUsername(courseId, "committed-student")).isTrue());
    }

    @Test
    void handle_eventPublishedInRolledBackTransaction_reviewerEnrollmentNotCreated() throws InterruptedException {
        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new StudentEnrolledToCourseIntegrationEvent(courseId, "rolled-back-student"));
            status.setRollbackOnly();
        });

        // then
        Thread.sleep(500);
        assertThat(repository.existsByCourseIdAndUsername(courseId, "rolled-back-student")).isFalse();
    }

    @Test
    void handle_sameEventPublishedTwiceInOneTransaction_singleReviewerEnrollmentCreated() throws InterruptedException {
        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new StudentEnrolledToCourseIntegrationEvent(courseId, "twice-enrolled-student"));
            eventPublisher.publishEvent(new StudentEnrolledToCourseIntegrationEvent(courseId, "twice-enrolled-student"));
        });

        // then
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(enrollmentCount("twice-enrolled-student")).isEqualTo(1));
        Thread.sleep(500);
        assertThat(enrollmentCount("twice-enrolled-student")).isEqualTo(1);
    }

    @Test
    void handle_eventPublishedWithoutTransaction_reviewerEnrollmentCreated() {
        // when
        eventPublisher.publishEvent(new StudentEnrolledToCourseIntegrationEvent(courseId, "no-tx-student"));

        // then
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(repository.existsByCourseIdAndUsername(courseId, "no-tx-student")).isTrue());
    }

    private Integer enrollmentCount(String username) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM reviewer_enrollment WHERE course_id = ? AND username = ?",
                Integer.class, courseId, username);
    }
}
