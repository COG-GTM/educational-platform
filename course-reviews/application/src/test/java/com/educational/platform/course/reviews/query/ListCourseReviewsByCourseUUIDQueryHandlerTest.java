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

    @Test
    void handle_repositoryThrows_exceptionPropagates() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        when(courseReviewRepository.listCourseReviews(courseId))
                .thenThrow(new RuntimeException("db error"));

        // when/then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    void handle_returnsExactListFromRepository() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        final List<CourseReviewDTO> expectedList = List.of(
                new CourseReviewDTO(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"), courseId, "user", "good", 4.0));
        when(courseReviewRepository.listCourseReviews(courseId)).thenReturn(expectedList);

        // when
        final List<CourseReviewDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(expectedList);
    }

    @Test
    void handle_repositoryThrowsRuntimeException_exceptionPropagates() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        when(courseReviewRepository.listCourseReviews(courseId)).thenThrow(new RuntimeException("db error"));

        // when/then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    void handle_twoDifferentUuids_eachDelegatesToRepositoryWithCorrectUuid() {
        // given
        final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final ListCourseReviewsByCourseUUIDQuery query1 = new ListCourseReviewsByCourseUUIDQuery(courseId1);
        final ListCourseReviewsByCourseUUIDQuery query2 = new ListCourseReviewsByCourseUUIDQuery(courseId2);
        final CourseReviewDTO dto1 = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440010"), courseId1, "user1", "good", 4.0);
        final CourseReviewDTO dto2 = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440020"), courseId2, "user2", "great", 5.0);
        when(courseReviewRepository.listCourseReviews(courseId1)).thenReturn(List.of(dto1));
        when(courseReviewRepository.listCourseReviews(courseId2)).thenReturn(List.of(dto2));

        // when
        final List<CourseReviewDTO> result1 = sut.handle(query1);
        final List<CourseReviewDTO> result2 = sut.handle(query2);

        // then
        assertThat(result1).hasSize(1);
        assertThat(result1.getFirst().course()).isEqualTo(courseId1);
        assertThat(result2).hasSize(1);
        assertThat(result2.getFirst().course()).isEqualTo(courseId2);
        verify(courseReviewRepository).listCourseReviews(courseId1);
        verify(courseReviewRepository).listCourseReviews(courseId2);
    }

    @Test
    void handle_repositoryCalledExactlyOnce() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);
        when(courseReviewRepository.listCourseReviews(courseId)).thenReturn(List.of());

        // when
        sut.handle(query);

        // then
        org.mockito.Mockito.verify(courseReviewRepository, org.mockito.Mockito.times(1)).listCourseReviews(courseId);
    }
}
