package com.educational.platform.administration.course;

import com.educational.platform.administration.course.approve.ApproveCourseProposalCommand;
import com.educational.platform.administration.course.approve.ApproveCourseProposalCommandHandler;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommand;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommandHandler;
import com.educational.platform.administration.course.query.ListCourseProposalsQueryHandler;
import com.educational.platform.web.handler.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseProposalControllerUnitTest {

    @Mock
    private ApproveCourseProposalCommandHandler approveHandler;

    @Mock
    private DeclineCourseProposalCommandHandler declineHandler;

    @Mock
    private ListCourseProposalsQueryHandler listHandler;

    private CourseProposalController sut;

    @BeforeEach
    void setUp() {
        sut = new CourseProposalController(approveHandler, declineHandler, listHandler);
    }

    @Test
    void approve_delegatesToApproveHandler() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        sut.approve(uuid);

        // then
        final ArgumentCaptor<ApproveCourseProposalCommand> captor = ArgumentCaptor.forClass(ApproveCourseProposalCommand.class);
        verify(approveHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void decline_delegatesToDeclineHandler() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        sut.decline(uuid);

        // then
        final ArgumentCaptor<DeclineCourseProposalCommand> captor = ArgumentCaptor.forClass(DeclineCourseProposalCommand.class);
        verify(declineHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void courseProposals_returnsDTOsFromQueryHandler() {
        // given
        final CourseProposalDTO dto = new CourseProposalDTO(UUID.randomUUID(), CourseProposalStatusDTO.APPROVED);
        when(listHandler.handle(any())).thenReturn(List.of(dto));

        // when
        final List<CourseProposalDTO> result = sut.courseProposals();

        // then
        assertThat(result).containsExactly(dto);
    }

    @Test
    void courseProposals_emptyList_returnsEmpty() {
        // given
        when(listHandler.handle(any())).thenReturn(List.of());

        // when
        final List<CourseProposalDTO> result = sut.courseProposals();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void onConflictException_alreadyApproved_returnsConflict() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConflictException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).anyMatch(e -> e.contains(uuid.toString()));
    }

    @Test
    void onConflictException_alreadyDeclined_returnsConflict() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConflictException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).anyMatch(e -> e.contains(uuid.toString()));
    }
}
