package com.educational.platform.administration.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;

@DataJpaTest
public class CourseProposalRepositoryTest {

	@Autowired
	private CourseProposalRepository sut;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void listCourseProposals_unpaged_courseProposals() {
		// given
		sut.save(new CourseProposal(new CreateCourseProposalCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))));

		// when
		var result = sut.listCourseProposals();

		// then
		assertThat(result).hasSize(1);
	}

	@Test
	void save_newCourseProposal_versionInitializedToZero() {
		// given
		final CourseProposal courseProposal = new CourseProposal(new CreateCourseProposalCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440001")));

		// when
		final CourseProposal saved = sut.saveAndFlush(courseProposal);

		// then
		assertThat(saved).hasFieldOrPropertyWithValue("version", 0);
	}

	@Test
	void save_updatedCourseProposal_versionIncremented() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// when
		final CourseProposal loaded = sut.findById(id).orElseThrow();
		loaded.approve();
		final CourseProposal updated = sut.saveAndFlush(loaded);

		// then
		assertThat(updated).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void save_staleCourseProposal_optimisticLockingFailureException() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// two independent (detached) reads of the same proposal simulate concurrent admins
		final CourseProposal firstRead = sut.findById(id).orElseThrow();
		entityManager.detach(firstRead);
		final CourseProposal secondRead = sut.findById(id).orElseThrow();
		entityManager.detach(secondRead);

		// the first update succeeds and bumps the version in the database
		firstRead.approve();
		sut.saveAndFlush(firstRead);
		entityManager.clear();

		// when - the second update is based on a now stale version
		secondRead.decline();

		// then
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> sut.saveAndFlush(secondRead));
	}

	private Integer persistFlushClear(UUID uuid) {
		final CourseProposal courseProposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
		final CourseProposal saved = sut.saveAndFlush(courseProposal);
		final Integer id = (Integer) ReflectionTestUtils.getField(saved, "id");
		entityManager.clear();
		return id;
	}
}
