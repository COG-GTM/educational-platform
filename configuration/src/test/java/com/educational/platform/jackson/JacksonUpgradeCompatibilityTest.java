package com.educational.platform.jackson;

import com.educational.platform.course.reviews.Comment;
import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.RoleDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Jackson upgrade compatibility for value objects, command records,
 * polymorphic types, and ObjectMapper feature defaults after Jackson Core
 * upgrade (2.20.1 → 2.21.2 via Spring Boot BOM 4.0.1 → 4.0.6).
 */
class JacksonUpgradeCompatibilityTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    // --- Value Object serialization (records implementing ValueObject + @Embeddable) ---

    @Test
    void courseRating_roundTrip() throws Exception {
        var original = new CourseRating(4.75);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(4.75);
    }

    @Test
    void courseRating_zero_roundTrip() throws Exception {
        var original = new CourseRating(0.0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(0.0);
    }

    @Test
    void courseRating_negative_roundTrip() throws Exception {
        var original = new CourseRating(-1.5);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(-1.5);
    }

    @Test
    void courseRating_maxDouble_roundTrip() throws Exception {
        var original = new CourseRating(Double.MAX_VALUE);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void numberOfStudents_roundTrip() throws Exception {
        var original = new NumberOfStudents(150);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, NumberOfStudents.class);

        assertThat(deserialized.number()).isEqualTo(150);
    }

    @Test
    void numberOfStudents_zero_roundTrip() throws Exception {
        var original = new NumberOfStudents(0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, NumberOfStudents.class);

        assertThat(deserialized.number()).isZero();
    }

    @Test
    void numberOfStudents_negative_roundTrip() throws Exception {
        var original = new NumberOfStudents(-1);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, NumberOfStudents.class);

        assertThat(deserialized.number()).isEqualTo(-1);
    }

    @Test
    void numberOfStudents_maxInt_roundTrip() throws Exception {
        var original = new NumberOfStudents(Integer.MAX_VALUE);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, NumberOfStudents.class);

        assertThat(deserialized.number()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void comment_roundTrip() throws Exception {
        var original = new Comment("Great course!");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, Comment.class);

        assertThat(deserialized.comment()).isEqualTo("Great course!");
    }

    @Test
    void comment_null_roundTrip() throws Exception {
        var original = new Comment(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, Comment.class);

        assertThat(deserialized.comment()).isNull();
    }

    @Test
    void comment_empty_roundTrip() throws Exception {
        var original = new Comment("");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, Comment.class);

        assertThat(deserialized.comment()).isEmpty();
    }

    @Test
    void comment_specialCharacters_roundTrip() throws Exception {
        var original = new Comment("Line1\nLine2\tTabbed \"quoted\" <html>&amp;");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, Comment.class);

        assertThat(deserialized.comment()).isEqualTo("Line1\nLine2\tTabbed \"quoted\" <html>&amp;");
    }

    // --- Command object serialization ---

    @Test
    void createCourseCommand_withNullCurriculumItems_roundTrip() throws Exception {
        var original = CreateCourseCommand.builder()
                .name("Test Course")
                .description("Description")
                .curriculumItems(null)
                .build();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseCommand.class);

        assertThat(deserialized.name()).isEqualTo("Test Course");
        assertThat(deserialized.description()).isEqualTo("Description");
        assertThat(deserialized.curriculumItems()).isNull();
    }

    @Test
    void createCourseCommand_withEmptyCurriculumItems_roundTrip() throws Exception {
        var original = CreateCourseCommand.builder()
                .name("Test Course")
                .description("Description")
                .curriculumItems(Collections.emptyList())
                .build();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseCommand.class);

        assertThat(deserialized.name()).isEqualTo("Test Course");
        assertThat(deserialized.curriculumItems()).isEmpty();
    }

    @Test
    void createQuestionCommand_roundTrip() throws Exception {
        var original = new CreateQuestionCommand("What is polymorphism?");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateQuestionCommand.class);

        assertThat(deserialized.content()).isEqualTo("What is polymorphism?");
    }

    @Test
    void createQuestionCommand_nullContent_roundTrip() throws Exception {
        var original = new CreateQuestionCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateQuestionCommand.class);

        assertThat(deserialized.content()).isNull();
    }

    @Test
    void registerStudentToCourseCommand_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new RegisterStudentToCourseCommand(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, RegisterStudentToCourseCommand.class);

        assertThat(deserialized.courseId()).isEqualTo(uuid);
    }

    @Test
    void registerStudentToCourseCommand_nullUuid_roundTrip() throws Exception {
        var original = new RegisterStudentToCourseCommand(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, RegisterStudentToCourseCommand.class);

        assertThat(deserialized.courseId()).isNull();
    }

    @Test
    void userRegistrationCommand_roundTrip() throws Exception {
        var original = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher1")
                .email("teacher@example.com")
                .password("SecureP@ss1")
                .build();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UserRegistrationCommand.class);

        assertThat(deserialized.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
        assertThat(deserialized.username()).isEqualTo("teacher1");
        assertThat(deserialized.email()).isEqualTo("teacher@example.com");
        assertThat(deserialized.password()).isEqualTo("SecureP@ss1");
    }

    @Test
    void userRegistrationCommand_nullFields_roundTrip() throws Exception {
        var original = UserRegistrationCommand.builder().build();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UserRegistrationCommand.class);

        assertThat(deserialized.role()).isNull();
        assertThat(deserialized.username()).isNull();
        assertThat(deserialized.email()).isNull();
        assertThat(deserialized.password()).isNull();
    }

    // --- Polymorphic type serialization (class hierarchy) ---

    @Test
    void createLectureCommand_serialization() throws Exception {
        var original = CreateLectureCommand.builder()
                .title("Introduction to Java")
                .description("First lecture")
                .serialNumber(1)
                .text("Java is a programming language...")
                .build();

        var json = objectMapper.writeValueAsString(original);
        var node = objectMapper.readTree(json);

        assertThat(node.get("title").asText()).isEqualTo("Introduction to Java");
        assertThat(node.get("description").asText()).isEqualTo("First lecture");
        assertThat(node.get("serialNumber").asInt()).isEqualTo(1);
        assertThat(node.get("text").asText()).isEqualTo("Java is a programming language...");
    }

    @Test
    void createLectureCommand_deserialization_failsWithoutDefaultConstructor() {
        var json = """
                {"title":"Lecture 1","description":"Desc","serialNumber":2,"text":"Content"}
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, CreateLectureCommand.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidDefinitionException.class)
                .hasMessageContaining("no Creators");
    }

    @Test
    void createLectureCommand_nullFields_serialization() throws Exception {
        var original = CreateLectureCommand.builder().build();

        var json = objectMapper.writeValueAsString(original);
        var node = objectMapper.readTree(json);

        assertThat(node.get("title").isNull()).isTrue();
        assertThat(node.get("description").isNull()).isTrue();
        assertThat(node.get("serialNumber").isNull()).isTrue();
        assertThat(node.get("text").isNull()).isTrue();
    }

    // --- CourseDTO with nested list ---

    @Test
    void courseDTO_withEmptyList_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseDTO(uuid, "Java Basics", "Intro to Java", 25, Collections.emptyList());

        var json = objectMapper.writeValueAsString(original);
        var node = objectMapper.readTree(json);

        assertThat(node.get("uuid").asText()).isEqualTo(uuid.toString());
        assertThat(node.get("name").asText()).isEqualTo("Java Basics");
        assertThat(node.get("description").asText()).isEqualTo("Intro to Java");
        assertThat(node.get("numberOfStudents").asInt()).isEqualTo(25);
        assertThat(node.get("curriculumItems").isArray()).isTrue();
        assertThat(node.get("curriculumItems").isEmpty()).isTrue();
    }

    @Test
    void courseDTO_withNullList_serializes() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseDTO(uuid, "Course", "Desc", 0, null);

        var json = objectMapper.writeValueAsString(original);
        var node = objectMapper.readTree(json);

        assertThat(node.get("uuid").asText()).isEqualTo(uuid.toString());
        assertThat(node.get("curriculumItems").isNull()).isTrue();
    }

    // --- ObjectMapper feature defaults (verifying behavior after upgrade) ---

    @Test
    void defaultObjectMapper_failsOnUnknownProperties() {
        var json = "{\"rating\": 4.5, \"unknownField\": \"value\"}";

        assertThatThrownBy(() -> objectMapper.readValue(json, CourseRating.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .hasMessageContaining("unknownField");
    }

    @Test
    void objectMapper_configuredToIgnoreUnknown_allowsExtraFields() throws Exception {
        var lenientMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        var json = "{\"rating\": 4.5, \"unknownField\": \"value\"}";
        var deserialized = lenientMapper.readValue(json, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(4.5);
    }

    @Test
    void objectMapper_failOnNullForPrimitives_enforcedWhenEnabled() {
        var strictMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        var json = "{\"number\": null}";

        assertThatThrownBy(() -> strictMapper.readValue(json, NumberOfStudents.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void objectMapper_defaultDoesNotFailOnNullPrimitives() throws Exception {
        var json = "{\"number\": null}";

        var deserialized = objectMapper.readValue(json, NumberOfStudents.class);

        assertThat(deserialized.number()).isZero();
    }

    @Test
    void objectMapper_serializationFeature_indentOutput() throws Exception {
        var prettyMapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();

        var original = new CourseRating(5.0);
        var json = prettyMapper.writeValueAsString(original);

        assertThat(json).contains("\n");
        assertThat(json).contains("rating");
    }

    @Test
    void objectMapper_moduleAutoDiscovery_works() {
        var mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        var registeredModules = mapper.getRegisteredModuleIds();
        assertThat(registeredModules).isNotEmpty();
    }

    // --- Type coercion behavior ---

    @Test
    void intToDouble_coercion_allowedByDefault() throws Exception {
        var json = "{\"rating\": 5}";

        var deserialized = objectMapper.readValue(json, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(5.0);
    }

    @Test
    void stringToNumber_coercion_allowedByDefault() throws Exception {
        var json = "{\"number\": \"123\"}";

        var deserialized = objectMapper.readValue(json, NumberOfStudents.class);

        assertThat(deserialized.number()).isEqualTo(123);
    }

    @Test
    void stringToNumber_coercion_rejectedWhenDisabled() {
        var strictMapper = JsonMapper.builder()
                .withCoercionConfigDefaults(cfg ->
                        cfg.setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.String,
                                com.fasterxml.jackson.databind.cfg.CoercionAction.Fail))
                .build();

        var json = "{\"number\": \"123\"}";

        assertThatThrownBy(() -> strictMapper.readValue(json, NumberOfStudents.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void jsonMapper_builder_producesEquivalentResults() throws Exception {
        var builderMapper = JsonMapper.builder().build();
        builderMapper.findAndRegisterModules();

        var uuid = UUID.randomUUID();
        var original = new CourseLightDTO(uuid, "Course", "Desc", 10);

        var json = builderMapper.writeValueAsString(original);
        var deserialized = builderMapper.readValue(json, CourseLightDTO.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.name()).isEqualTo("Course");
    }

    // --- JSON tree API behavior ---

    @Test
    void readTree_preservesNumericPrecision() throws Exception {
        var json = "{\"value\": 1234567890.123456789}";

        var node = objectMapper.readTree(json);

        assertThat(node.get("value").isNumber()).isTrue();
        assertThat(node.get("value").decimalValue().toPlainString()).startsWith("1234567890.12345");
    }

    @Test
    void readTree_handlesLargeArrays() throws Exception {
        var sb = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) sb.append(",");
            sb.append(i);
        }
        sb.append("]");

        var node = objectMapper.readTree(sb.toString());

        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(1000);
        assertThat(node.get(0).asInt()).isZero();
        assertThat(node.get(999).asInt()).isEqualTo(999);
    }

    @Test
    void readTree_handlesUnicodeEscapes() throws Exception {
        var json = "{\"value\": \"\\u0048\\u0065\\u006C\\u006C\\u006F\"}";

        var node = objectMapper.readTree(json);

        assertThat(node.get("value").asText()).isEqualTo("Hello");
    }

    @Test
    void valueToTree_andTreeToValue_roundTrip() throws Exception {
        var original = new CourseRating(4.5);

        JsonNode tree = objectMapper.valueToTree(original);
        var deserialized = objectMapper.treeToValue(tree, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(4.5);
    }

    @Test
    void convertValue_betweenCompatibleTypes() throws Exception {
        var map = Map.of("rating", 3.5);

        var deserialized = objectMapper.convertValue(map, CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(3.5);
    }

    // --- Collection of value objects ---

    @Test
    void listOfValueObjects_roundTrip() throws Exception {
        var list = List.of(new CourseRating(1.0), new CourseRating(2.5), new CourseRating(5.0));

        var json = objectMapper.writeValueAsString(list);
        var deserialized = objectMapper.readValue(json, new TypeReference<List<CourseRating>>() {});

        assertThat(deserialized).hasSize(3);
        assertThat(deserialized.get(0).rating()).isEqualTo(1.0);
        assertThat(deserialized.get(1).rating()).isEqualTo(2.5);
        assertThat(deserialized.get(2).rating()).isEqualTo(5.0);
    }

    @Test
    void listOfComments_roundTrip() throws Exception {
        var list = List.of(new Comment("Good"), new Comment(""), new Comment(null));

        var json = objectMapper.writeValueAsString(list);
        var deserialized = objectMapper.readValue(json, new TypeReference<List<Comment>>() {});

        assertThat(deserialized).hasSize(3);
        assertThat(deserialized.get(0).comment()).isEqualTo("Good");
        assertThat(deserialized.get(1).comment()).isEmpty();
        assertThat(deserialized.get(2).comment()).isNull();
    }

    @Test
    void mapWithValueObjects_roundTrip() throws Exception {
        var map = Map.of("java", new NumberOfStudents(100), "python", new NumberOfStudents(200));

        var json = objectMapper.writeValueAsString(map);
        var deserialized = objectMapper.readValue(json, new TypeReference<Map<String, NumberOfStudents>>() {});

        assertThat(deserialized).hasSize(2);
        assertThat(deserialized.get("java").number()).isEqualTo(100);
        assertThat(deserialized.get("python").number()).isEqualTo(200);
    }

    // --- Course Reviews CourseRating value object (separate from courses.CourseRating) ---

    @Test
    void courseReviewsCourseRating_roundTrip() throws Exception {
        var original = new com.educational.platform.course.reviews.CourseRating(4.75);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, com.educational.platform.course.reviews.CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(4.75);
    }

    @Test
    void courseReviewsCourseRating_zero_roundTrip() throws Exception {
        var original = new com.educational.platform.course.reviews.CourseRating(0.0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, com.educational.platform.course.reviews.CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(0.0);
    }

    @Test
    void courseReviewsCourseRating_negative_roundTrip() throws Exception {
        var original = new com.educational.platform.course.reviews.CourseRating(-2.0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, com.educational.platform.course.reviews.CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(-2.0);
    }

    @Test
    void courseReviewsCourseRating_maxDouble_roundTrip() throws Exception {
        var original = new com.educational.platform.course.reviews.CourseRating(Double.MAX_VALUE);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, com.educational.platform.course.reviews.CourseRating.class);

        assertThat(deserialized.rating()).isEqualTo(Double.MAX_VALUE);
    }

    // --- Jackson version verification ---

    @Test
    void jacksonVersion_isAtLeast_2_21() {
        Version version = objectMapper.version();

        assertThat(version.getMajorVersion()).isEqualTo(2);
        assertThat(version.getMinorVersion()).isGreaterThanOrEqualTo(21);
    }
}
