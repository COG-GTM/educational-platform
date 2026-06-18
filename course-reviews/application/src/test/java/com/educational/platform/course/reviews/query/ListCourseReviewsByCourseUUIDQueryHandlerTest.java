package com.educational.platform.course.reviews.query;

import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.CourseReviewRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseReviewsByCourseUUIDQueryHandlerTest {

    @Mock
    private CourseReviewRepository repository;

    @InjectMocks
    private ListCourseReviewsByCourseUUIDQueryHandler sut;

    @Test
    void handle_reviewsExist_forwardsCourseUuidAndReturnsListing() {
        // given - the read path must scope the listing to the queried course uuid
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReviewDTO dto = new CourseReviewDTO(
                UUID.randomUUID(), courseUuid, "reviewer", "great course", 4.5);
        when(repository.listCourseReviews(courseUuid)).thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.handle(new ListCourseReviewsByCourseUUIDQuery(courseUuid));

        // then
        assertThat(result).containsExactly(dto);
        verify(repository).listCourseReviews(courseUuid);
    }

    @Test
    void handle_noReviews_returnsEmptyList() {
        // given - a course with no reviews yields an empty list rather than null
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(repository.listCourseReviews(courseUuid)).thenReturn(Collections.emptyList());

        // when
        final List<CourseReviewDTO> result = sut.handle(new ListCourseReviewsByCourseUUIDQuery(courseUuid));

        // then
        assertThat(result).isEmpty();
    }
}
