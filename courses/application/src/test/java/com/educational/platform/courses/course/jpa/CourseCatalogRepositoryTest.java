package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.catalog.CourseCatalogItemDTO;
import com.educational.platform.courses.course.catalog.CourseCatalogQuery;
import com.educational.platform.courses.course.catalog.CourseCatalogSort;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
public class CourseCatalogRepositoryTest {

	private static final String TEACHER = "teacher";
	private static final String ANOTHER_TEACHER = "another-teacher";

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	private Teacher teacher;
	private Teacher anotherTeacher;

	@BeforeEach
	void setUp() {
		courseRepository.deleteAll();
		teacherRepository.deleteAll();
		teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		anotherTeacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(ANOTHER_TEACHER)));
	}

	@Test
	void findCatalog_onlyPublishedCoursesReturned() {
		savePublished("Java Basics", "Learn Java", "Programming", teacher);
		saveDraft("Hidden Draft", "Not visible", "Programming", teacher);

		var result = courseRepository.findCatalog(query(null, null, null, null, CourseCatalogSort.NEWEST, 0, 10));

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.items()).extracting(CourseCatalogItemDTO::name).containsExactly("Java Basics");
		assertThat(result.items().getFirst().teacherName()).isEqualTo(TEACHER);
	}

	@Test
	void findCatalog_searchMatchesTitleAndDescription() {
		savePublished("Java Basics", "Learn programming", "Programming", teacher);
		savePublished("Cooking", "Kitchen fundamentals with java coffee", "Lifestyle", teacher);
		savePublished("Painting", "Art for beginners", "Art", teacher);

		var result = courseRepository.findCatalog(query("java", null, null, null, CourseCatalogSort.NEWEST, 0, 10));

		assertThat(result.items()).extracting(CourseCatalogItemDTO::name).containsExactlyInAnyOrder("Java Basics", "Cooking");
	}

	@Test
	void findCatalog_filtersByCategoryTeacherAndMinRating() {
		var lowRated = savePublished("Java Basics", "Learn Java", "Programming", teacher);
		lowRated.updateRating(2.0);
		var highRated = savePublished("Advanced Java", "Deep dive", "Programming", teacher);
		highRated.updateRating(4.5);
		savePublished("Painting", "Art for beginners", "Art", anotherTeacher);
		courseRepository.save(lowRated);
		courseRepository.save(highRated);

		var byCategory = courseRepository.findCatalog(query(null, "Programming", null, null, CourseCatalogSort.NEWEST, 0, 10));
		var byTeacher = courseRepository.findCatalog(query(null, null, ANOTHER_TEACHER, null, CourseCatalogSort.NEWEST, 0, 10));
		var byRating = courseRepository.findCatalog(query(null, null, null, 4.0, CourseCatalogSort.NEWEST, 0, 10));

		assertThat(byCategory.items()).extracting(CourseCatalogItemDTO::name).containsExactlyInAnyOrder("Java Basics", "Advanced Java");
		assertThat(byTeacher.items()).extracting(CourseCatalogItemDTO::name).containsExactly("Painting");
		assertThat(byRating.items()).extracting(CourseCatalogItemDTO::name).containsExactly("Advanced Java");
	}

	@Test
	void findCatalog_sortsByRating() {
		var low = savePublished("Low", "low rated", null, teacher);
		low.updateRating(1.0);
		var high = savePublished("High", "high rated", null, teacher);
		high.updateRating(5.0);
		courseRepository.save(low);
		courseRepository.save(high);

		var result = courseRepository.findCatalog(query(null, null, null, null, CourseCatalogSort.RATING, 0, 10));

		assertThat(result.items()).extracting(CourseCatalogItemDTO::name).containsExactly("High", "Low");
	}

	@Test
	void findCatalog_paginationReturnsTotals() {
		for (int i = 0; i < 5; i++) {
			savePublished("Course " + i, "description", null, teacher);
		}

		var result = courseRepository.findCatalog(query(null, null, null, null, CourseCatalogSort.NEWEST, 1, 2));

		assertThat(result.totalElements()).isEqualTo(5);
		assertThat(result.totalPages()).isEqualTo(3);
		assertThat(result.page()).isEqualTo(1);
		assertThat(result.items()).hasSize(2);
	}

	@Test
	void publishedCategoriesAndTeachers_returnedDistinctSorted() {
		savePublished("Java Basics", "Learn Java", "Programming", teacher);
		savePublished("Advanced Java", "Deep dive", "Programming", teacher);
		savePublished("Painting", "Art for beginners", "Art", anotherTeacher);
		saveDraft("Hidden Draft", "Not visible", "Hidden", teacher);

		assertThat(courseRepository.publishedCategories()).containsExactly("Art", "Programming");
		assertThat(courseRepository.publishedTeachers()).containsExactly(ANOTHER_TEACHER, TEACHER);
	}

	private CourseCatalogQuery query(String search, String category, String teacherUsername, Double minRating, CourseCatalogSort sort, int page, int size) {
		return new CourseCatalogQuery(search, category, teacherUsername, minRating, sort, page, size);
	}

	private Course savePublished(String name, String description, String category, Teacher courseTeacher) {
		var course = course(name, description, category, courseTeacher);
		course.approve();
		course.publish();
		return courseRepository.save(course);
	}

	private Course saveDraft(String name, String description, String category, Teacher courseTeacher) {
		return courseRepository.save(course(name, description, category, courseTeacher));
	}

	private Course course(String name, String description, String category, Teacher courseTeacher) {
		var command = CreateCourseCommand.builder().name(name).description(description).category(category).build();
		return new Course(command, courseTeacher.getId());
	}

}
