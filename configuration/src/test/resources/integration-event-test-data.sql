-- Test data for CrossModuleIntegrationEventFlowTest.
--
-- Although the application configures Liquibase (spring.liquibase.change-log),
-- those migrations are NOT applied in this @SpringBootTest: no
-- spring.jpa.hibernate.ddl-auto override is set, so for the in-memory H2
-- datasource Hibernate's create-drop default generates the schema (verified:
-- Hibernate logs "create table course (... number integer ...)" and no
-- DATABASECHANGELOG tables are created). Hence the column is NUMBER (the
-- numberOfStudents value object's mapping), not the changelog's
-- number_of_students, and approval_status / publish_status are H2 ENUM columns.
-- This matches the existing fixtures in
-- courses/application/src/test/resources/approved_course.sql.
--
-- A distinct course UUID is used per flow so the asynchronous handlers cannot
-- interfere with one another and each assertion observes only its own effect.
-- No course_proposal row is inserted: the only testable consumer that touches
-- proposals (test 1) CREATES one, so a pre-existing row would mask a broken
-- handler; the approve flow (test 2) approves the Course, not the proposal.

DELETE FROM course_proposal;
DELETE FROM course;
DELETE FROM teacher;

INSERT INTO teacher (username) VALUES ('teacher');

-- Flow 1: SendCourseToApproveIntegrationEvent -> Administration creates a CourseProposal.
INSERT INTO course (uuid, name, description, approval_status, publish_status, number, rating)
VALUES ('123e4567-e89b-12d3-a456-426655440001', 'Send To Approve Course', 'course for the send-to-approve flow', 'WAITING_FOR_APPROVAL', 'DRAFT', 0, 0);

-- Flow 2: CourseApprovedByAdminIntegrationEvent -> Courses approves the Course.
INSERT INTO course (uuid, name, description, approval_status, publish_status, number, rating)
VALUES ('123e4567-e89b-12d3-a456-426655440002', 'Approve Course', 'course for the approve flow', 'WAITING_FOR_APPROVAL', 'DRAFT', 0, 0);

-- Flow 3: StudentEnrolledToCourseIntegrationEvent -> Courses increases numberOfStudents.
INSERT INTO course (uuid, name, description, approval_status, publish_status, number, rating)
VALUES ('123e4567-e89b-12d3-a456-426655440003', 'Enroll Course', 'course for the enrollment flow', 'APPROVED', 'PUBLISHED', 0, 0);

-- Flow 4: CourseRatingRecalculatedIntegrationEvent -> Courses updates rating.
INSERT INTO course (uuid, name, description, approval_status, publish_status, number, rating)
VALUES ('123e4567-e89b-12d3-a456-426655440004', 'Rating Course', 'course for the rating flow', 'APPROVED', 'PUBLISHED', 0, 0);

-- Flow 5 (UserCreatedIntegrationEvent -> Courses creates a Teacher) needs no course.

UPDATE course SET teacher = (SELECT teacher.id FROM teacher WHERE teacher.username = 'teacher' GROUP BY teacher.id);
