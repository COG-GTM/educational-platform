package com.educational.platform.courses;

import com.educational.platform.courses.course.CourseCannotBePublishedException;
import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCourseCommandHandler;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommandHandler;
import com.educational.platform.courses.course.query.ListCourseQuery;
import com.educational.platform.courses.course.query.ListCourseQueryHandler;
import com.educational.platform.web.handler.ErrorResponse;

import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    private final ListCourseQueryHandler listCourseQueryHandler;

    public CourseController(CreateCourseCommandHandler createCourseCommandHandler, PublishCourseCommandHandler publishCourseCommandHandler, ListCourseQueryHandler listCourseQueryHandler) {
        this.createCourseCommandHandler = createCourseCommandHandler;
        this.publishCourseCommandHandler = publishCourseCommandHandler;
        this.listCourseQueryHandler = listCourseQueryHandler;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    PagedModel<CourseLightDTO> list(@RequestParam(value = "page", defaultValue = "0") int page,
                                    @RequestParam(value = "size", defaultValue = "20") int size) {
        return new PagedModel<>(listCourseQueryHandler.handle(new ListCourseQuery(page, size)));
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CreatedCourseResponse create(@Valid @RequestBody CreateCourseRequest courseCreateRequest) {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(courseCreateRequest.name())
                .description(courseCreateRequest.description())
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
