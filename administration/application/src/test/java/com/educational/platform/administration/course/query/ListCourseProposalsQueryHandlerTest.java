package com.educational.platform.administration.course.query;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.annotation.Nonnull;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Test
    void handle_multipleProposalsWithDifferentStatuses_returnsAll() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UUID uuid3 = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final CourseProposalDTO dto1 = new CourseProposalDTO(uuid1, CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        final CourseProposalDTO dto2 = new CourseProposalDTO(uuid2, CourseProposalStatusDTO.APPROVED);
        final CourseProposalDTO dto3 = new CourseProposalDTO(uuid3, CourseProposalStatusDTO.DECLINED);
        when(repository.listCourseProposals()).thenReturn(List.of(dto1, dto2, dto3));
        final ListCourseProposalsQuery query = new ListCourseProposalsQuery();

        // when
        final List<CourseProposalDTO> result = sut.handle(query);

        // then
        verify(repository).listCourseProposals();
        assertThat(result)
                .hasSize(3)
                .extracting(CourseProposalDTO::uuid)
                .containsExactly(uuid1, uuid2, uuid3);
        assertThat(result)
                .extracting(CourseProposalDTO::status)
                .containsExactly(
                        CourseProposalStatusDTO.WAITING_FOR_APPROVAL,
                        CourseProposalStatusDTO.APPROVED,
                        CourseProposalStatusDTO.DECLINED
                );
    }

    @Test
    void handle_hasPreAuthorizeAnnotation() throws NoSuchMethodException {
        // when
        final var method = ListCourseProposalsQueryHandler.class
                .getMethod("handle", ListCourseProposalsQuery.class);

        // then
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    @Test
    void handle_preAuthorizeAnnotation_requiresAdminRole() throws NoSuchMethodException {
        // when
        final var method = ListCourseProposalsQueryHandler.class
                .getMethod("handle", ListCourseProposalsQuery.class);
        final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        // then
        assertThat(annotation.value()).contains("hasRole('ADMIN')");
    }

    @Test
    void handle_hasNonnullAnnotation() throws NoSuchMethodException {
        // when
        final var method = ListCourseProposalsQueryHandler.class
                .getMethod("handle", ListCourseProposalsQuery.class);

        // then
        assertThat(method.isAnnotationPresent(Nonnull.class)).isTrue();
    }

    @Test
    void class_hasNamedAnnotation() {
        assertThat(ListCourseProposalsQueryHandler.class.isAnnotationPresent(jakarta.inject.Named.class)).isTrue();
    }

    @Test
    void handle_delegatesToRepositoryListCourseProposals() {
        // given
        when(repository.listCourseProposals()).thenReturn(Collections.emptyList());
        final ListCourseProposalsQuery query = new ListCourseProposalsQuery();

        // when
        sut.handle(query);

        // then
        verify(repository).listCourseProposals();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void class_doesNotHaveTransactionalAnnotation() {
        assertThat(ListCourseProposalsQueryHandler.class
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
    }

    @Test
    void handle_preAuthorizeAnnotation_exactValue() throws NoSuchMethodException {
        // when
        final var method = ListCourseProposalsQueryHandler.class
                .getMethod("handle", ListCourseProposalsQuery.class);
        final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        // then
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
