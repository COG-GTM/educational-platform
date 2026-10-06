package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.archive.ArchiveCourseEnrollmentCommand;
import com.educational.platform.course.enrollments.archive.ArchiveCourseEnrollmentCommandHandler;
import com.educational.platform.course.enrollments.archive.RestoreCourseEnrollmentCommand;
import com.educational.platform.course.enrollments.archive.RestoreCourseEnrollmentCommandHandler;
import com.educational.platform.course.enrollments.progress.UpdateLectureProgressCommand;
import com.educational.platform.course.enrollments.progress.UpdateLectureProgressCommandHandler;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByCourseQuery;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByCourseQueryHandler;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQuery;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQueryHandler;
import com.educational.platform.course.enrollments.query.EnrollmentStatusFilter;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Represents Course Enrollment Controller.
 */
@RestController
public class CourseEnrollmentController {

    private final RegisterStudentToCourseCommandHandler registerStudentToCourseCommandHandler;
    private final ListCourseEnrollmentsQueryHandler listCourseEnrollmentsQueryHandler;
    private final CourseEnrollmentByUUIDQueryHandler courseEnrollmentByUUIDQueryHandler;
    private final CourseEnrollmentByCourseQueryHandler courseEnrollmentByCourseQueryHandler;
    private final UpdateLectureProgressCommandHandler updateLectureProgressCommandHandler;
    private final ArchiveCourseEnrollmentCommandHandler archiveCourseEnrollmentCommandHandler;
    private final RestoreCourseEnrollmentCommandHandler restoreCourseEnrollmentCommandHandler;

    public CourseEnrollmentController(RegisterStudentToCourseCommandHandler registerStudentToCourseCommandHandler,
                                      ListCourseEnrollmentsQueryHandler listCourseEnrollmentsQueryHandler,
                                      CourseEnrollmentByUUIDQueryHandler courseEnrollmentByUUIDQueryHandler,
                                      CourseEnrollmentByCourseQueryHandler courseEnrollmentByCourseQueryHandler,
                                      UpdateLectureProgressCommandHandler updateLectureProgressCommandHandler,
                                      ArchiveCourseEnrollmentCommandHandler archiveCourseEnrollmentCommandHandler,
                                      RestoreCourseEnrollmentCommandHandler restoreCourseEnrollmentCommandHandler) {
        this.registerStudentToCourseCommandHandler = registerStudentToCourseCommandHandler;
        this.listCourseEnrollmentsQueryHandler = listCourseEnrollmentsQueryHandler;
        this.courseEnrollmentByUUIDQueryHandler = courseEnrollmentByUUIDQueryHandler;
        this.courseEnrollmentByCourseQueryHandler = courseEnrollmentByCourseQueryHandler;
        this.updateLectureProgressCommandHandler = updateLectureProgressCommandHandler;
        this.archiveCourseEnrollmentCommandHandler = archiveCourseEnrollmentCommandHandler;
        this.restoreCourseEnrollmentCommandHandler = restoreCourseEnrollmentCommandHandler;
    }

    @PostMapping(value = "/courses/{uuid}/course-enrollments", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UUID enroll(@PathVariable("uuid") UUID uuid, @RequestBody CourseEnrollmentRequest request) {
        return registerStudentToCourseCommandHandler.handle(new RegisterStudentToCourseCommand(uuid));
    }

    @GetMapping(value = "/courses/{uuid}/course-enrollments/current", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CourseEnrollmentDTO currentEnrollment(@PathVariable("uuid") UUID uuid) {
        return courseEnrollmentByCourseQueryHandler.handle(new CourseEnrollmentByCourseQuery(uuid))
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Enrollment to course with uuid: %s not found", uuid)));
    }

    @GetMapping(value = "/course-enrollments", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CourseEnrollmentPageDTO courseEnrollments(@RequestParam(value = "status", required = false) EnrollmentStatusFilter status,
                                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "size", defaultValue = "" + ListCourseEnrollmentsQuery.DEFAULT_SIZE) int size) {
        return listCourseEnrollmentsQueryHandler.handle(new ListCourseEnrollmentsQuery(status, page, size));
    }

    @GetMapping(value = "/course-enrollments/{uuid}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CourseEnrollmentDetailsDTO courseEnrollment(@PathVariable("uuid") UUID uuid) {
        return courseEnrollmentByUUIDQueryHandler.handle(new CourseEnrollmentByUUIDQuery(uuid))
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Course enrollment with uuid: %s not found", uuid)));
    }

    @PutMapping(value = "/course-enrollments/{uuid}/lectures/{lectureUuid}/progress", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CourseEnrollmentDetailsDTO updateLectureProgress(@PathVariable("uuid") UUID uuid, @PathVariable("lectureUuid") UUID lectureUuid,
                                                            @RequestBody LectureProgressRequest request) {
        return updateLectureProgressCommandHandler.handle(new UpdateLectureProgressCommand(uuid, lectureUuid, request.completed()));
    }

    @PutMapping(value = "/course-enrollments/{uuid}/archive-status", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CourseEnrollmentDTO updateArchiveStatus(@PathVariable("uuid") UUID uuid, @RequestBody ArchiveStatusRequest request) {
        if (request.archived()) {
            return archiveCourseEnrollmentCommandHandler.handle(new ArchiveCourseEnrollmentCommand(uuid));
        }

        return restoreCourseEnrollmentCommandHandler.handle(new RestoreCourseEnrollmentCommand(uuid));
    }
}
