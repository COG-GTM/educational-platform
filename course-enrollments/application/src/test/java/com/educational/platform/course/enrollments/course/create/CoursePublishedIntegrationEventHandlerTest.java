package com.educational.platform.course.enrollments.course.create;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.courses.integration.event.CoursePublishedIntegrationEvent;

@Sql(scripts = "classpath:course.sql")
@SpringBootTest
@Transactional
public class CoursePublishedIntegrationEventHandlerTest {

	private static final UUID KNOWN_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	@Autowired
	private CoursePublishedIntegrationEventHandler sut;

	@Autowired
	private EnrollCourseRepository repository;

	@Test
	void handle_newCourse_courseWithLecturesCreated() {
		// given
		final UUID uuid = UUID.randomUUID();
		final UUID lecture = UUID.randomUUID();

		// when
		sut.handleCoursePublishedEvent(new CoursePublishedIntegrationEvent(uuid, "Kotlin", List.of(
				new CoursePublishedIntegrationEvent.Lecture(lecture, "Intro", 1))));

		// then
		var course = repository.findByUuid(uuid).orElseThrow();
		assertThat(course.getName()).isEqualTo("Kotlin");
		assertThat(course.getLectures()).extracting("uuid").containsExactly(lecture);
	}

	@Test
	void handle_knownCourse_snapshotRefreshedWithoutDuplicate() {
		// when
		sut.handleCoursePublishedEvent(new CoursePublishedIntegrationEvent(KNOWN_COURSE, "Java Basics (2nd edition)", List.of(
				new CoursePublishedIntegrationEvent.Lecture(UUID.fromString("223e4567-e89b-12d3-a456-426655440001"), "Intro", 1))));

		// then
		assertThat(repository.findAll()).filteredOn(c -> c.toReference().equals(KNOWN_COURSE)).hasSize(1);
		var course = repository.findByUuid(KNOWN_COURSE).orElseThrow();
		assertThat(course.getName()).isEqualTo("Java Basics (2nd edition)");
		assertThat(course.getLectures()).hasSize(1);
	}
}
