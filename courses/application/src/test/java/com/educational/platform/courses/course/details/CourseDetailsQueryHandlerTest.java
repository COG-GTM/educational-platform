package com.educational.platform.courses.course.details;

import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseDetailsQueryHandlerTest {

	private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	@Mock
	private CourseRepository repository;

	@InjectMocks
	private CourseDetailsQueryHandler sut;

	@Test
	void handle_publishedCourse_detailsWithCurriculumReturned() {
		// given
		final LocalDateTime publishedDate = LocalDateTime.of(2026, 10, 6, 12, 0);
		final CourseDetailsDTO details = new CourseDetailsDTO(COURSE_UUID, "name", "description", "Programming", "teacher", 4.5, 3, publishedDate);
		final List<CurriculumItemSummaryDTO> curriculum = List.of(
				new CurriculumItemSummaryDTO(UUID.randomUUID(), "Intro lecture", "lecture description", 1, "LECTURE"),
				new CurriculumItemSummaryDTO(UUID.randomUUID(), "Intro quiz", "quiz description", 2, "QUIZ"));
		when(repository.findPublishedDetails(COURSE_UUID)).thenReturn(Optional.of(details));
		when(repository.findCurriculum(COURSE_UUID)).thenReturn(curriculum);

		// when
		final Optional<CourseDetailsDTO> result = sut.handle(new CourseDetailsQuery(COURSE_UUID));

		// then
		assertThat(result).isPresent();
		assertThat(result.get())
				.hasFieldOrPropertyWithValue("uuid", COURSE_UUID)
				.hasFieldOrPropertyWithValue("name", "name")
				.hasFieldOrPropertyWithValue("description", "description")
				.hasFieldOrPropertyWithValue("category", "Programming")
				.hasFieldOrPropertyWithValue("teacherName", "teacher")
				.hasFieldOrPropertyWithValue("rating", 4.5)
				.hasFieldOrPropertyWithValue("numberOfStudents", 3)
				.hasFieldOrPropertyWithValue("publishedDate", publishedDate);
		assertThat(result.get().curriculumItems()).containsExactlyElementsOf(curriculum);
	}

	@Test
	void handle_publishedCourseWithoutCurriculum_emptyCurriculumReturned() {
		// given
		final CourseDetailsDTO details = new CourseDetailsDTO(COURSE_UUID, "name", "description", null, "teacher", 0, 0, null);
		when(repository.findPublishedDetails(COURSE_UUID)).thenReturn(Optional.of(details));
		when(repository.findCurriculum(COURSE_UUID)).thenReturn(List.of());

		// when
		final Optional<CourseDetailsDTO> result = sut.handle(new CourseDetailsQuery(COURSE_UUID));

		// then
		assertThat(result).isPresent();
		assertThat(result.get().curriculumItems()).isEmpty();
		assertThat(result.get().category()).isNull();
		assertThat(result.get().publishedDate()).isNull();
	}

	@Test
	void handle_courseNotPublishedOrUnknown_emptyAndCurriculumNotQueried() {
		// given
		when(repository.findPublishedDetails(COURSE_UUID)).thenReturn(Optional.empty());

		// when
		final Optional<CourseDetailsDTO> result = sut.handle(new CourseDetailsQuery(COURSE_UUID));

		// then
		assertThat(result).isEmpty();
		verify(repository, never()).findCurriculum(any());
	}
}
