package com.educational.platform.administration.course;

import com.educational.platform.administration.course.approve.ApproveCourseProposalCommand;
import com.educational.platform.administration.course.approve.ApproveCourseProposalCommandHandler;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommand;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommandHandler;
import com.educational.platform.administration.course.query.ListCourseProposalsQuery;
import com.educational.platform.administration.course.query.ListCourseProposalsQueryHandler;
import com.educational.platform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseProposalController.class)
class CourseProposalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApproveCourseProposalCommandHandler approveCourseProposalCommandHandler;

    @MockitoBean
    private DeclineCourseProposalCommandHandler declineCourseProposalCommandHandler;

    @MockitoBean
    private ListCourseProposalsQueryHandler listCourseProposalsQueryHandler;

    @Test
    void approve_existingCourseProposal_noContent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        final ArgumentCaptor<ApproveCourseProposalCommand> captor = ArgumentCaptor.forClass(ApproveCourseProposalCommand.class);
        verify(approveCourseProposalCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void decline_existingCourseProposal_noContent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        final ArgumentCaptor<DeclineCourseProposalCommand> captor = ArgumentCaptor.forClass(DeclineCourseProposalCommand.class);
        verify(declineCourseProposalCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void courseProposals_returnsListOfProposals() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class))).thenReturn(List.of(dto));

        // when / then
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(uuid.toString()))
                .andExpect(jsonPath("$[0].status").value("WAITING_FOR_APPROVAL"));
    }

    @Test
    void courseProposals_noProposals_returnsEmptyList() throws Exception {
        // given
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class))).thenReturn(Collections.emptyList());

        // when / then
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void approve_alreadyApproved_conflict() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyApprovedException(uuid))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(uuid.toString())));
    }

    @Test
    void decline_alreadyDeclined_conflict() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyDeclinedException(uuid))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(uuid.toString())));
    }

    @Test
    void approve_notFound_returnsNotFound() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new ResourceNotFoundException("Course Proposal with uuid: " + uuid + " not found"))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(uuid.toString())));
    }

    @Test
    void decline_notFound_returnsNotFound() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new ResourceNotFoundException("Course Proposal with uuid: " + uuid + " not found"))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(uuid.toString())));
    }

    @Test
    void courseProposals_multipleProposals_returnsAllWithCorrectJson() throws Exception {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UUID uuid3 = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final CourseProposalDTO dto1 = new CourseProposalDTO(uuid1, CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        final CourseProposalDTO dto2 = new CourseProposalDTO(uuid2, CourseProposalStatusDTO.APPROVED);
        final CourseProposalDTO dto3 = new CourseProposalDTO(uuid3, CourseProposalStatusDTO.DECLINED);
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class)))
                .thenReturn(List.of(dto1, dto2, dto3));

        // when / then
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].uuid").value(uuid1.toString()))
                .andExpect(jsonPath("$[0].status").value("WAITING_FOR_APPROVAL"))
                .andExpect(jsonPath("$[1].uuid").value(uuid2.toString()))
                .andExpect(jsonPath("$[1].status").value("APPROVED"))
                .andExpect(jsonPath("$[2].uuid").value(uuid3.toString()))
                .andExpect(jsonPath("$[2].status").value("DECLINED"));
    }

    @Test
    void approve_existingCourseProposal_handlerNotInteractedForDecline() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // then
        verifyNoInteractions(declineCourseProposalCommandHandler);
    }

    @Test
    void decline_existingCourseProposal_handlerNotInteractedForApprove() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // then
        verifyNoInteractions(approveCourseProposalCommandHandler);
    }
}
