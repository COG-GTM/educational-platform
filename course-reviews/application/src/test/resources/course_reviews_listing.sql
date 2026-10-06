DELETE FROM course_review;
DELETE FROM reviewer;
DELETE FROM reviewable_course;

INSERT INTO reviewer (username) VALUES ('reviewer');
INSERT INTO reviewable_course (original_course_id) VALUES ('123E4567E89B12D3A456426655440000');
INSERT INTO reviewable_course (original_course_id) VALUES ('123E4567E89B12D3A456426655440009');

-- reviews of the requested course: ids ascending, created dates deliberately out of insertion order
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (101, '123E4567E89B12D3A456426655440001', 2, 'oldest', TIMESTAMP '2026-01-01 10:00:00',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (102, '123E4567E89B12D3A456426655440002', 5, 'newest', TIMESTAMP '2026-03-01 10:00:00',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (103, '123E4567E89B12D3A456426655440003', 4, 'same instant, lower id', TIMESTAMP '2026-02-01 10:00:00',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (104, '123E4567E89B12D3A456426655440004', 3, 'same instant, higher id', TIMESTAMP '2026-02-01 10:00:00',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));

-- review of another course, must never leak into the requested course's listing
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (105, '123E4567E89B12D3A456426655440005', 1, 'other course', TIMESTAMP '2026-04-01 10:00:00',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440009'));
