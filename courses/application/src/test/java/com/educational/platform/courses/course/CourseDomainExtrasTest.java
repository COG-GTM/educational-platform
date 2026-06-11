package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDomainExtrasTest {

    private static final Integer TEACHER_ID = 15;

    private Course courseWithoutCurriculum() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(command, TEACHER_ID);
    }

    @Test
    void archive_setsArchivedPublishStatus() {
        // given
        final Course course = courseWithoutCurriculum();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void updateRating_storesNewRating() {
        // given
        final Course course = courseWithoutCurriculum();

        // when
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void increaseNumberOfStudents_incrementsByOne() {
        // given
        final Course course = courseWithoutCurriculum();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void toIdentity_returnsGeneratedUuid() {
        // given
        final Course course = courseWithoutCurriculum();

        // then
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void create_withCurriculumItems_createsLectureAndQuiz() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("lecture title")
                .description("lecture description")
                .serialNumber(1)
                .text("lecture text")
                .build();
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("quiz title")
                .description("quiz description")
                .serialNumber(2)
                .text("quiz text")
                .questions(List.of(new CreateQuestionCommand("question content")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        @SuppressWarnings("unchecked")
        final List<CurriculumItem> items = (List<CurriculumItem>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(2);
        assertThat(items.get(0)).isInstanceOf(Lecture.class);
        assertThat(items.get(1)).isInstanceOf(Quiz.class);
    }

    @Test
    void create_withoutCurriculumItems_leavesCurriculumNull() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(ReflectionTestUtils.getField(course, "curriculumItems")).isNull();
    }
}
