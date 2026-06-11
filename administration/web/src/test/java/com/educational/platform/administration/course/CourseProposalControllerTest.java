package com.educational.platform.administration.course;

import com.educational.platform.administration.course.approve.ApproveCourseProposalCommand;
import com.educational.platform.administration.course.approve.ApproveCourseProposalCommandHandler;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommand;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommandHandler;
import com.educational.platform.administration.course.query.ListCourseProposalsQuery;
import com.educational.platform.administration.course.query.ListCourseProposalsQueryHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseProposalControllerTest {

    @Mock
    private ApproveCourseProposalCommandHandler approveCourseProposalCommandHandler;

    @Mock
    private DeclineCourseProposalCommandHandler declineCourseProposalCommandHandler;

    @Mock
    private ListCourseProposalsQueryHandler listCourseProposalsQueryHandler;

    @InjectMocks
    private CourseProposalController sut;

    @Test
    void approve_validUuid_delegatesToHandler() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        sut.approve(uuid);

        // then
        final ArgumentCaptor<ApproveCourseProposalCommand> argument = ArgumentCaptor.forClass(ApproveCourseProposalCommand.class);
        verify(approveCourseProposalCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void decline_validUuid_delegatesToHandler() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        sut.decline(uuid);

        // then
        final ArgumentCaptor<DeclineCourseProposalCommand> argument = ArgumentCaptor.forClass(DeclineCourseProposalCommand.class);
        verify(declineCourseProposalCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void courseProposals_existingProposals_returnsList() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class))).thenReturn(List.of(dto));

        // when
        final List<CourseProposalDTO> result = sut.courseProposals();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void courseProposals_noProposals_returnsEmptyList() {
        // given
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class))).thenReturn(Collections.emptyList());

        // when
        final List<CourseProposalDTO> result = sut.courseProposals();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void onConflictException_alreadyApprovedException_returnsConflictResponse() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // when
        final var response = sut.onConflictException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).contains(exception.getMessage());
    }

    @Test
    void onConflictException_alreadyDeclinedException_returnsConflictResponse() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // when
        final var response = sut.onConflictException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).contains(exception.getMessage());
    }
}
