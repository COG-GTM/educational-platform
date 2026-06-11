package com.educational.platform.administration.course.query;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCourseProposalsQueryHandlerTest {

    @Mock
    private CourseProposalRepository repository;

    private ListCourseProposalsQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ListCourseProposalsQueryHandler(repository);
    }

    @Test
    void handle_courseProposalsExist_returnsList() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        when(repository.listCourseProposals()).thenReturn(List.of(dto));
        final ListCourseProposalsQuery query = new ListCourseProposalsQuery();

        // when
        final List<CourseProposalDTO> result = sut.handle(query);

        // then
        verify(repository).listCourseProposals();
        assertThat(result)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void handle_noCourseProposals_returnsEmptyList() {
        // given
        when(repository.listCourseProposals()).thenReturn(Collections.emptyList());
        final ListCourseProposalsQuery query = new ListCourseProposalsQuery();

        // when
        final List<CourseProposalDTO> result = sut.handle(query);

        // then
        verify(repository).listCourseProposals();
        assertThat(result).isEmpty();
    }
}
