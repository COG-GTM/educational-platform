package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Domain-level unit tests for the {@link CurriculumItem} construction invariants introduced by
 * adding {@code @Version}.
 *
 * <p>{@code CurriculumItem} is the third entity this PR gives an optimistic-lock {@code @Version}
 * field. {@code CourseTest} and {@code TeacherTest} already pin the "version is null before persist"
 * invariant for {@code Course}/{@code Teacher}; this class closes the same gap for the curriculum-item
 * hierarchy, which - unlike the other two - is built through {@link CurriculumItemFactory} from the
 * {@code Course} aggregate root rather than via a direct command constructor. Keeping the field null
 * until persist is what lets Hibernate initialise it to 0 on INSERT instead of issuing a versioned
 * UPDATE against a stale snapshot - the very invariant whose violation forced the SQL fixtures to seed
 * {@code version = 0} explicitly.
 *
 * <p>These are pure domain assertions (no JPA); the persistence-level increment/conflict mechanics for
 * {@code CurriculumItem} live in {@code CurriculumItemOptimisticLockingTest}.
 */
class CurriculumItemTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void create_lectureThroughCourseAggregate_versionIsNullBeforePersist() {
        // given - a course whose single curriculum item is a Lecture, built through the production
        // aggregate path (Course constructor -> CurriculumItemFactory -> Lecture)
        final CurriculumItem lecture = singleCurriculumItem(courseWith(lectureCommand()));

        // then - the @Version field must stay null pre-persist so Hibernate initialises it on INSERT
        assertThat(lecture).isInstanceOf(Lecture.class);
        assertThat(ReflectionTestUtils.getField(lecture, "version")).isNull();
    }

    @Test
    void create_quizThroughCourseAggregate_versionIsNullBeforePersist() {
        // given - a course whose single curriculum item is a Quiz (a different @DiscriminatorValue
        // subtype that shares the @Version declared on the abstract CurriculumItem base)
        final CurriculumItem quiz = singleCurriculumItem(courseWith(quizCommand()));

        // then - the inherited @Version field must stay null pre-persist for the Quiz subtype too
        assertThat(quiz).isInstanceOf(Quiz.class);
        assertThat(ReflectionTestUtils.getField(quiz, "version")).isNull();
    }

    @Test
    void create_curriculumItemThroughCourseAggregate_idIsNullBeforePersist() {
        // given - both concrete subtypes built through the aggregate path
        final CurriculumItem lecture = singleCurriculumItem(courseWith(lectureCommand()));
        final CurriculumItem quiz = singleCurriculumItem(courseWith(quizCommand()));

        // then - the generated id is unset until persist, mirroring the version invariant
        assertThat(ReflectionTestUtils.getField(lecture, "id")).isNull();
        assertThat(ReflectionTestUtils.getField(quiz, "id")).isNull();
    }

    @Test
    void createFromFactory_lectureCommand_versionIsNullBeforePersist() {
        // given - the factory path the Course aggregate uses to map a command to a CurriculumItem
        final CurriculumItem item = CurriculumItemFactory.createFrom(lectureCommand(), null);

        // then
        assertThat(item).isInstanceOf(Lecture.class);
        assertThat(ReflectionTestUtils.getField(item, "version")).isNull();
    }

    @Test
    void createFromFactory_quizCommand_versionIsNullBeforePersist() {
        // given
        final CurriculumItem item = CurriculumItemFactory.createFrom(quizCommand(), null);

        // then
        assertThat(item).isInstanceOf(Quiz.class);
        assertThat(ReflectionTestUtils.getField(item, "version")).isNull();
    }

    private static Course courseWith(CreateCurriculumItemCommand item) {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(item))
                .build();
        return new Course(command, TEACHER_ID);
    }

    private static CreateCurriculumItemCommand lectureCommand() {
        return CreateLectureCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("content")
                .build();
    }

    private static CreateCurriculumItemCommand quizCommand() {
        return CreateQuizCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("content")
                .questions(List.of())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static CurriculumItem singleCurriculumItem(Course course) {
        final List<CurriculumItem> items = (List<CurriculumItem>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(1);
        return items.get(0);
    }
}
