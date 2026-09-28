package com.educational.platform.course.reviews.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.CourseReviewRepository;

@ExtendWith(MockitoExtension.class)
public class ListCourseReviewsByCourseUUIDQueryHandlerTest {

    @Mock
    private CourseReviewRepository repository;

    private ListCourseReviewsByCourseUUIDQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ListCourseReviewsByCourseUUIDQueryHandler(repository);
    }

    @Test
    void handle_courseUuid_delegatesRequestedUuidToRepository() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        final CourseReviewDTO review = new CourseReviewDTO(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), courseUuid, "reviewer", "comment", 4.0);
        when(repository.listCourseReviews(courseUuid)).thenReturn(List.of(review));

        // when
        final List<CourseReviewDTO> result = sut.handle(new ListCourseReviewsByCourseUUIDQuery(courseUuid));

        // then
        assertThat(result).containsExactly(review);
        verify(repository).listCourseReviews(courseUuid);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void handle_courseWithoutReviews_emptyList() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        when(repository.listCourseReviews(courseUuid)).thenReturn(List.of());

        // when
        final List<CourseReviewDTO> result = sut.handle(new ListCourseReviewsByCourseUUIDQuery(courseUuid));

        // then
        assertThat(result).isEmpty();
    }
}
