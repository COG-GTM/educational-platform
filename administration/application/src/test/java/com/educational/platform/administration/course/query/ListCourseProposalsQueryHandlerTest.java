package com.educational.platform.administration.course.query;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatusDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseProposalsQueryHandlerTest {

    @Mock
    private CourseProposalRepository repository;

    @InjectMocks
    private ListCourseProposalsQueryHandler sut;

    @Test
    void handle_query_courseProposalsReturned() {
        // given
        final ListCourseProposalsQuery query = new ListCourseProposalsQuery();
        final CourseProposalDTO proposal = new CourseProposalDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"),
                CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        when(repository.listCourseProposals()).thenReturn(List.of(proposal));

        // when
        final List<CourseProposalDTO> result = sut.handle(query);

        // then
        assertThat(result).containsExactly(proposal);
    }
}
