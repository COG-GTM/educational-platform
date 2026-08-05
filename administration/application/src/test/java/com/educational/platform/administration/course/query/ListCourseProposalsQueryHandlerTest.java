package com.educational.platform.administration.course.query;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatusDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseProposalsQueryHandlerTest {

	@Mock
	private CourseProposalRepository repository;

	@InjectMocks
	private ListCourseProposalsQueryHandler sut;

	@Test
	void handle_pagedQuery_repositoryCalledWithCorrespondingPageRequest() {
		// given
		var query = new ListCourseProposalsQuery(2, 15);
		var dto = new CourseProposalDTO(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
		when(repository.listCourseProposals(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(dto)));

		// when
		var result = sut.handle(query);

		// then
		assertThat(result.getContent()).containsExactly(dto);
		var pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).listCourseProposals(pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(15);
	}

	@Test
	void handle_defaultQuery_repositoryCalledWithFirstPageAndDefaultSize() {
		// given
		var query = new ListCourseProposalsQuery();
		when(repository.listCourseProposals(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

		// when
		sut.handle(query);

		// then
		var pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).listCourseProposals(pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isZero();
		assertThat(pageable.getValue().getPageSize()).isEqualTo(ListCourseProposalsQuery.DEFAULT_PAGE_SIZE);
	}
}
