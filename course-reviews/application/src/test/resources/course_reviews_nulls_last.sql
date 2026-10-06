DELETE FROM course_review;
DELETE FROM reviewer;
DELETE FROM reviewable_course;

INSERT INTO reviewer (username) VALUES ('reviewer');
INSERT INTO reviewable_course (original_course_id) VALUES ('123E4567E89B12D3A456426655440000');

-- legacy reviews (created before created_date existed) interleaved with dated ones; ids ascending
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (201, '123E4567E89B12D3A456426655440001', 3, 'legacy, lower id', NULL,
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (202, '123E4567E89B12D3A456426655440002', 4, 'older dated', TIMESTAMP '2026-01-01 10:00:00',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (203, '123E4567E89B12D3A456426655440003', 5, 'legacy, higher id', NULL,
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));
INSERT INTO course_review (id, uuid, rating, comment, created_date, reviewer, course) VALUES (204, '123E4567E89B12D3A456426655440004', 2, 'newer dated', TIMESTAMP '2026-02-01 10:00:00',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000'));
