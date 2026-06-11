package com.educational.platform.administration.course.query;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private ListCourseProposalsQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ListCourseProposalsQueryHandler(repository);
    }

    @Test
    void handle_existingProposals_returnsProposalsFromRepository() {
        // given
        final CourseProposalDTO dto = new CourseProposalDTO(UUID.randomUUID(), CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        when(repository.listCourseProposals()).thenReturn(List.of(dto));

        // when
        final List<CourseProposalDTO> result = sut.handle(new ListCourseProposalsQuery());

        // then
        assertThat(result).containsExactly(dto);
    }

    @Test
    void handle_noProposals_returnsEmptyList() {
        // given
        when(repository.listCourseProposals()).thenReturn(List.of());

        // when
        final List<CourseProposalDTO> result = sut.handle(new ListCourseProposalsQuery());

        // then
        assertThat(result).isEmpty();
    }
}
