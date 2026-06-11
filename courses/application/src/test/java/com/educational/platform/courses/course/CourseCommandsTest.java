package com.educational.platform.courses.course;

import com.educational.platform.courses.course.approve.ApproveCourseCommand;
import com.educational.platform.courses.course.approve.SendCourseToApproveCommand;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.query.CourseByUUIDQuery;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCommandsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void createCourseCommand_builderBuildsAllFields() {
        // when
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of())
                .build();

        // then
        assertThat(command.name()).isEqualTo("name");
        assertThat(command.description()).isEqualTo("description");
        assertThat(command.curriculumItems()).isEmpty();
    }

    @Test
    void createLectureCommand_builderBuildsAllFields() {
        // when
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(3)
                .text("text")
                .build();

        // then
        assertThat(command.getTitle()).isEqualTo("title");
        assertThat(command.getDescription()).isEqualTo("description");
        assertThat(command.getSerialNumber()).isEqualTo(3);
        assertThat(command.getText()).isEqualTo("text");
    }

    @Test
    void createQuizCommand_builderBuildsAllFields() {
        // given
        final CreateQuestionCommand question = new CreateQuestionCommand("content");

        // when
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(4)
                .text("text")
                .questions(List.of(question))
                .build();

        // then
        assertThat(command.getTitle()).isEqualTo("title");
        assertThat(command.getSerialNumber()).isEqualTo(4);
        assertThat(command.getText()).isEqualTo("text");
        assertThat(command.getQuestions()).containsExactly(question);
    }

    @Test
    void createQuestionCommand_exposesContent() {
        assertThat(new CreateQuestionCommand("content").content()).isEqualTo("content");
    }

    @Test
    void uuidCommands_exposeUuid() {
        assertThat(new ApproveCourseCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
        assertThat(new SendCourseToApproveCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
        assertThat(new PublishCourseCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
        assertThat(new IncreaseNumberOfStudentsCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
        assertThat(new CourseByUUIDQuery(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void updateCourseRatingCommand_exposesUuidAndRating() {
        // when
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(UUID_VALUE, 4.5);

        // then
        assertThat(command.uuid()).isEqualTo(UUID_VALUE);
        assertThat(command.rating()).isEqualTo(4.5);
    }
}
