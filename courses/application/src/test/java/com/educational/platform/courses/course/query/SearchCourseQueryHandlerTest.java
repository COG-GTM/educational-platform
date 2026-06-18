package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
public class SearchCourseQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private SearchCourseQueryHandler sut;

    @Test
    void handle_matchingKeyword_returnsCoursesFromRepository() {
        // given
        final CourseLightDTO java = new CourseLightDTO(UUID.randomUUID(), "Java Fundamentals", "Learn Java", 5);
        final CourseLightDTO advanced = new CourseLightDTO(UUID.randomUUID(), "Advanced Java", "JVM internals", 3);
        when(repository.searchByKeyword("java")).thenReturn(List.of(java, advanced));

        // when
        final List<CourseLightDTO> result = sut.handle(new SearchCourseQuery("java"));

        // then
        assertThat(result).containsExactly(java, advanced);
    }

    @Test
    void handle_noMatches_returnsEmptyList() {
        // given
        when(repository.searchByKeyword("missing")).thenReturn(List.of());

        // when
        final List<CourseLightDTO> result = sut.handle(new SearchCourseQuery("missing"));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_anyQuery_delegatesKeywordToRepository() {
        // given
        when(repository.searchByKeyword("spring")).thenReturn(List.of());

        // when
        sut.handle(new SearchCourseQuery("spring"));

        // then
        final ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        verify(repository).searchByKeyword(keyword.capture());
        assertThat(keyword.getValue()).isEqualTo("spring");
    }

    @Test
    void handle_nullKeyword_delegatesNullToRepositoryAndReturnsResult() {
        // given
        when(repository.searchByKeyword(null)).thenReturn(List.of());

        // when
        final List<CourseLightDTO> result = sut.handle(new SearchCourseQuery(null));

        // then
        assertThat(result).isNotNull().isEmpty();
        verify(repository).searchByKeyword(null);
    }

    @Test
    void handle_emptyKeyword_delegatesEmptyStringToRepository() {
        // given
        when(repository.searchByKeyword("")).thenReturn(List.of());

        // when
        final List<CourseLightDTO> result = sut.handle(new SearchCourseQuery(""));

        // then
        assertThat(result).isNotNull().isEmpty();
        verify(repository).searchByKeyword("");
    }

    @Test
    void handle_keywordWithSurroundingWhitespace_passedVerbatimToRepository() {
        // given
        when(repository.searchByKeyword("  java  ")).thenReturn(List.of());

        // when
        sut.handle(new SearchCourseQuery("  java  "));

        // then
        // the handler must not trim or otherwise normalize the keyword before delegating.
        verify(repository).searchByKeyword("  java  ");
    }

    @Test
    void handle_query_onlyInteractsWithRepositorySearch() {
        // given
        when(repository.searchByKeyword("java")).thenReturn(List.of());

        // when
        sut.handle(new SearchCourseQuery("java"));

        // then
        verify(repository).searchByKeyword("java");
        verifyNoMoreInteractions(repository);
    }
}
