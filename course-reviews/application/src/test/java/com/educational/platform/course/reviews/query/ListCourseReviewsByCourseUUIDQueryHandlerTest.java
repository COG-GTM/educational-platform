package com.educational.platform.course.reviews.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;

/**
 * Exercises the bounded context's only read use case, {@link ListCourseReviewsByCourseUUIDQueryHandler}, against the
 * {@code @Version} columns this PR added. The read projection joins all three versioned tables
 * (course_review -&gt; reviewable_course -&gt; reviewer), so the PR's claim that "no service/repository code needed to
 * change" has to hold at the query-handler boundary too. Every existing read assertion goes straight to the
 * repository ({@code CourseReviewRepositoryTest}, the {@code listCourseReviews} cases in {@code OptimisticLockingTest}),
 * leaving the public query use case itself untested; these tests pin it: it projects the full review and never leaks
 * the internal optimistic-lock version, including once every joined row has advanced past its initial version.
 */
@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class ListCourseReviewsByCourseUUIDQueryHandlerTest {

	private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
	private static final UUID COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private CourseReviewRepository courseReviewRepository;

	@Autowired
	private ReviewableCourseRepository reviewableCourseRepository;

	@Autowired
	private ReviewerRepository reviewerRepository;

	@Test
	void handle_seededCourse_projectsReviewThroughUseCaseWithoutLeakingVersion() {
		// given - the production read use case wired with the real repository (the module never tests this handler;
		// every read assertion elsewhere calls the repository query directly)
		final ListCourseReviewsByCourseUUIDQueryHandler handler =
				new ListCourseReviewsByCourseUUIDQueryHandler(courseReviewRepository);

		// when - the seeded course's reviews are read through the public query
		final List<CourseReviewDTO> reviews = handler.handle(new ListCourseReviewsByCourseUUIDQuery(COURSE_UUID));

		// then - the use case projects the seeded review with every field resolved across the three joined tables
		assertThat(reviews).hasSize(1);
		final CourseReviewDTO dto = reviews.get(0);
		assertThat(dto.uuid()).isEqualTo(COURSE_REVIEW_UUID);
		assertThat(dto.course()).isEqualTo(COURSE_UUID);
		assertThat(dto.username()).isEqualTo("reviewer");
		assertThat(dto.comment()).isEqualTo("comment");
		assertThat(dto.rating()).isEqualTo(4.0);

		// and - the internal @Version this PR added is not part of the read contract: the DTO type returned by the
		// use case declares no version component, so the optimistic-lock column cannot leak through the read side
		assertThat(CourseReviewDTO.class.getRecordComponents())
				.extracting(RecordComponent::getName)
				.doesNotContain("version");
	}

	@Test
	void handle_afterVersionBumpsAcrossAllJoinedRows_stillProjectsEveryFieldThroughUseCase() {
		// given - each of the three rows the read projection joins is mutated once, advancing every table this PR
		// versioned to version 1 (reviewable_course's originalCourseId, the reviewer's username, the review itself).
		// The foreign keys are untouched, so the joins still match.
		final UUID newOriginalCourseId = UUID.randomUUID();
		final ReviewableCourse course = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", newOriginalCourseId);
		reviewableCourseRepository.saveAndFlush(course);

		final Reviewer reviewer = reviewerRepository.findByUsername("reviewer");
		ReflectionTestUtils.setField(reviewer, "username", "reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// and - all three joined rows really sit at version 1 now
		assertThat(ReflectionTestUtils.getField(
				reviewableCourseRepository.findByOriginalCourseId(newOriginalCourseId).orElseThrow(), "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reviewerRepository.findByUsername("reviewer-renamed"), "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(
				courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow(), "version")).isEqualTo(1);

		// when - the reviews are read through the public query use case after every joined table advanced its version
		final ListCourseReviewsByCourseUUIDQueryHandler handler =
				new ListCourseReviewsByCourseUUIDQueryHandler(courseReviewRepository);
		final List<CourseReviewDTO> reviews = handler.handle(new ListCourseReviewsByCourseUUIDQuery(newOriginalCourseId));

		// then - the use case still projects every field to its now-current value; the added version columns do not
		// corrupt the read join when the joined rows sit at a non-zero version
		assertThat(reviews).hasSize(1);
		final CourseReviewDTO dto = reviews.get(0);
		assertThat(dto.uuid()).isEqualTo(COURSE_REVIEW_UUID);
		assertThat(dto.course()).isEqualTo(newOriginalCourseId);
		assertThat(dto.username()).isEqualTo("reviewer-renamed");
		assertThat(dto.comment()).isEqualTo("updated comment");
		assertThat(dto.rating()).isEqualTo(5.0);
	}
}
