package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

/**
 * Pins the JPA optimistic-lock <em>mapping</em> this PR introduced, as opposed to its emergent behaviour. Every
 * other test in the module exercises a consequence of {@code @Version} (a fresh row starts at 0, an update bumps
 * the version, a stale write fails). They do so by reading a field named {@code version} reflectively, which would
 * still pass if that field were a plain column with the {@code @Version} accidentally dropped or moved to a
 * different field/type. This test closes that gap by asserting the mapping itself through the JPA metamodel and
 * {@link PersistenceUnitUtil}: each of the three entities declares a managed optimistic-lock version attribute named
 * {@code version} of type {@link Integer} (the deliberate {@code Integer} field over a {@code BIGINT} column the PR
 * documents), and the persistence provider treats that exact field as the version it manages.
 */
@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class CourseReviewVersionMappingTest {

	private static final UUID COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private CourseReviewRepository courseReviewRepository;

	@Autowired
	private ReviewerRepository reviewerRepository;

	@Autowired
	private ReviewableCourseRepository reviewableCourseRepository;

	@Test
	void courseReview_entityMapping_declaresIntegerOptimisticLockVersion() {
		assertVersionAttribute(CourseReview.class);
	}

	@Test
	void reviewer_entityMapping_declaresIntegerOptimisticLockVersion() {
		assertVersionAttribute(Reviewer.class);
	}

	@Test
	void reviewableCourse_entityMapping_declaresIntegerOptimisticLockVersion() {
		assertVersionAttribute(ReviewableCourse.class);
	}

	@Test
	void persistenceProvider_treatsVersionFieldAsManagedVersionForEachEntity() {
		// given - one persisted instance of each entity (the seeded review plus a fresh reviewer and course)
		final CourseReview courseReview = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final Reviewer reviewer = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("mapping-reviewer")));
		final ReviewableCourse reviewableCourse = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID())));

		// when - the provider is asked for each entity's version
		final PersistenceUnitUtil persistenceUnitUtil = entityManagerFactory.getPersistenceUnitUtil();

		// then - the provider reports a version for each entity, and it is exactly the value held in the
		// reflectively-read "version" field. This is what the field-only behaviour tests cannot prove: the
		// field the provider manages as the optimistic-lock version is that field, not a coincidentally
		// same-named property the @Version could have drifted away from.
		assertThat(persistenceUnitUtil.getVersion(courseReview))
				.isEqualTo(ReflectionTestUtils.getField(courseReview, "version"));
		assertThat(persistenceUnitUtil.getVersion(reviewer))
				.isEqualTo(ReflectionTestUtils.getField(reviewer, "version"));
		assertThat(persistenceUnitUtil.getVersion(reviewableCourse))
				.isEqualTo(ReflectionTestUtils.getField(reviewableCourse, "version"));
	}

	private void assertVersionAttribute(Class<?> entityClass) {
		final Metamodel metamodel = entityManagerFactory.getMetamodel();
		final EntityType<?> entityType = metamodel.entity(entityClass);

		// the entity carries a JPA optimistic-lock version attribute (the @Version this PR added), declared on
		// the entity itself rather than inherited - getDeclaredVersion throws if it is absent or not Integer-typed
		assertThat(entityType.hasVersionAttribute()).isTrue();
		final SingularAttribute<?, Integer> version = entityType.getDeclaredVersion(Integer.class);

		// the attribute is the "version" field, is flagged as the version, and is typed Integer
		assertThat(version.getName()).isEqualTo("version");
		assertThat(version.isVersion()).isTrue();
		assertThat(version.getJavaType()).isEqualTo(Integer.class);
	}
}
