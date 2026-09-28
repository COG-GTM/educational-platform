package com.educational.platform.course.reviews.jpa;

import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class ReviewerRepositoryTest {

	public static final String REVIEWER_USERNAME = "reviewer";

	@Autowired
	private ReviewerRepository sut;

	@Test
	void findByUsername_existingReviewer_reviewerReturned() {
		// given/when
		final Optional<Reviewer> result = sut.findByUsername(REVIEWER_USERNAME);

		// then
		assertThat(result).isPresent();
		assertThat(result.get())
				.hasFieldOrPropertyWithValue("username", REVIEWER_USERNAME)
				.extracting(Reviewer::getId).isNotNull();
	}

	@Test
	void findByUsername_unknownReviewer_empty() {
		// given/when
		final Optional<Reviewer> result = sut.findByUsername("unknown-reviewer");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void findByUsername_usernameDiffersByCase_empty() {
		// given/when
		final Optional<Reviewer> result = sut.findByUsername("REVIEWER");

		// then
		assertThat(result).isEmpty();
	}
}
