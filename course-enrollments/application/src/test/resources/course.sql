DELETE FROM course_enrollment_completed_lecture;
DELETE FROM course_enrollment;
DELETE FROM enroll_lecture;
DELETE FROM enroll_course;
DELETE FROM student;

INSERT INTO student (username) VALUES ('student');
INSERT INTO enroll_course (uuid, name) VALUES ('123e4567-e89b-12d3-a456-426655440001', 'Java Basics');
INSERT INTO enroll_course (uuid, name) VALUES ('123e4567-e89b-12d3-a456-426655440002', 'Spring Boot');
INSERT INTO enroll_lecture (uuid, title, serial_number, course) VALUES ('223e4567-e89b-12d3-a456-426655440001', 'Intro', 1, (SELECT id FROM enroll_course WHERE uuid = '123e4567-e89b-12d3-a456-426655440001'));
INSERT INTO enroll_lecture (uuid, title, serial_number, course) VALUES ('223e4567-e89b-12d3-a456-426655440002', 'Variables', 2, (SELECT id FROM enroll_course WHERE uuid = '123e4567-e89b-12d3-a456-426655440001'));
INSERT INTO enroll_lecture (uuid, title, serial_number, course) VALUES ('223e4567-e89b-12d3-a456-426655440003', 'Spring Intro', 1, (SELECT id FROM enroll_course WHERE uuid = '123e4567-e89b-12d3-a456-426655440002'));
