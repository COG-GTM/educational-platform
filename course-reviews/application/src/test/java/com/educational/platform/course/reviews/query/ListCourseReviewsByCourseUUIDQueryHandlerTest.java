package com.educational.platform.course.reviews.query;

import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private ListCourseReviewsByCourseUUIDQueryHandler sut;

    @Test
    void handle_validQuery_courseReviewsReturned() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseUuid);
        final CourseReviewDTO dto = new CourseReviewDTO(UUID.randomUUID(), courseUuid, "username", "comment", 4.0);
        when(repository.listCourseReviews(courseUuid)).thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(dto);
    }

    @Test
    void handle_noReviews_emptyListReturned() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseUuid);
        when(repository.listCourseReviews(courseUuid)).thenReturn(List.of());

        // when
        final List<CourseReviewDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }
}
