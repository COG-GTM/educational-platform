package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

@DataJpaTest
public class CourseRepositoryTest {

	private static final String TEACHER = "teacher";

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void queryDtoByUUID_validQuery_dtoRetrieved() {
		// given
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		var createCourseCommand = CreateCourseCommand.builder().name("name").description("description").build();
		var course = new Course(createCourseCommand, teacher.getId());
		courseRepository.save(course);

		// when
		var result = courseRepository.findDTOByUuid(course.toIdentity());

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.get()).hasFieldOrPropertyWithValue("name", "name").hasFieldOrPropertyWithValue("description", "description");
	}

	@ParameterizedTest
	@CsvSource({"3.49, 3.0", "3.50, 4.0"})
	void recalculateRating_existingCourse_roundedRatingPersisted(double rating, double expectedRating) {
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var command = CreateCourseCommand.builder().name("name").description("description").build();
		var course = courseRepository.save(new Course(command, teacher.getId()));
		var courseId = course.toIdentity();
		var handler = new UpdateCourseRatingCommandHandler(courseRepository);
		var listener = new CourseRatingRecalculatedIntegrationEventHandler(handler);

		listener.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(courseId, rating));
		entityManager.flush();
		entityManager.clear();

		assertThat(courseRepository.findByUuid(courseId)).hasValueSatisfying(reloaded ->
				assertThat(reloaded).hasFieldOrPropertyWithValue("rating", new CourseRating(expectedRating)));
	}

	@Test
	void recalculateRating_previouslyRatedCourse_persistedRatingReplacedAndOtherFieldsKept() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var command = CreateCourseCommand.builder().name("name").description("description").build();
		var courseId = courseRepository.save(new Course(command, teacher.getId())).toIdentity();
		var listener = new CourseRatingRecalculatedIntegrationEventHandler(new UpdateCourseRatingCommandHandler(courseRepository));
		listener.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5));
		entityManager.flush();
		entityManager.clear();

		// when
		listener.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(courseId, 2.49));
		entityManager.flush();
		entityManager.clear();

		// then
		assertThat(courseRepository.findByUuid(courseId)).hasValueSatisfying(reloaded -> assertThat(reloaded)
				.hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
				.hasFieldOrPropertyWithValue("name", "name")
				.hasFieldOrPropertyWithValue("description", "description"));
	}

	@Test
	void recalculateRating_unknownCourse_resourceNotFoundAndExistingCourseUnchanged() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var command = CreateCourseCommand.builder().name("name").description("description").build();
		var courseId = courseRepository.save(new Course(command, teacher.getId())).toIdentity();
		entityManager.flush();
		entityManager.clear();
		var unknownId = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
		var listener = new CourseRatingRecalculatedIntegrationEventHandler(new UpdateCourseRatingCommandHandler(courseRepository));
		var event = new CourseRatingRecalculatedIntegrationEvent(unknownId, 4.5);

		// when / then
		assertThatExceptionOfType(ResourceNotFoundException.class)
				.isThrownBy(() -> listener.handleCourseRatingRecalculatedEvent(event))
				.withMessageContaining(unknownId.toString());
		entityManager.flush();
		entityManager.clear();
		assertThat(courseRepository.findByUuid(unknownId)).isEmpty();
		assertThat(courseRepository.findByUuid(courseId)).hasValueSatisfying(reloaded ->
				assertThat(reloaded).hasFieldOrPropertyWithValue("rating", new CourseRating(0.0)));
	}

}
