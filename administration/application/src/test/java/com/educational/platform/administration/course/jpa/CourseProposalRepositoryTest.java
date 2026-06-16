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
import com.educational.platform.administration.course.CourseProposalStatus;
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

	@Test
	void findById_persistedProposal_initialVersionLoadedFromDatabase() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// when - reload from the database after clearing the persistence context
		final CourseProposal reloaded = sut.findById(id).orElseThrow();

		// then - the @Version column is persisted and loaded back as the initial 0
		assertThat(reloaded)
				.hasFieldOrPropertyWithValue("version", 0)
				.hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
	}

	@Test
	void save_multipleSequentialUpdates_versionIncrementedEachTime() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// when - first update in its own load/save cycle
		final CourseProposal firstUpdate = sut.findById(id).orElseThrow();
		firstUpdate.approve();
		final CourseProposal afterApprove = sut.saveAndFlush(firstUpdate);
		entityManager.clear();

		// and - second update on a freshly loaded instance
		final CourseProposal secondUpdate = sut.findById(id).orElseThrow();
		secondUpdate.decline();
		final CourseProposal afterDecline = sut.saveAndFlush(secondUpdate);

		// then - the version is bumped once per persisted modification
		assertThat(afterApprove).hasFieldOrPropertyWithValue("version", 1);
		assertThat(afterDecline).hasFieldOrPropertyWithValue("version", 2);
	}

	@Test
	void save_staleCourseProposal_winningWriteNotOverwritten() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// two independent (detached) reads of the same proposal simulate concurrent admins
		final CourseProposal firstRead = sut.findById(id).orElseThrow();
		entityManager.detach(firstRead);
		final CourseProposal secondRead = sut.findById(id).orElseThrow();
		entityManager.detach(secondRead);

		// the first admin approves and the write wins
		firstRead.approve();
		sut.saveAndFlush(firstRead);
		entityManager.clear();

		// then - the winning write is persisted (status + bumped version)
		final CourseProposal afterWinningWrite = sut.findById(id).orElseThrow();
		assertThat(afterWinningWrite)
				.hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED)
				.hasFieldOrPropertyWithValue("version", 1);
		entityManager.clear();

		// when - the second admin's decline is based on a now stale version
		secondRead.decline();

		// then - the conflict is detected, so the stale write is rejected instead of overwriting
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
