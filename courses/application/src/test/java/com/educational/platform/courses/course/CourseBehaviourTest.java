package com.educational.platform.courses.course;

import java.util.List;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseBehaviourTest {

    private static final Integer TEACHER_ID = 15;

    private Course course() {
        return new Course(CreateCourseCommand.builder().name("name").description("description").build(), TEACHER_ID);
    }

    @Test
    void create_defaultRatingAndNumberOfStudents() {
        // when
        final Course course = course();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }

    @Test
    void archive_archivedStatus() {
        // given
        final Course course = course();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void updateRating_ratingUpdated() {
        // given
        final Course course = course();

        // when
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void increaseNumberOfStudents_numberIncreased() {
        // given
        final Course course = course();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void create_withCurriculumItems_itemsMapped() {
        // given
        final List<CreateCurriculumItemCommand> items = List.of(
                CreateLectureCommand.builder().title("t").description("d").serialNumber(1).text("text").build()
        );
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name").description("description").curriculumItems(items).build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        @SuppressWarnings("unchecked")
        final List<CurriculumItem> curriculumItems = (List<CurriculumItem>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(curriculumItems).hasSize(1);
        assertThat(curriculumItems.get(0)).isInstanceOf(Lecture.class);
    }
}
