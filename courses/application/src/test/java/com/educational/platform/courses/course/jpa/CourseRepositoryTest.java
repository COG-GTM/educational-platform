package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
public class CourseRepositoryTest {

	private static final String TEACHER = "teacher";

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TestEntityManager entityManager;

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
	void findDTOByUuid_missingUuid_emptyOptional() {
		// when
		var result = courseRepository.findDTOByUuid(UUID.fromString("123e4567-e89b-12d3-a456-426655440999"));

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void findDTOByUuid_courseWithMultipleCurriculumItems_singleDto() {
		// given
		dropInvalidCurriculumItemCheckConstraints();
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		var createCourseCommand = CreateCourseCommand.builder()
				.name("name")
				.description("description")
				.curriculumItems(List.of(
						CreateLectureCommand.builder().title("lecture 1").description("lecture 1 description").serialNumber(1).text("text 1").build(),
						CreateLectureCommand.builder().title("lecture 2").description("lecture 2 description").serialNumber(2).text("text 2").build()))
				.build();
		var course = new Course(createCourseCommand, teacher.getId());
		courseRepository.save(course);
		entityManager.flush();
		entityManager.clear();

		// when
		var result = courseRepository.findDTOByUuid(course.toIdentity());

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.get()).hasFieldOrPropertyWithValue("name", "name").hasFieldOrPropertyWithValue("description", "description");
	}

	// Hibernate generates a discriminator check constraint on curriculum_item that H2 cannot evaluate,
	// rejecting every insert into the single-table hierarchy. Drop it so child rows can be persisted.
	@SuppressWarnings("unchecked")
	private void dropInvalidCurriculumItemCheckConstraints() {
		var em = entityManager.getEntityManager();
		var names = (List<String>) em
				.createNativeQuery("select constraint_name from information_schema.table_constraints where table_name = 'CURRICULUM_ITEM' and constraint_type = 'CHECK'")
				.getResultList();
		for (var name : names) {
			em.createNativeQuery("alter table curriculum_item drop constraint \"" + name + "\"").executeUpdate();
		}
	}

	@Test
	void list_paged_pageOfCourses() {
		// given
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		for (int i = 0; i < 3; i++) {
			var createCourseCommand = CreateCourseCommand.builder().name("name" + i).description("description" + i).build();
			var course = new Course(createCourseCommand, teacher.getId());
			course.approve();
			course.publish();
			courseRepository.save(course);
		}

		// when
		var result = courseRepository.list(PageRequest.of(0, 2));

		// then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getTotalElements()).isEqualTo(3);
	}

	@Test
	void list_draftAndPublishedCourses_onlyPublishedReturned() {
		// given
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		var draft = new Course(CreateCourseCommand.builder().name("draft").description("draft description").build(), teacher.getId());
		courseRepository.save(draft);
		var published = new Course(CreateCourseCommand.builder().name("published").description("published description").build(), teacher.getId());
		published.approve();
		published.publish();
		courseRepository.save(published);

		// when
		var result = courseRepository.list(PageRequest.of(0, 20));

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).extracting(CourseLightDTO::name).containsExactly("published");
	}

	@Test
	void list_secondPage_remainingCoursesInStableOrder() {
		// given
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		for (int i = 0; i < 3; i++) {
			var course = new Course(CreateCourseCommand.builder().name("name" + i).description("description" + i).build(), teacher.getId());
			course.approve();
			course.publish();
			courseRepository.save(course);
		}

		// when
		var result = courseRepository.list(PageRequest.of(1, 2));

		// then
		assertThat(result.getContent()).extracting(CourseLightDTO::name).containsExactly("name2");
		assertThat(result.getTotalElements()).isEqualTo(3);
		assertThat(result.getTotalPages()).isEqualTo(2);
	}

	@Test
	void findByUuid_persistedCourse_curriculumItemsLazilyLoaded() {
		// given
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		var createCourseCommand = CreateCourseCommand.builder().name("name").description("description").build();
		var course = new Course(createCourseCommand, teacher.getId());
		courseRepository.save(course);
		entityManager.flush();
		entityManager.clear();

		// when
		var result = courseRepository.findByUuid(course.toIdentity());

		// then
		assertThat(result).isNotEmpty();
		var persistenceUnitUtil = entityManager.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
		assertThat(persistenceUnitUtil.isLoaded(result.get(), "curriculumItems")).isFalse();
	}

}
