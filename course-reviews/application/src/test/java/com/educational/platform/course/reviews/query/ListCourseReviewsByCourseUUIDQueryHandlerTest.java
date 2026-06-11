package com.educational.platform.course.reviews.query;

import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    void handle_existingReviews_returnsReviewsFromRepository() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final CourseReviewDTO dto = new CourseReviewDTO(UUID.randomUUID(), courseUuid, "username", "comment", 4.0);
        when(repository.listCourseReviews(courseUuid)).thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.handle(new ListCourseReviewsByCourseUUIDQuery(courseUuid));

        // then
        assertThat(result).containsExactly(dto);
    }

    @Test
    void handle_noReviews_returnsEmptyList() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        when(repository.listCourseReviews(courseUuid)).thenReturn(List.of());

        // when
        final List<CourseReviewDTO> result = sut.handle(new ListCourseReviewsByCourseUUIDQuery(courseUuid));

        // then
        assertThat(result).isEmpty();
    }
}
