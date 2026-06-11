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

    @Test
    void approve_invalidUuidFormat_errorResponse() throws Exception {
        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verifyNoInteractions(approveCourseProposalCommandHandler);
    }

    @Test
    void decline_invalidUuidFormat_errorResponse() throws Exception {
        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verifyNoInteractions(declineCourseProposalCommandHandler);
    }

    @Test
    void approve_alreadyApproved_conflictResponseContainsCannotBeApprovedMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyApprovedException(uuid))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("cannot be approved")))
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("already approved")));
    }

    @Test
    void decline_alreadyDeclined_conflictResponseContainsCannotBeDeclinedMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyDeclinedException(uuid))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("cannot be declined")))
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("already declined")));
    }

    @Test
    void courseProposals_responseContentTypeIsJson() throws Exception {
        // given
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class)))
                .thenReturn(Collections.emptyList());

        // when / then
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void approve_success_responseBodyIsEmpty() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void decline_success_responseBodyIsEmpty() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void approve_genericRuntimeException_internalServerError() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("unexpected error"))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void decline_genericRuntimeException_internalServerError() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("unexpected error"))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void courseProposals_queryHandlerNotInteractedForApproveAndDecline() throws Exception {
        // given
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class)))
                .thenReturn(Collections.emptyList());

        // when
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isOk());

        // then
        verifyNoInteractions(approveCourseProposalCommandHandler);
        verifyNoInteractions(declineCourseProposalCommandHandler);
    }

    @Test
    void approve_postMethod_handlerNotCalled() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(post("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        verifyNoInteractions(approveCourseProposalCommandHandler);
    }

    @Test
    void decline_getMethod_handlerNotCalled() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(get("/administration/course-proposals/{uuid}/approval-status", uuid));

        // then
        verifyNoInteractions(declineCourseProposalCommandHandler);
    }

    @Test
    void approve_alreadyApproved_conflictResponseContentTypeIsJson() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyApprovedException(uuid))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void decline_alreadyDeclined_conflictResponseContentTypeIsJson() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyDeclinedException(uuid))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void class_hasRestControllerAnnotation() {
        assertThat(CourseProposalController.class.isAnnotationPresent(
                org.springframework.web.bind.annotation.RestController.class)).isTrue();
    }

    @Test
    void class_hasRequestMappingWithCorrectPath() {
        final org.springframework.web.bind.annotation.RequestMapping mapping =
                CourseProposalController.class.getAnnotation(
                        org.springframework.web.bind.annotation.RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).contains("/administration/course-proposals");
    }

    @Test
    void approve_alreadyApproved_conflictResponseHasErrorsArray() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyApprovedException(uuid))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    @Test
    void decline_alreadyDeclined_conflictResponseHasErrorsArray() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyDeclinedException(uuid))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    @Test
    void approveMethod_hasPutMappingAnnotation() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("approve", UUID.class);

        // then
        assertThat(method.isAnnotationPresent(
                org.springframework.web.bind.annotation.PutMapping.class)).isTrue();
        final var mapping = method.getAnnotation(
                org.springframework.web.bind.annotation.PutMapping.class);
        assertThat(mapping.value()).contains("/{uuid}/approval-status");
    }

    @Test
    void declineMethod_hasDeleteMappingAnnotation() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("decline", UUID.class);

        // then
        assertThat(method.isAnnotationPresent(
                org.springframework.web.bind.annotation.DeleteMapping.class)).isTrue();
        final var mapping = method.getAnnotation(
                org.springframework.web.bind.annotation.DeleteMapping.class);
        assertThat(mapping.value()).contains("/{uuid}/approval-status");
    }

    @Test
    void courseProposalsMethod_hasGetMappingAnnotation() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("courseProposals");

        // then
        assertThat(method.isAnnotationPresent(
                org.springframework.web.bind.annotation.GetMapping.class)).isTrue();
    }

    @Test
    void exceptionHandler_handlesBothConflictExceptionTypes() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("onConflictException", Exception.class);
        final var annotation = method.getAnnotation(
                org.springframework.web.bind.annotation.ExceptionHandler.class);

        // then
        assertThat(annotation).isNotNull();
        assertThat(annotation.value())
                .containsExactlyInAnyOrder(
                        CourseProposalAlreadyDeclinedException.class,
                        CourseProposalAlreadyApprovedException.class
                );
    }

    @Test
    void approveMethod_hasResponseStatusNoContent() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("approve", UUID.class);
        final var annotation = method.getAnnotation(
                org.springframework.web.bind.annotation.ResponseStatus.class);

        // then
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
    }

    @Test
    void declineMethod_hasResponseStatusNoContent() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("decline", UUID.class);
        final var annotation = method.getAnnotation(
                org.springframework.web.bind.annotation.ResponseStatus.class);

        // then
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
    }

    @Test
    void courseProposalsMethod_hasResponseStatusOk() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("courseProposals");
        final var annotation = method.getAnnotation(
                org.springframework.web.bind.annotation.ResponseStatus.class);

        // then
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(org.springframework.http.HttpStatus.OK);
    }

    @Test
    void patchOnApprovalStatus_handlerNotCalled() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(patch("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        verifyNoInteractions(approveCourseProposalCommandHandler);
        verifyNoInteractions(declineCourseProposalCommandHandler);
    }

    @Test
    void approve_notFound_responseContentTypeIsJson() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new ResourceNotFoundException("Course Proposal with uuid: " + uuid + " not found"))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void decline_notFound_responseContentTypeIsJson() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new ResourceNotFoundException("Course Proposal with uuid: " + uuid + " not found"))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void approve_notFound_responseHasErrorsArray() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new ResourceNotFoundException("Course Proposal with uuid: " + uuid + " not found"))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    @Test
    void decline_notFound_responseHasErrorsArray() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new ResourceNotFoundException("Course Proposal with uuid: " + uuid + " not found"))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    @Test
    void courseProposals_singleApprovedProposal_correctJsonStructure() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatusDTO.APPROVED);
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class))).thenReturn(List.of(dto));

        // when / then
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uuid").value(uuid.toString()))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void approve_success_listHandlerNotInteracted() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // then
        verifyNoInteractions(listCourseProposalsQueryHandler);
    }

    @Test
    void decline_success_listHandlerNotInteracted() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // then
        verifyNoInteractions(listCourseProposalsQueryHandler);
    }

    @Test
    void approve_handlerThrowsAlreadyDeclinedException_returnsConflict() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyDeclinedException(uuid))
                .when(approveCourseProposalCommandHandler).handle(any(ApproveCourseProposalCommand.class));

        // when / then
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(uuid.toString())));
    }

    @Test
    void decline_handlerThrowsAlreadyApprovedException_returnsConflict() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseProposalAlreadyApprovedException(uuid))
                .when(declineCourseProposalCommandHandler).handle(any(DeclineCourseProposalCommand.class));

        // when / then
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(uuid.toString())));
    }

    @Test
    void courseProposals_handlerThrowsRuntimeException_internalServerError() throws Exception {
        // given
        doThrow(new RuntimeException("unexpected query error"))
                .when(listCourseProposalsQueryHandler).handle(any(ListCourseProposalsQuery.class));

        // when / then
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void courseProposals_singleDeclinedProposal_correctJsonStructure() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatusDTO.DECLINED);
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class))).thenReturn(List.of(dto));

        // when / then
        mockMvc.perform(get("/administration/course-proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uuid").value(uuid.toString()))
                .andExpect(jsonPath("$[0].status").value("DECLINED"));
    }

    @Test
    void onConflictException_methodReturnType_isResponseEntity() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("onConflictException", Exception.class);

        // then
        assertThat(method.getReturnType()).isEqualTo(org.springframework.http.ResponseEntity.class);
    }

    @Test
    void approveMethod_producesApplicationJson() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("approve", UUID.class);
        final var mapping = method.getAnnotation(
                org.springframework.web.bind.annotation.PutMapping.class);

        // then
        assertThat(mapping.produces()).contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void declineMethod_producesApplicationJson() throws NoSuchMethodException {
        // when
        final var method = CourseProposalController.class.getMethod("decline", UUID.class);
        final var mapping = method.getAnnotation(
                org.springframework.web.bind.annotation.DeleteMapping.class);

        // then
        assertThat(mapping.produces()).contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void approve_existingCourseProposal_declineAndListHandlersNotInteracted() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(put("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // then
        verifyNoInteractions(declineCourseProposalCommandHandler);
        verifyNoInteractions(listCourseProposalsQueryHandler);
    }

    @Test
    void decline_existingCourseProposal_approveAndListHandlersNotInteracted() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        mockMvc.perform(delete("/administration/course-proposals/{uuid}/approval-status", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // then
        verifyNoInteractions(approveCourseProposalCommandHandler);
        verifyNoInteractions(listCourseProposalsQueryHandler);
    }

    @Test
    void courseProposals_listEndpoint_returnsJsonArray() throws Exception {
        // given
        when(listCourseProposalsQueryHandler.handle(any(ListCourseProposalsQuery.class)))
                .thenReturn(Collections.emptyList());

        // when / then
        mockMvc.perform(get("/administration/course-proposals")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
