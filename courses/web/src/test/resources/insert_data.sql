delete from course;
delete from teacher;

INSERT INTO teacher (username) VALUES ('username');
INSERT INTO course (uuid, name, description, approval_status, number, rating) VALUES ('123E4567E89B12D3A456426655440001', 'course name', 'description', 'APPROVED', 0, 0);
INSERT INTO course (uuid, name, description, category, created_date, publish_status, approval_status, number, rating) VALUES ('123E4567E89B12D3A456426655440002', 'published course', 'published description', 'Programming', CURRENT_TIMESTAMP, 'PUBLISHED', 'APPROVED', 3, 4.5);
UPDATE course SET teacher = (SELECT teacher.id FROM teacher WHERE teacher.username = 'username' GROUP BY teacher.id);
