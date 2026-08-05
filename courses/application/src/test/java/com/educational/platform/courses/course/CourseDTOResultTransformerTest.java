package com.educational.platform.courses.course;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDTOResultTransformerTest {

	private static final String[] ALIASES = {
			CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN, CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
			CurriculumItemDTO.SERIAL_NUMBER, CurriculumItemDTO.UUID_COLUMN, CurriculumItemDTO.TITLE, CurriculumItemDTO.DESCRIPTION,
			CurriculumItemDTO.TYPE, LectureDTO.TEXT
	};

	private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID ITEM_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

	private final CourseDTOResultTransformer sut = new CourseDTOResultTransformer();

	private Object[] tuple(Object type, String title, String text) {
		return new Object[]{COURSE_UUID, "name", "description", new NumberOfStudents(5), 1, ITEM_UUID, title, "item description", type, text};
	}

	@Test
	void transformTuple_classDiscriminator_lectureWithTextAdded() {
		// when
		var result = sut.transformTuple(tuple(Lecture.class, "lecture", "lecture text"), ALIASES);

		// then
		assertThat(result.uuid()).isEqualTo(COURSE_UUID);
		assertThat(result.curriculumItems()).hasSize(1);
		var item = result.curriculumItems().get(0);
		assertThat(item).isInstanceOf(LectureDTO.class);
		assertThat(((LectureDTO) item).text).isEqualTo("lecture text");
		assertThat(item.title).isEqualTo("lecture");
		assertThat(item.uuid).isEqualTo(ITEM_UUID);
	}

	@Test
	void transformTuple_quizClassDiscriminator_quizAdded() {
		// when
		var result = sut.transformTuple(tuple(Quiz.class, "quiz", null), ALIASES);

		// then
		assertThat(result.curriculumItems()).hasSize(1);
		assertThat(result.curriculumItems().get(0)).isInstanceOf(QuizDTO.class);
	}

	@Test
	void transformTuple_stringDiscriminator_lectureAdded() {
		// when
		var result = sut.transformTuple(tuple("Lecture", "lecture", "text"), ALIASES);

		// then
		assertThat(result.curriculumItems()).hasSize(1);
		assertThat(result.curriculumItems().get(0)).isInstanceOf(LectureDTO.class);
	}

	@Test
	void transformTuple_nullDiscriminator_courseWithoutItems() {
		// when
		var result = sut.transformTuple(tuple(null, null, null), ALIASES);

		// then
		assertThat(result.uuid()).isEqualTo(COURSE_UUID);
		assertThat(result.name()).isEqualTo("name");
		assertThat(result.curriculumItems()).isEmpty();
	}

	@Test
	void transformTuple_unknownDiscriminator_itemSkipped() {
		// when
		var result = sut.transformTuple(tuple("Unknown", "title", null), ALIASES);

		// then
		assertThat(result.curriculumItems()).isEmpty();
	}

	@Test
	void transformTuple_multipleTuplesSameCourse_singleDtoAccumulatesItems() {
		// when
		sut.transformTuple(tuple(Lecture.class, "lecture 1", "text 1"), ALIASES);
		var result = sut.transformTuple(tuple(Lecture.class, "lecture 2", "text 2"), ALIASES);

		// then
		assertThat(result.curriculumItems()).hasSize(2);
	}
}
