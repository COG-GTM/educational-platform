package com.educational.platform.course.reviews.course;

import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewableCourseTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void create_validCommand_originalCourseIdStored() {
        // when
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(UUID_VALUE));

        // then
        assertThat(course).hasFieldOrPropertyWithValue("originalCourseId", UUID_VALUE);
    }

    @Test
    void getId_freshlyCreated_isNull() {
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(UUID_VALUE));
        assertThat(course.getId()).isNull();
    }

    @Test
    void createReviewableCourseCommand_exposesUuid() {
        assertThat(new CreateReviewableCourseCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }
}
