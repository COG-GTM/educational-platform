DELETE FROM course_enrollment_completed_lecture;
DELETE FROM course_enrollment;
DELETE FROM enroll_lecture;
DELETE FROM enroll_course;
DELETE FROM student;

INSERT INTO enroll_course (uuid, name) VALUES ('123E4567E89B12D3A456426655440001', 'Java Basics');
INSERT INTO enroll_lecture (uuid, title, serial_number, course) VALUES ('223E4567E89B12D3A456426655440001', 'Intro', 1, (SELECT id FROM enroll_course WHERE uuid = '123E4567E89B12D3A456426655440001'));
INSERT INTO enroll_lecture (uuid, title, serial_number, course) VALUES ('223E4567E89B12D3A456426655440002', 'Variables', 2, (SELECT id FROM enroll_course WHERE uuid = '123E4567E89B12D3A456426655440001'));
INSERT INTO student (username) VALUES ('username');
