package com.educational.platform.courses.course.details;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDetailsDTOTest {

	private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final LocalDateTime PUBLISHED_DATE = LocalDateTime.of(2026, 10, 6, 12, 0);

	@Test
	void constructor_withoutCurriculum_emptyCurriculumItems() {
		// given/when
		final CourseDetailsDTO details = new CourseDetailsDTO(COURSE_UUID, "name", "description", "Programming", "teacher", 4.5, 3, PUBLISHED_DATE);

		// then
		assertThat(details.curriculumItems()).isNotNull().isEmpty();
	}

	@Test
	void withCurriculumItems_itemsAttachedAndOtherFieldsPreserved() {
		// given
		final CourseDetailsDTO details = new CourseDetailsDTO(COURSE_UUID, "name", "description", "Programming", "teacher", 4.5, 3, PUBLISHED_DATE);
		final List<CurriculumItemSummaryDTO> items = List.of(
				new CurriculumItemSummaryDTO(UUID.randomUUID(), "Intro lecture", null, 1, "LECTURE"),
				new CurriculumItemSummaryDTO(UUID.randomUUID(), "Intro quiz", "quiz description", 2, "QUIZ"));

		// when
		final CourseDetailsDTO result = details.withCurriculumItems(items);

		// then
		assertThat(result.curriculumItems()).containsExactlyElementsOf(items);
		assertThat(result)
				.hasFieldOrPropertyWithValue("uuid", COURSE_UUID)
				.hasFieldOrPropertyWithValue("name", "name")
				.hasFieldOrPropertyWithValue("description", "description")
				.hasFieldOrPropertyWithValue("category", "Programming")
				.hasFieldOrPropertyWithValue("teacherName", "teacher")
				.hasFieldOrPropertyWithValue("rating", 4.5)
				.hasFieldOrPropertyWithValue("numberOfStudents", 3)
				.hasFieldOrPropertyWithValue("publishedDate", PUBLISHED_DATE);
	}

	@Test
	void withCurriculumItems_originalInstanceUnchanged() {
		// given
		final CourseDetailsDTO details = new CourseDetailsDTO(COURSE_UUID, "name", "description", null, "teacher", 0, 0, null);
		final List<CurriculumItemSummaryDTO> items = List.of(new CurriculumItemSummaryDTO(UUID.randomUUID(), "Intro lecture", null, 1, "LECTURE"));

		// when
		final CourseDetailsDTO result = details.withCurriculumItems(items);

		// then
		assertThat(result).isNotSameAs(details);
		assertThat(details.curriculumItems()).isEmpty();
	}
}
