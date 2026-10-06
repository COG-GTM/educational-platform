package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LOCAL_DATE_TIME;

public class CourseReviewTest {

	private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	@Test
	void create_validCommand_createdDateSetToNow() {
		// given
		final LocalDateTime before = LocalDateTime.now();
		final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_UUID, 4.0, "comment");

		// when
		final CourseReview review = new CourseReview(command, 11, 22);

		// then
		final LocalDateTime after = LocalDateTime.now();
		assertThat(review).extracting("createdDate", LOCAL_DATE_TIME)
				.isAfterOrEqualTo(before)
				.isBeforeOrEqualTo(after);
		assertThat(review)
				.hasFieldOrPropertyWithValue("course", 11)
				.hasFieldOrPropertyWithValue("reviewer", 22);
	}

	@Test
	void update_validCommand_createdDateUnchanged() {
		// given
		final CourseReview review = new CourseReview(new ReviewCourseCommand(COURSE_UUID, 4.0, "comment"), 11, 22);
		final Object createdDate = ReflectionTestUtils.getField(review, "createdDate");

		// when
		review.update(new UpdateCourseReviewCommand(review.toIdentifier(), 2.0, "updated"));

		// then
		assertThat(createdDate).isNotNull();
		assertThat(review).hasFieldOrPropertyWithValue("createdDate", createdDate);
	}
}
