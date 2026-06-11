package com.educational.platform.administration.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
public class CourseProposalRepositoryTest {

	@Autowired
	private CourseProposalRepository sut;

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
	void findByUuid_existingProposal_returnsProposal() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid)));

		// when
		final Optional<CourseProposal> result = sut.findByUuid(uuid);

		// then
		assertThat(result).isPresent();
	}

	@Test
	void findByUuid_nonExistingUuid_returnsEmpty() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID otherUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid)));

		// when
		final Optional<CourseProposal> result = sut.findByUuid(otherUuid);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void findByUuid_emptyDatabase_returnsEmpty() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final Optional<CourseProposal> result = sut.findByUuid(uuid);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void listCourseProposals_emptyDatabase_returnsEmptyList() {
		// when
		var result = sut.listCourseProposals();

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void listCourseProposals_multipleProposals_returnsAll() {
		// given
		final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
		final UUID uuid3 = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid1)));
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid2)));
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid3)));

		// when
		var result = sut.listCourseProposals();

		// then
		assertThat(result).hasSize(3);
		assertThat(result).extracting(CourseProposalDTO::uuid)
				.containsExactlyInAnyOrder(uuid1, uuid2, uuid3);
	}

	@Test
	void listCourseProposals_newProposal_returnsDTOWithWaitingForApprovalStatus() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid)));

		// when
		var result = sut.listCourseProposals();

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).uuid()).isEqualTo(uuid);
		assertThat(result.get(0).status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
	}

	@Test
	void listCourseProposals_approvedProposal_returnsDTOWithApprovedStatus() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
		proposal.approve();
		sut.save(proposal);

		// when
		var result = sut.listCourseProposals();

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).uuid()).isEqualTo(uuid);
		assertThat(result.get(0).status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
	}

	@Test
	void listCourseProposals_declinedProposal_returnsDTOWithDeclinedStatus() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
		proposal.decline();
		sut.save(proposal);

		// when
		var result = sut.listCourseProposals();

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).uuid()).isEqualTo(uuid);
		assertThat(result.get(0).status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
	}

	@Test
	void listCourseProposals_mixedStatuses_returnsCorrectStatusForEach() {
		// given
		final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
		final UUID uuid3 = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");

		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid1)));

		final CourseProposal approved = new CourseProposal(new CreateCourseProposalCommand(uuid2));
		approved.approve();
		sut.save(approved);

		final CourseProposal declined = new CourseProposal(new CreateCourseProposalCommand(uuid3));
		declined.decline();
		sut.save(declined);

		// when
		var result = sut.listCourseProposals();

		// then
		assertThat(result).hasSize(3);
		assertThat(result).anySatisfy(dto -> {
			assertThat(dto.uuid()).isEqualTo(uuid1);
			assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
		});
		assertThat(result).anySatisfy(dto -> {
			assertThat(dto.uuid()).isEqualTo(uuid2);
			assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
		});
		assertThat(result).anySatisfy(dto -> {
			assertThat(dto.uuid()).isEqualTo(uuid3);
			assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
		});
	}

	@Test
	void findByUuid_afterApprove_returnsProposalWithApprovedStatus() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
		proposal.approve();
		sut.save(proposal);

		// when
		final Optional<CourseProposal> result = sut.findByUuid(uuid);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().toDTO().status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
	}

	@Test
	void findByUuid_afterDecline_returnsProposalWithDeclinedStatus() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
		proposal.decline();
		sut.save(proposal);

		// when
		final Optional<CourseProposal> result = sut.findByUuid(uuid);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().toDTO().status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
	}

	@Test
	void findByUuid_multipleProposals_returnsCorrectOne() {
		// given
		final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid1)));
		sut.save(new CourseProposal(new CreateCourseProposalCommand(uuid2)));

		// when
		final Optional<CourseProposal> result = sut.findByUuid(uuid2);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().toDTO().uuid()).isEqualTo(uuid2);
	}

	@Test
	void save_thenFindByUuid_roundTripsCorrectly() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
		sut.save(proposal);

		// when
		final Optional<CourseProposal> result = sut.findByUuid(uuid);

		// then
		assertThat(result).isPresent();
		final CourseProposalDTO dto = result.get().toDTO();
		assertThat(dto.uuid()).isEqualTo(uuid);
		assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
	}
}
