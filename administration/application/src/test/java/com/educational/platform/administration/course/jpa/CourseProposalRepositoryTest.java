package com.educational.platform.administration.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import javax.sql.DataSource;

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

	@Autowired
	private DataSource dataSource;

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
	void save_declinedCourseProposal_versionIncremented() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// when - the decline write path is the mutation, mirroring the approve increment test
		final CourseProposal loaded = sut.findById(id).orElseThrow();
		loaded.decline();
		final CourseProposal updated = sut.saveAndFlush(loaded);

		// then - declining also bumps the @Version exactly once
		assertThat(updated)
				.hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED)
				.hasFieldOrPropertyWithValue("version", 1);
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
	void save_staleProposalSameOperation_optimisticLockingFailureException() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// two independent (detached) reads of the still-WAITING proposal simulate concurrent admins
		final CourseProposal firstRead = sut.findById(id).orElseThrow();
		entityManager.detach(firstRead);
		final CourseProposal secondRead = sut.findById(id).orElseThrow();
		entityManager.detach(secondRead);

		// the first admin approves and the write wins
		firstRead.approve();
		sut.saveAndFlush(firstRead);
		entityManager.clear();

		// when - the second admin performs the SAME operation on a now stale snapshot; the domain
		// AlreadyApproved guard does not fire because the stale snapshot is still WAITING_FOR_APPROVAL
		secondRead.approve();

		// then - the conflict is caught purely by the @Version check, not by the domain status guard
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> sut.saveAndFlush(secondRead));
	}

	@Test
	void save_staleProposalAfterWinningDecline_optimisticLockingFailureException() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// two independent (detached) reads of the same proposal simulate concurrent admins
		final CourseProposal firstRead = sut.findById(id).orElseThrow();
		entityManager.detach(firstRead);
		final CourseProposal secondRead = sut.findById(id).orElseThrow();
		entityManager.detach(secondRead);

		// the first admin declines and that write wins, bumping the version in the database
		firstRead.decline();
		sut.saveAndFlush(firstRead);
		entityManager.clear();

		// when - the second admin's approve is based on a now stale version
		secondRead.approve();

		// then - the conflict is detected symmetrically when decline is the winning write
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

	@Test
	void findByUuid_persistedProposal_initialVersionLoadedFromDatabase() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		persistFlushClear(uuid);

		// when - reload through the uuid lookup used by the approve/decline handlers
		final CourseProposal reloaded = sut.findByUuid(uuid).orElseThrow();

		// then - the @Version column is populated on the production read path
		assertThat(reloaded)
				.hasFieldOrPropertyWithValue("version", 0)
				.hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
	}

	@Test
	void save_staleProposalLoadedByUuid_optimisticLockingFailureException() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		persistFlushClear(uuid);

		// two independent (detached) reads via findByUuid mirror the approve/decline handler flow
		final CourseProposal firstRead = sut.findByUuid(uuid).orElseThrow();
		entityManager.detach(firstRead);
		final CourseProposal secondRead = sut.findByUuid(uuid).orElseThrow();
		entityManager.detach(secondRead);

		// one admin approves and the write wins
		firstRead.approve();
		sut.saveAndFlush(firstRead);
		entityManager.clear();

		// when - the other admin's decline is based on a now stale version
		secondRead.decline();

		// then - the conflict is detected on the same read path the handlers use
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> sut.saveAndFlush(secondRead));
	}

	@Test
	void findByUuid_rowSeededViaRawSqlWithoutVersion_versionDefaultsToZeroAndUpdateSucceeds() {
		// given - a row inserted via raw SQL (mirroring the web module's insert_data.sql seed)
		// omits the version column, so it relies solely on the DB-level "default 0 not null"
		// contributed by @Column(columnDefinition = "bigint default 0 not null"). Without that
		// default the column would store NULL and the optimistic-lock UPDATE ... WHERE version = NULL
		// would match 0 rows and fail.
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		entityManager.getEntityManager()
				.createNativeQuery("insert into course_proposal (uuid, status) values (:uuid, :status)")
				.setParameter("uuid", uuid)
				.setParameter("status", CourseProposalStatus.WAITING_FOR_APPROVAL.name())
				.executeUpdate();
		entityManager.flush();
		entityManager.clear();

		// when - load the seeded row on the production read path used by the approve/decline handlers
		final CourseProposal seeded = sut.findByUuid(uuid).orElseThrow();

		// then - the DB default populated the version, so it is the initial 0 (never null)
		assertThat(seeded).hasFieldOrPropertyWithValue("version", 0);

		// and - a subsequent approve + save passes the optimistic-lock check and bumps the version
		seeded.approve();
		final CourseProposal updated = sut.saveAndFlush(seeded);
		assertThat(updated)
				.hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED)
				.hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void schema_versionColumn_mappedToNonNullBigint() throws SQLException {
		// given - the @Version Integer field is deliberately mapped to a BIGINT column via
		// @Column(columnDefinition = "bigint default 0 not null"). This test pins that mapping so a
		// later refactor cannot silently revert it to Hibernate's default (nullable INTEGER), which is
		// exactly the regression that broke optimistic locking for raw-SQL-seeded rows.
		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "COURSE_PROPOSAL", "VERSION")) {

			// then
			assertThat(columns.next()).as("version column exists on course_proposal").isTrue();
			assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
			assertThat(columns.getString("TYPE_NAME")).isEqualToIgnoringCase("BIGINT");
			assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
		}
	}

	@Test
	void save_staleCourseProposal_failedWriteLeavesWinningStatePersisted() {
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

		// when - the second admin's stale decline is rejected by the @Version check
		secondRead.decline();
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> sut.saveAndFlush(secondRead));

		// the failed flush leaves the persistence context unusable, so clear it before reloading
		entityManager.clear();

		// then - the persisted row still reflects the winning approve and was NOT silently overwritten
		// by the rejected decline (the whole point of optimistic locking)
		final CourseProposal reloaded = sut.findById(id).orElseThrow();
		assertThat(reloaded)
				.hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED)
				.hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void save_reloadedProposalWithoutChanges_versionNotIncremented() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final Integer id = persistFlushClear(uuid);

		// when - reload and persist again without mutating any state
		final CourseProposal loaded = sut.findById(id).orElseThrow();
		final CourseProposal resaved = sut.saveAndFlush(loaded);

		// then - the version only advances on a real modification, never on a no-op save
		assertThat(resaved).hasFieldOrPropertyWithValue("version", 0);
	}

	private Integer persistFlushClear(UUID uuid) {
		final CourseProposal courseProposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
		final CourseProposal saved = sut.saveAndFlush(courseProposal);
		final Integer id = (Integer) ReflectionTestUtils.getField(saved, "id");
		entityManager.clear();
		return id;
	}
}
