package com.educational.platform.courses;

import com.educational.platform.courses.course.CourseCannotBePublishedException;
import com.educational.platform.courses.course.catalog.CatalogFacetsDTO;
import com.educational.platform.courses.course.catalog.CatalogFacetsQueryHandler;
import com.educational.platform.courses.course.catalog.CourseCatalogPageDTO;
import com.educational.platform.courses.course.catalog.CourseCatalogQuery;
import com.educational.platform.courses.course.catalog.CourseCatalogQueryHandler;
import com.educational.platform.courses.course.catalog.CourseCatalogSort;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCourseCommandHandler;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommandHandler;
import com.educational.platform.web.handler.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Represents Course API adapter.
 */
@Validated
@RequestMapping(value = "/courses")
@RestController
public class CourseController {

    private final CreateCourseCommandHandler createCourseCommandHandler;
    private final PublishCourseCommandHandler publishCourseCommandHandler;
    private final CourseCatalogQueryHandler courseCatalogQueryHandler;
    private final CatalogFacetsQueryHandler catalogFacetsQueryHandler;

    public CourseController(CreateCourseCommandHandler createCourseCommandHandler, PublishCourseCommandHandler publishCourseCommandHandler,
                            CourseCatalogQueryHandler courseCatalogQueryHandler, CatalogFacetsQueryHandler catalogFacetsQueryHandler) {
        this.createCourseCommandHandler = createCourseCommandHandler;
        this.publishCourseCommandHandler = publishCourseCommandHandler;
        this.courseCatalogQueryHandler = courseCatalogQueryHandler;
        this.catalogFacetsQueryHandler = catalogFacetsQueryHandler;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    CourseCatalogPageDTO catalog(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "category", required = false) String category,
                                 @RequestParam(value = "teacher", required = false) String teacher,
                                 @RequestParam(value = "minRating", required = false) Double minRating,
                                 @RequestParam(value = "sort", defaultValue = "NEWEST") CourseCatalogSort sort,
                                 @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
                                 @RequestParam(value = "size", defaultValue = "12") @Min(1) @Max(100) int size) {
        return courseCatalogQueryHandler.handle(new CourseCatalogQuery(search, category, teacher, minRating, sort, page, size));
    }

    @GetMapping(value = "/catalog-facets", produces = APPLICATION_JSON_VALUE)
    CatalogFacetsDTO catalogFacets() {
        return catalogFacetsQueryHandler.handle();
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CreatedCourseResponse create(@Valid @RequestBody CreateCourseRequest courseCreateRequest) {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(courseCreateRequest.name())
                .description(courseCreateRequest.description())
                .category(courseCreateRequest.category())
                .build();

        return new CreatedCourseResponse(createCourseCommandHandler.handle(command));
    }

    @PutMapping(value = "/{uuid}/publish-status", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void publish(@PathVariable("uuid") UUID uuid) {
        publishCourseCommandHandler.handle(new PublishCourseCommand(uuid));
    }

    @ExceptionHandler(CourseCannotBePublishedException.class)
    public ResponseEntity<ErrorResponse> onConflictException(Exception e) {
        final ErrorResponse response = new ErrorResponse(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
