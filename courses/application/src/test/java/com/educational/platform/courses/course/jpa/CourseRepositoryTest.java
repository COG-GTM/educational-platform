package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

	@Test
	void recalculateRating_existingCourse_fractionalRatingPersisted() {
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var command = CreateCourseCommand.builder().name("name").description("description").build();
		var course = courseRepository.save(new Course(command, teacher.getId()));
		var courseId = course.toIdentity();
		var listener = new CourseRatingRecalculatedIntegrationEventHandler(new UpdateCourseRatingCommandHandler(courseRepository));

		listener.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(courseId, 3.7));
		entityManager.flush();
		entityManager.clear();

		assertThat(courseRepository.findByUuid(courseId)).hasValueSatisfying(reloaded ->
				assertThat(reloaded).hasFieldOrPropertyWithValue("rating", new CourseRating(3.7)));
	}

}
