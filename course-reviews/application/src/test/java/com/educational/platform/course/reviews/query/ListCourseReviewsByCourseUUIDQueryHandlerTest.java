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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseReviewsByCourseUUIDQueryHandlerTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    private ListCourseReviewsByCourseUUIDQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ListCourseReviewsByCourseUUIDQueryHandler(courseReviewRepository);
    }

    @Test
    void handle_validQuery_courseReviewsReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseReviewDTO dto = new CourseReviewDTO(reviewUuid, courseId, "username", "comment", 4.0);
        when(courseReviewRepository.listCourseReviews(courseId)).thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst())
                .hasFieldOrPropertyWithValue("uuid", reviewUuid)
                .hasFieldOrPropertyWithValue("course", courseId)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("comment", "comment")
                .hasFieldOrPropertyWithValue("rating", 4.0);
    }

    @Test
    void handle_noReviews_emptyListReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        when(courseReviewRepository.listCourseReviews(courseId)).thenReturn(List.of());

        // when
        final List<CourseReviewDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_multipleReviews_allReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        final CourseReviewDTO dto1 = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"), courseId, "user1", "good", 4.0);
        final CourseReviewDTO dto2 = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440003"), courseId, "user2", "great", 5.0);
        when(courseReviewRepository.listCourseReviews(courseId)).thenReturn(List.of(dto1, dto2));

        // when
        final List<CourseReviewDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CourseReviewDTO::username).containsExactly("user1", "user2");
    }

    @Test
    void handle_validQuery_repositoryCalledWithCorrectUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        when(courseReviewRepository.listCourseReviews(courseId)).thenReturn(List.of());

        // when
        sut.handle(query);

        // then
        verify(courseReviewRepository).listCourseReviews(courseId);
    }

    @Test
    void handle_nullUuidQuery_delegatesToRepository() {
        // given
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(null);
        when(courseReviewRepository.listCourseReviews(null)).thenReturn(List.of());

        // when
        final List<CourseReviewDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
        verify(courseReviewRepository).listCourseReviews(null);
    }
}
