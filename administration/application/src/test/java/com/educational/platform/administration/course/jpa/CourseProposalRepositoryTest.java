package com.educational.platform.administration.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
public class CourseProposalRepositoryTest {

	@Autowired
	private CourseProposalRepository sut;

	@Test
	void listCourseProposals_paged_courseProposals() {
		// given
		sut.save(new CourseProposal(new CreateCourseProposalCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))));

		// when
		var result = sut.listCourseProposals(PageRequest.of(0, 20));

		// then
		assertThat(result).hasSize(1);
	}

	@Test
	void listCourseProposals_secondPage_remainingProposalsInStableOrder() {
		// given
		var first = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		var second = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
		var third = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
		sut.save(new CourseProposal(new CreateCourseProposalCommand(first)));
		sut.save(new CourseProposal(new CreateCourseProposalCommand(second)));
		sut.save(new CourseProposal(new CreateCourseProposalCommand(third)));

		// when
		var result = sut.listCourseProposals(PageRequest.of(1, 2));

		// then
		assertThat(result.getContent()).extracting(CourseProposalDTO::uuid).containsExactly(third);
		assertThat(result.getTotalElements()).isEqualTo(3);
		assertThat(result.getTotalPages()).isEqualTo(2);
	}
}
