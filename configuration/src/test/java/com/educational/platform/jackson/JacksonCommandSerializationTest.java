package com.educational.platform.jackson;

import com.educational.platform.administration.course.approve.ApproveCourseProposalCommand;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.course.decline.DeclineCourseProposalCommand;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import com.educational.platform.courses.course.approve.ApproveCourseCommand;
import com.educational.platform.courses.course.approve.SendCourseToApproveCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import com.educational.platform.users.login.SignInCommand;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Jackson serialization/deserialization of command records after the
 * Jackson Core upgrade (via Spring Boot BOM 4.0.1 -> 4.0.6). Covers command types
 * from all bounded contexts that were not exercised in JacksonUpgradeCompatibilityTest.
 */
class JacksonCommandSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    // --- Administration module commands ---

    @Test
    void approveCourseProposalCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ApproveCourseProposalCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ApproveCourseProposalCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void approveCourseProposalCommand_nullUuid_roundTrip() throws Exception {
        var original = new ApproveCourseProposalCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ApproveCourseProposalCommand.class);

        assertThat(deserialized.uuid()).isNull();
    }

    @Test
    void declineCourseProposalCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new DeclineCourseProposalCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, DeclineCourseProposalCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void declineCourseProposalCommand_nullUuid_roundTrip() throws Exception {
        var original = new DeclineCourseProposalCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, DeclineCourseProposalCommand.class);

        assertThat(deserialized.uuid()).isNull();
    }

    @Test
    void createCourseProposalCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CreateCourseProposalCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseProposalCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void createCourseProposalCommand_nullUuid_roundTrip() throws Exception {
        var original = new CreateCourseProposalCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseProposalCommand.class);

        assertThat(deserialized.uuid()).isNull();
    }

    // --- Course module commands ---

    @Test
    void approveCourseCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ApproveCourseCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ApproveCourseCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void sendCourseToApproveCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new SendCourseToApproveCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, SendCourseToApproveCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void publishCourseCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new PublishCourseCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, PublishCourseCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void publishCourseCommand_nullUuid_roundTrip() throws Exception {
        var original = new PublishCourseCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, PublishCourseCommand.class);

        assertThat(deserialized.uuid()).isNull();
    }

    @Test
    void increaseNumberOfStudentsCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new IncreaseNumberOfStudentsCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, IncreaseNumberOfStudentsCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void updateCourseRatingCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new UpdateCourseRatingCommand(uuid, 4.25);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UpdateCourseRatingCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(4.25);
    }

    @Test
    void updateCourseRatingCommand_zeroRating_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new UpdateCourseRatingCommand(uuid, 0.0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UpdateCourseRatingCommand.class);

        assertThat(deserialized.rating()).isEqualTo(0.0);
    }

    @Test
    void updateCourseRatingCommand_maxDoubleRating_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new UpdateCourseRatingCommand(uuid, Double.MAX_VALUE);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UpdateCourseRatingCommand.class);

        assertThat(deserialized.rating()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void createTeacherCommand_roundTrip() throws Exception {
        var original = new CreateTeacherCommand("teacher1");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateTeacherCommand.class);

        assertThat(deserialized.username()).isEqualTo("teacher1");
    }

    @Test
    void createTeacherCommand_nullUsername_roundTrip() throws Exception {
        var original = new CreateTeacherCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateTeacherCommand.class);

        assertThat(deserialized.username()).isNull();
    }

    // --- Course enrollment commands ---

    @Test
    void createCourseCommand_enrollment_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CreateCourseCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void createStudentCommand_roundTrip() throws Exception {
        var original = new CreateStudentCommand("student_user");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateStudentCommand.class);

        assertThat(deserialized.username()).isEqualTo("student_user");
    }

    @Test
    void createStudentCommand_nullUsername_roundTrip() throws Exception {
        var original = new CreateStudentCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateStudentCommand.class);

        assertThat(deserialized.username()).isNull();
    }

    @Test
    void createStudentCommand_emptyUsername_roundTrip() throws Exception {
        var original = new CreateStudentCommand("");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateStudentCommand.class);

        assertThat(deserialized.username()).isEmpty();
    }

    @Test
    void registerStudentToCourseCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new RegisterStudentToCourseCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, RegisterStudentToCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
    }

    @Test
    void registerStudentToCourseCommand_nullCourseId_roundTrip() throws Exception {
        var original = new RegisterStudentToCourseCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, RegisterStudentToCourseCommand.class);

        assertThat(deserialized.courseId()).isNull();
    }

    @Test
    void registerStudentToCourseCommand_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\"}";

        var deserialized = objectMapper.readValue(json, RegisterStudentToCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
    }

    // --- Course reviews commands ---

    @Test
    void reviewCourseCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ReviewCourseCommand(uuid, 4.5, "Great course!");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(4.5);
        assertThat(deserialized.comment()).isEqualTo("Great course!");
    }

    @Test
    void reviewCourseCommand_nullComment_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ReviewCourseCommand(uuid, 5.0, null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(5.0);
        assertThat(deserialized.comment()).isNull();
    }

    @Test
    void reviewCourseCommand_zeroRating_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ReviewCourseCommand(uuid, 0.0, "No rating");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.rating()).isEqualTo(0.0);
    }

    @Test
    void reviewCourseCommand_maxRating_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ReviewCourseCommand(uuid, 5.0, "Perfect!");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.rating()).isEqualTo(5.0);
    }

    @Test
    void updateCourseReviewCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new UpdateCourseReviewCommand(uuid, 3.0, "Updated review");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UpdateCourseReviewCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(3.0);
        assertThat(deserialized.comment()).isEqualTo("Updated review");
    }

    @Test
    void updateCourseReviewCommand_nullFields_roundTrip() throws Exception {
        var original = new UpdateCourseReviewCommand(null, null, null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UpdateCourseReviewCommand.class);

        assertThat(deserialized.uuid()).isNull();
        assertThat(deserialized.rating()).isNull();
        assertThat(deserialized.comment()).isNull();
    }

    @Test
    void createReviewableCourseCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CreateReviewableCourseCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateReviewableCourseCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void createReviewerCommand_roundTrip() throws Exception {
        var original = new CreateReviewerCommand("reviewer1");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateReviewerCommand.class);

        assertThat(deserialized.username()).isEqualTo("reviewer1");
    }

    @Test
    void createReviewerCommand_nullUsername_roundTrip() throws Exception {
        var original = new CreateReviewerCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateReviewerCommand.class);

        assertThat(deserialized.username()).isNull();
    }

    // --- User module commands ---

    @Test
    void signInCommand_roundTrip() throws Exception {
        var original = SignInCommand.builder()
                .username("admin")
                .password("P@ssword1")
                .build();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, SignInCommand.class);

        assertThat(deserialized.username()).isEqualTo("admin");
        assertThat(deserialized.password()).isEqualTo("P@ssword1");
    }

    @Test
    void signInCommand_nullFields_roundTrip() throws Exception {
        var original = SignInCommand.builder().build();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, SignInCommand.class);

        assertThat(deserialized.username()).isNull();
        assertThat(deserialized.password()).isNull();
    }

    @Test
    void signInCommand_emptyFields_roundTrip() throws Exception {
        var original = SignInCommand.builder()
                .username("")
                .password("")
                .build();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, SignInCommand.class);

        assertThat(deserialized.username()).isEmpty();
        assertThat(deserialized.password()).isEmpty();
    }

    // --- Quiz command with nested questions ---

    @Test
    void createQuizCommand_withQuestions_serialization() throws Exception {
        var questions = List.of(
                new CreateQuestionCommand("What is Java?"),
                new CreateQuestionCommand("What is OOP?")
        );
        var original = CreateQuizCommand.builder()
                .title("Java Quiz")
                .description("Basic Java quiz")
                .serialNumber(1)
                .text("Answer the following questions")
                .questions(questions)
                .build();

        var json = objectMapper.writeValueAsString(original);
        var node = objectMapper.readTree(json);

        assertThat(node.get("title").asText()).isEqualTo("Java Quiz");
        assertThat(node.get("description").asText()).isEqualTo("Basic Java quiz");
        assertThat(node.get("serialNumber").asInt()).isEqualTo(1);
        assertThat(node.get("text").asText()).isEqualTo("Answer the following questions");
        assertThat(node.get("questions").isArray()).isTrue();
        assertThat(node.get("questions").size()).isEqualTo(2);
        assertThat(node.get("questions").get(0).get("content").asText()).isEqualTo("What is Java?");
    }

    @Test
    void createQuizCommand_emptyQuestions_serialization() throws Exception {
        var original = CreateQuizCommand.builder()
                .title("Empty Quiz")
                .description("No questions")
                .serialNumber(0)
                .text("N/A")
                .questions(Collections.emptyList())
                .build();

        var json = objectMapper.writeValueAsString(original);
        var node = objectMapper.readTree(json);

        assertThat(node.get("questions").isArray()).isTrue();
        assertThat(node.get("questions").isEmpty()).isTrue();
    }

    @Test
    void createQuizCommand_nullFields_serialization() throws Exception {
        var original = CreateQuizCommand.builder().build();

        var json = objectMapper.writeValueAsString(original);
        var node = objectMapper.readTree(json);

        assertThat(node.get("title").isNull()).isTrue();
        assertThat(node.get("description").isNull()).isTrue();
        assertThat(node.get("serialNumber").isNull()).isTrue();
        assertThat(node.get("text").isNull()).isTrue();
        assertThat(node.get("questions").isNull()).isTrue();
    }

    // --- Deserialization from raw JSON (simulating external input) ---

    @Test
    void reviewCourseCommand_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\",\"rating\":4.5,\"comment\":\"Nice\"}";

        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(4.5);
        assertThat(deserialized.comment()).isEqualTo("Nice");
    }

    @Test
    void updateCourseReviewCommand_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"uuid\":\"" + uuid + "\",\"rating\":2.5,\"comment\":\"Updated\"}";

        var deserialized = objectMapper.readValue(json, UpdateCourseReviewCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(2.5);
        assertThat(deserialized.comment()).isEqualTo("Updated");
    }

    @Test
    void signInCommand_unknownProperties_rejected() {
        var json = "{\"username\":\"admin\",\"password\":\"pass\",\"extra\":\"field\"}";

        assertThatThrownBy(() -> objectMapper.readValue(json, SignInCommand.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .hasMessageContaining("extra");
    }

    @Test
    void reviewCourseCommand_unknownProperties_rejected() {
        var json = "{\"courseId\":\"" + UUID.randomUUID() + "\",\"rating\":4.0,\"comment\":\"x\",\"unknown\":true}";

        assertThatThrownBy(() -> objectMapper.readValue(json, ReviewCourseCommand.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .hasMessageContaining("unknown");
    }

    // --- Resource allocation constraints applied to project command types ---

    @Test
    void resourceConstraints_deeplyNestedCommand_rejected() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(5)
                        .build()
        );

        var deepJson = buildDeeplyNestedReviewCommand(10);

        assertThatThrownBy(() -> restrictedMapper.readTree(deepJson))
                .isInstanceOf(Exception.class);
    }

    @Test
    void resourceConstraints_longStringInCommand_withinDefaultLimit_parsesSuccessfully() throws Exception {
        var longComment = "x".repeat(1000);
        var uuid = UUID.randomUUID();
        var original = new ReviewCourseCommand(uuid, 4.0, longComment);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.comment()).hasSize(1000);
    }

    @Test
    void resourceConstraints_customStringLimit_exceedingInCommand_rejected() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(50)
                        .build()
        );

        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\",\"rating\":4.0,\"comment\":\"" + "x".repeat(100) + "\"}";

        assertThatThrownBy(() -> restrictedMapper.readValue(json, ReviewCourseCommand.class))
                .isInstanceOf(Exception.class);
    }

    // --- JSON field ordering independence for command records ---

    @Test
    void reviewCourseCommand_fieldOrderIndependence() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"comment\":\"First\",\"courseId\":\"" + uuid + "\",\"rating\":3.0}";

        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(3.0);
        assertThat(deserialized.comment()).isEqualTo("First");
    }

    @Test
    void updateCourseRatingCommand_fieldOrderIndependence() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"rating\":4.5,\"uuid\":\"" + uuid + "\"}";

        var deserialized = objectMapper.readValue(json, UpdateCourseRatingCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(4.5);
    }

    // --- Missing fields for command records ---

    @Test
    void reviewCourseCommand_missingOptionalComment_defaultsToNull() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\",\"rating\":4.0}";

        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(4.0);
        assertThat(deserialized.comment()).isNull();
    }

    @Test
    void updateCourseReviewCommand_missingComment_defaultsToNull() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"uuid\":\"" + uuid + "\",\"rating\":3.5}";

        var deserialized = objectMapper.readValue(json, UpdateCourseReviewCommand.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.rating()).isEqualTo(3.5);
        assertThat(deserialized.comment()).isNull();
    }

    // --- Special characters in command fields ---

    @Test
    void createTeacherCommand_specialCharacters_roundTrip() throws Exception {
        var original = new CreateTeacherCommand("user@domain.com <Teacher> \"quoted\"");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateTeacherCommand.class);

        assertThat(deserialized.username()).isEqualTo("user@domain.com <Teacher> \"quoted\"");
    }

    @Test
    void reviewCourseCommand_unicodeComment_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ReviewCourseCommand(uuid, 5.0, "\u00e9\u00f1\u00fc \u2014 \u201cExcellent\u201d \u2605");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseCommand.class);

        assertThat(deserialized.comment()).contains("\u00e9", "\u00f1", "\u2605");
    }

    private String buildDeeplyNestedReviewCommand(int depth) {
        var sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{\"a\":");
        }
        sb.append("1");
        for (int i = 0; i < depth; i++) {
            sb.append("}");
        }
        return sb.toString();
    }
}
