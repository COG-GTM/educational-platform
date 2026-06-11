package com.educational.platform.courses;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseWebRequestsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createCourseRequest_validValues_noViolations() {
        final CreateCourseRequest request = new CreateCourseRequest("Math 101", "Intro to math");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createCourseRequest_blankName_hasViolation() {
        final CreateCourseRequest request = new CreateCourseRequest("", "Description");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void createCourseRequest_nullName_hasViolation() {
        final CreateCourseRequest request = new CreateCourseRequest(null, "Description");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void createCourseRequest_blankDescription_hasViolation() {
        final CreateCourseRequest request = new CreateCourseRequest("Name", "");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void createCourseRequest_nullDescription_hasViolation() {
        final CreateCourseRequest request = new CreateCourseRequest("Name", null);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void createCourseRequest_accessors() {
        final CreateCourseRequest request = new CreateCourseRequest("Physics", "Study of matter");
        assertThat(request.name()).isEqualTo("Physics");
        assertThat(request.description()).isEqualTo("Study of matter");
    }

    @Test
    void createCourseRequest_equalInstances() {
        assertThat(new CreateCourseRequest("a", "b"))
                .isEqualTo(new CreateCourseRequest("a", "b"));
    }

    @Test
    void createdCourseResponse_exposesUuid() {
        final UUID uuid = UUID.randomUUID();
        final CreatedCourseResponse response = new CreatedCourseResponse(uuid);
        assertThat(response.uuid()).isEqualTo(uuid);
    }

    @Test
    void createdCourseResponse_equalInstances() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CreatedCourseResponse(uuid))
                .isEqualTo(new CreatedCourseResponse(uuid));
    }
}
