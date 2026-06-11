package com.educational.platform.courses.course;

import com.educational.platform.courses.course.approve.ApproveCourseCommand;
import com.educational.platform.courses.course.approve.SendCourseToApproveCommand;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCommandRecordsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void approveCourseCommand_recordAccessors() {
        final ApproveCourseCommand cmd = new ApproveCourseCommand(UUID_VALUE);
        assertThat(cmd.uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void approveCourseCommand_equalInstances() {
        assertThat(new ApproveCourseCommand(UUID_VALUE))
                .isEqualTo(new ApproveCourseCommand(UUID_VALUE));
    }

    @Test
    void sendCourseToApproveCommand_recordAccessors() {
        final SendCourseToApproveCommand cmd = new SendCourseToApproveCommand(UUID_VALUE);
        assertThat(cmd.uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void sendCourseToApproveCommand_equalInstances() {
        assertThat(new SendCourseToApproveCommand(UUID_VALUE))
                .isEqualTo(new SendCourseToApproveCommand(UUID_VALUE));
    }

    @Test
    void publishCourseCommand_recordAccessors() {
        final PublishCourseCommand cmd = new PublishCourseCommand(UUID_VALUE);
        assertThat(cmd.uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void publishCourseCommand_equalInstances() {
        assertThat(new PublishCourseCommand(UUID_VALUE))
                .isEqualTo(new PublishCourseCommand(UUID_VALUE));
    }

    @Test
    void increaseNumberOfStudentsCommand_recordAccessors() {
        final IncreaseNumberOfStudentsCommand cmd = new IncreaseNumberOfStudentsCommand(UUID_VALUE);
        assertThat(cmd.uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void updateCourseRatingCommand_recordAccessors() {
        final UpdateCourseRatingCommand cmd = new UpdateCourseRatingCommand(UUID_VALUE, 4.5);
        assertThat(cmd.uuid()).isEqualTo(UUID_VALUE);
        assertThat(cmd.rating()).isEqualTo(4.5);
    }

    @Test
    void updateCourseRatingCommand_equalInstances() {
        assertThat(new UpdateCourseRatingCommand(UUID_VALUE, 3.0))
                .isEqualTo(new UpdateCourseRatingCommand(UUID_VALUE, 3.0));
    }

    @Test
    void updateCourseRatingCommand_differentRating_notEqual() {
        assertThat(new UpdateCourseRatingCommand(UUID_VALUE, 3.0))
                .isNotEqualTo(new UpdateCourseRatingCommand(UUID_VALUE, 4.0));
    }

    @Test
    void createCourseCommand_builderAccessors() {
        final CreateCourseCommand cmd = CreateCourseCommand.builder()
                .name("Java 101")
                .description("Intro to Java")
                .build();
        assertThat(cmd.name()).isEqualTo("Java 101");
        assertThat(cmd.description()).isEqualTo("Intro to Java");
    }

    @Test
    void createLectureCommand_builderAccessors() {
        final CreateLectureCommand cmd = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("First lecture")
                .serialNumber(1)
                .text("content")
                .build();
        assertThat(cmd.getTitle()).isEqualTo("Lecture 1");
        assertThat(cmd.getDescription()).isEqualTo("First lecture");
        assertThat(cmd.getSerialNumber()).isEqualTo(1);
        assertThat(cmd.getText()).isEqualTo("content");
    }

    @Test
    void createQuizCommand_builderAccessors() {
        final CreateQuestionCommand question = new CreateQuestionCommand("What is Java?");
        final CreateQuizCommand cmd = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(2)
                .text("quiz text")
                .questions(List.of(question))
                .build();
        assertThat(cmd.getTitle()).isEqualTo("Quiz 1");
        assertThat(cmd.getDescription()).isEqualTo("First quiz");
        assertThat(cmd.getSerialNumber()).isEqualTo(2);
        assertThat(cmd.getText()).isEqualTo("quiz text");
        assertThat(cmd.getQuestions()).containsExactly(question);
    }

    @Test
    void createQuestionCommand_recordAccessors() {
        final CreateQuestionCommand cmd = new CreateQuestionCommand("What is DDD?");
        assertThat(cmd.content()).isEqualTo("What is DDD?");
    }

    @Test
    void createQuestionCommand_equalInstances() {
        assertThat(new CreateQuestionCommand("Q1"))
                .isEqualTo(new CreateQuestionCommand("Q1"));
    }
}
