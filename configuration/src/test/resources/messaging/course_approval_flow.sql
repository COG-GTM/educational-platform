DELETE FROM course_proposal;
DELETE FROM course;
DELETE FROM teacher;

INSERT INTO teacher (username) VALUES ('messaging-teacher');
INSERT INTO course (uuid, name, description, approval_status, number, rating) VALUES ('223E4567E89B12D3A456426655440101', 'course name', 'description', 'NOT_SENT_FOR_APPROVAL', 0, 0);
UPDATE course SET teacher = (SELECT teacher.id FROM teacher WHERE teacher.username = 'messaging-teacher' GROUP BY teacher.id);
