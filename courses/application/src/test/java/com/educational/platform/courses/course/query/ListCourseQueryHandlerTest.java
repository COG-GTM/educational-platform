package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private ListCourseQueryHandler sut;

    @Test
    void handle_coursesExist_listReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO course1 = new CourseLightDTO(UUID.randomUUID(), "course1", "description1", 10);
        final CourseLightDTO course2 = new CourseLightDTO(UUID.randomUUID(), "course2", "description2", 20);
        when(repository.list()).thenReturn(List.of(course1, course2));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(course1, course2);
    }

    @Test
    void handle_singleCourseExists_singletonListReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), "single-course", "only course", 5);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(course);
    }

    @Test
    void handle_noCoursesExist_emptyListReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenReturn(List.of());

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_delegatesToRepositoryListMethod() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenReturn(List.of());

        // when
        sut.handle(query);

        // then
        verify(repository).list();
    }

    @Test
    void handle_coursesExist_preservesReturnedOrder() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO first = new CourseLightDTO(UUID.randomUUID(), "alpha", "first", 1);
        final CourseLightDTO second = new CourseLightDTO(UUID.randomUUID(), "beta", "second", 2);
        final CourseLightDTO third = new CourseLightDTO(UUID.randomUUID(), "gamma", "third", 3);
        when(repository.list()).thenReturn(List.of(first, second, third));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).containsExactly(first, second, third);
    }

    @Test
    void handle_coursesExist_dtoFieldsAccessible() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseLightDTO course = new CourseLightDTO(uuid, "Java Basics", "Intro to Java", 42);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        final CourseLightDTO returned = result.getFirst();
        assertThat(returned.uuid()).isEqualTo(uuid);
        assertThat(returned.name()).isEqualTo("Java Basics");
        assertThat(returned.description()).isEqualTo("Intro to Java");
        assertThat(returned.numberOfStudents()).isEqualTo(42);
    }

    @Test
    void handle_returnedListIsSameInstanceFromRepository() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final List<CourseLightDTO> repositoryList = List.of(
                new CourseLightDTO(UUID.randomUUID(), "course", "desc", 1));
        when(repository.list()).thenReturn(repositoryList);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(repositoryList);
    }

    @Test
    void handle_nullQuery_delegatesToRepository() {
        // given
        when(repository.list()).thenReturn(List.of());

        // when
        final List<CourseLightDTO> result = sut.handle(null);

        // then
        assertThat(result).isEmpty();
        verify(repository).list();
    }

    @Test
    void handle_calledMultipleTimes_delegatesEachCall() {
        // given
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), "c", "d", 1);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result1 = sut.handle(new ListCourseQuery());
        final List<CourseLightDTO> result2 = sut.handle(new ListCourseQuery());

        // then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        verify(repository, org.mockito.Mockito.times(2)).list();
    }

    @Test
    void handle_repositoryCalledExactlyOnce_noExtraInteractions() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenReturn(List.of());

        // when
        sut.handle(query);

        // then
        verify(repository).list();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void handle_courseWithZeroStudents_includedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO zeroStudentsCourse = new CourseLightDTO(UUID.randomUUID(), "new course", "just created", 0);
        when(repository.list()).thenReturn(List.of(zeroStudentsCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().numberOfStudents()).isZero();
    }

    @Test
    void handle_largeCourseList_allItemsReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final List<CourseLightDTO> courses = java.util.stream.IntStream.rangeClosed(1, 100)
                .mapToObj(i -> new CourseLightDTO(UUID.randomUUID(), "course-" + i, "desc-" + i, i))
                .toList();
        when(repository.list()).thenReturn(courses);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(100);
        assertThat(result.getFirst().name()).isEqualTo("course-1");
        assertThat(result.getLast().name()).isEqualTo("course-100");
    }

    @Test
    void handle_queryParameterIsIgnored_resultDependsOnlyOnRepository() {
        // given
        final ListCourseQuery query1 = new ListCourseQuery();
        final ListCourseQuery query2 = new ListCourseQuery();
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), "c", "d", 1);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result1 = sut.handle(query1);
        final List<CourseLightDTO> result2 = sut.handle(query2);

        // then
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void handle_courseWithNullFields_includedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO nullFieldsCourse = new CourseLightDTO(null, null, null, 0);
        when(repository.list()).thenReturn(List.of(nullFieldsCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().uuid()).isNull();
        assertThat(result.getFirst().name()).isNull();
        assertThat(result.getFirst().description()).isNull();
    }

}
