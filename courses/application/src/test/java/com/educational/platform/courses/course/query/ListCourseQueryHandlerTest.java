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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void handle_coursesWithDuplicateNames_allReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO dup1 = new CourseLightDTO(UUID.randomUUID(), "same-name", "desc1", 1);
        final CourseLightDTO dup2 = new CourseLightDTO(UUID.randomUUID(), "same-name", "desc2", 2);
        final CourseLightDTO dup3 = new CourseLightDTO(UUID.randomUUID(), "same-name", "desc3", 3);
        when(repository.list()).thenReturn(List.of(dup1, dup2, dup3));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(CourseLightDTO::name)
                .containsExactly("same-name", "same-name", "same-name");
    }

    @Test
    void handle_courseWithNegativeStudents_includedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO negativeCourse = new CourseLightDTO(UUID.randomUUID(), "negative", "desc", -5);
        when(repository.list()).thenReturn(List.of(negativeCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().numberOfStudents()).isEqualTo(-5);
    }

    @Test
    void handle_coursesWithSameUuid_allReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final UUID sharedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        final CourseLightDTO c1 = new CourseLightDTO(sharedUuid, "course1", "desc1", 1);
        final CourseLightDTO c2 = new CourseLightDTO(sharedUuid, "course2", "desc2", 2);
        when(repository.list()).thenReturn(List.of(c1, c2));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(c1, c2);
    }

    @Test
    void handle_repositoryThrowsException_exceptionPropagated() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenThrow(new RuntimeException("db error"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    void handle_courseWithMaxIntStudents_includedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO maxStudentsCourse = new CourseLightDTO(UUID.randomUUID(), "popular", "desc", Integer.MAX_VALUE);
        when(repository.list()).thenReturn(List.of(maxStudentsCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().numberOfStudents()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void handle_coursesWithEmptyStringFields_preservedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO emptyFields = new CourseLightDTO(UUID.randomUUID(), "", "", 0);
        when(repository.list()).thenReturn(List.of(emptyFields));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEmpty();
        assertThat(result.getFirst().description()).isEmpty();
    }

    @Test
    void handle_mixedNullAndPopulatedCourses_allReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO normal = new CourseLightDTO(UUID.randomUUID(), "Java", "Intro", 10);
        final CourseLightDTO nullFields = new CourseLightDTO(null, null, null, 0);
        final CourseLightDTO emptyFields = new CourseLightDTO(UUID.randomUUID(), "", "", 0);
        when(repository.list()).thenReturn(List.of(normal, nullFields, emptyFields));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("Java");
        assertThat(result.get(1).name()).isNull();
        assertThat(result.get(2).name()).isEmpty();
    }

    @Test
    void handle_repositoryReturnsMinIntStudents_includedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO minCourse = new CourseLightDTO(UUID.randomUUID(), "min", "desc", Integer.MIN_VALUE);
        when(repository.list()).thenReturn(List.of(minCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().numberOfStudents()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void handle_repositoryThrowsIllegalStateException_exceptionPropagated() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenThrow(new IllegalStateException("connection closed"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("connection closed");
    }

    @Test
    void handle_courseWithLongNameAndDescription_preservedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final String longName = "N".repeat(1000);
        final String longDesc = "D".repeat(5000);
        final CourseLightDTO longCourse = new CourseLightDTO(UUID.randomUUID(), longName, longDesc, 1);
        when(repository.list()).thenReturn(List.of(longCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).hasSize(1000);
        assertThat(result.getFirst().description()).hasSize(5000);
    }

    @Test
    void handle_repositoryReturnsListWithNullElement_returnedAsIs() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final java.util.ArrayList<CourseLightDTO> listWithNull = new java.util.ArrayList<>();
        listWithNull.add(new CourseLightDTO(UUID.randomUUID(), "course", "desc", 1));
        listWithNull.add(null);
        when(repository.list()).thenReturn(listWithNull);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isNotNull();
        assertThat(result.get(1)).isNull();
    }

    @Test
    void handle_courseWithWhitespaceOnlyFields_preservedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO whitespaceCourse = new CourseLightDTO(UUID.randomUUID(), "   ", "\t\n", 0);
        when(repository.list()).thenReturn(List.of(whitespaceCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("   ");
        assertThat(result.getFirst().description()).isEqualTo("\t\n");
    }

    @Test
    void handle_repositoryReturnsMutableList_returnedDirectly() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final java.util.ArrayList<CourseLightDTO> mutableList = new java.util.ArrayList<>();
        mutableList.add(new CourseLightDTO(UUID.randomUUID(), "course", "desc", 1));
        when(repository.list()).thenReturn(mutableList);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(mutableList);
    }

    @Test
    void handle_consecutiveCallsReturnFreshResults() {
        // given
        final CourseLightDTO course1 = new CourseLightDTO(UUID.randomUUID(), "first", "desc1", 1);
        final CourseLightDTO course2 = new CourseLightDTO(UUID.randomUUID(), "second", "desc2", 2);
        when(repository.list())
                .thenReturn(List.of(course1))
                .thenReturn(List.of(course2));

        // when
        final List<CourseLightDTO> result1 = sut.handle(new ListCourseQuery());
        final List<CourseLightDTO> result2 = sut.handle(new ListCourseQuery());

        // then
        assertThat(result1).hasSize(1);
        assertThat(result1.getFirst().name()).isEqualTo("first");
        assertThat(result2).hasSize(1);
        assertThat(result2.getFirst().name()).isEqualTo("second");
    }

    @Test
    void handle_courseWithUnicodeFields_preservedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO unicodeCourse = new CourseLightDTO(UUID.randomUUID(), "日本語コース", "Описание αβγ", 5);
        when(repository.list()).thenReturn(List.of(unicodeCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("日本語コース");
        assertThat(result.getFirst().description()).isEqualTo("Описание αβγ");
    }

    @Test
    void handle_repositoryThrowsNullPointerException_exceptionPropagated() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenThrow(new NullPointerException("unexpected null"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("unexpected null");
    }

    @Test
    void handle_firstCallEmpty_secondCallPopulated_bothDelegated() {
        // given
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), "c", "d", 1);
        when(repository.list())
                .thenReturn(List.of())
                .thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result1 = sut.handle(new ListCourseQuery());
        final List<CourseLightDTO> result2 = sut.handle(new ListCourseQuery());

        // then
        assertThat(result1).isEmpty();
        assertThat(result2).hasSize(1);
        verify(repository, org.mockito.Mockito.times(2)).list();
    }

    @Test
    void handle_courseWithOneStudent_includedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO singleStudentCourse = new CourseLightDTO(UUID.randomUUID(), "intro", "desc", 1);
        when(repository.list()).thenReturn(List.of(singleStudentCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().numberOfStudents()).isEqualTo(1);
    }

    @Test
    void handle_twoCoursesInReverseAlphaOrder_orderPreservedFromRepository() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO zCourse = new CourseLightDTO(UUID.randomUUID(), "Zebra", "z desc", 1);
        final CourseLightDTO aCourse = new CourseLightDTO(UUID.randomUUID(), "Alpha", "a desc", 2);
        when(repository.list()).thenReturn(List.of(zCourse, aCourse));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).containsExactly(zCourse, aCourse);
        assertThat(result.get(0).name()).isEqualTo("Zebra");
        assertThat(result.get(1).name()).isEqualTo("Alpha");
    }

    @Test
    void handle_courseWithEmptyNameNonEmptyDescription_preservedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), "", "has description", 3);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEmpty();
        assertThat(result.getFirst().description()).isEqualTo("has description");
    }

    @Test
    void handle_consecutiveCallsReturnDifferentSizedLists_bothDelegated() {
        // given
        final CourseLightDTO c1 = new CourseLightDTO(UUID.randomUUID(), "c1", "d1", 1);
        final CourseLightDTO c2 = new CourseLightDTO(UUID.randomUUID(), "c2", "d2", 2);
        final CourseLightDTO c3 = new CourseLightDTO(UUID.randomUUID(), "c3", "d3", 3);
        when(repository.list())
                .thenReturn(List.of(c1))
                .thenReturn(List.of(c1, c2, c3));

        // when
        final List<CourseLightDTO> result1 = sut.handle(new ListCourseQuery());
        final List<CourseLightDTO> result2 = sut.handle(new ListCourseQuery());

        // then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(3);
        verify(repository, org.mockito.Mockito.times(2)).list();
    }

    @Test
    void handle_repositoryReturnsNull_nullPropagated() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenReturn(null);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isNull();
    }

    @Test
    void handle_repositoryReturnsUnmodifiableList_returnedAsIs() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final List<CourseLightDTO> unmodifiable = java.util.Collections.unmodifiableList(
                List.of(new CourseLightDTO(UUID.randomUUID(), "course", "desc", 5)));
        when(repository.list()).thenReturn(unmodifiable);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(unmodifiable);
        assertThat(result).hasSize(1);
    }

    @Test
    void handle_courseWithSpecialCharsInFields_preservedInResults() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final String specialName = "C++ & Java <> \"Rust\" 'Go' \\ /path";
        final String specialDesc = "SELECT * FROM courses; DROP TABLE --";
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), specialName, specialDesc, 1);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo(specialName);
        assertThat(result.getFirst().description()).isEqualTo(specialDesc);
    }

    @Test
    void handle_handlerDoesNotSortOrFilter_reverseOrderPreserved() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO z = new CourseLightDTO(UUID.randomUUID(), "Zebra", "z", 100);
        final CourseLightDTO m = new CourseLightDTO(UUID.randomUUID(), "Mango", "m", 50);
        final CourseLightDTO a = new CourseLightDTO(UUID.randomUUID(), "Apple", "a", 1);
        when(repository.list()).thenReturn(List.of(z, m, a));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).extracting(CourseLightDTO::name)
                .containsExactly("Zebra", "Mango", "Apple");
    }

    @Test
    void handle_repositoryThrowsOutOfMemoryError_errorPropagated() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenThrow(new OutOfMemoryError("test OOM"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(OutOfMemoryError.class)
                .hasMessage("test OOM");
    }

    @Test
    void handle_repositoryReturnsSingletonList_returnedAsIs() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final List<CourseLightDTO> singletonList = java.util.Collections.singletonList(
                new CourseLightDTO(UUID.randomUUID(), "only", "desc", 1));
        when(repository.list()).thenReturn(singletonList);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(singletonList);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("only");
    }

    @Test
    void handle_threeConsecutiveCalls_allDelegatedToRepository() {
        // given
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), "c", "d", 1);
        when(repository.list()).thenReturn(List.of(course));

        // when
        sut.handle(new ListCourseQuery());
        sut.handle(new ListCourseQuery());
        sut.handle(new ListCourseQuery());

        // then
        verify(repository, org.mockito.Mockito.times(3)).list();
    }

    @Test
    void handle_repositoryThrowsStackOverflowError_errorPropagated() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenThrow(new StackOverflowError("test stack overflow"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(StackOverflowError.class)
                .hasMessage("test stack overflow");
    }

    @Test
    void handle_repositoryReturnsLinkedList_returnedDirectly() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final java.util.LinkedList<CourseLightDTO> linkedList = new java.util.LinkedList<>();
        linkedList.add(new CourseLightDTO(UUID.randomUUID(), "course1", "desc1", 1));
        linkedList.add(new CourseLightDTO(UUID.randomUUID(), "course2", "desc2", 2));
        when(repository.list()).thenReturn(linkedList);

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(linkedList);
        assertThat(result).hasSize(2);
    }

    @Test
    void handle_repositoryReturnsListWithAllNullFieldDtos_returnedAsIs() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO nullDto1 = new CourseLightDTO(null, null, null, 0);
        final CourseLightDTO nullDto2 = new CourseLightDTO(null, null, null, 0);
        when(repository.list()).thenReturn(List.of(nullDto1, nullDto2));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.uuid()).isNull();
            assertThat(dto.name()).isNull();
            assertThat(dto.description()).isNull();
            assertThat(dto.numberOfStudents()).isZero();
        });
    }

    @Test
    void handle_repositoryThrowsRuntimeExceptionWithNullMessage_exceptionPropagated() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenThrow(new RuntimeException((String) null));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(null);
    }

}
