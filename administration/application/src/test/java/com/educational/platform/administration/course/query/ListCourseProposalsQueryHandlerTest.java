package com.educational.platform.administration.course.query;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatusDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseProposalsQueryHandlerTest {

    @Mock
    private CourseProposalRepository repository;

    @InjectMocks
    private ListCourseProposalsQueryHandler sut;

    @Test
    void handle_proposalsExist_returnsRepositoryListing() {
        // given - the admin listing read path returns the repository's proposal projections unchanged
        final CourseProposalDTO dto = new CourseProposalDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"),
                CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        when(repository.listCourseProposals()).thenReturn(List.of(dto));

        // when
        final List<CourseProposalDTO> result = sut.handle(new ListCourseProposalsQuery());

        // then
        assertThat(result).containsExactly(dto);
        verify(repository).listCourseProposals();
    }

    @Test
    void handle_noProposals_returnsEmptyList() {
        // given - no proposals awaiting moderation yields an empty list rather than null
        when(repository.listCourseProposals()).thenReturn(Collections.emptyList());

        // when
        final List<CourseProposalDTO> result = sut.handle(new ListCourseProposalsQuery());

        // then
        assertThat(result).isEmpty();
    }
}
