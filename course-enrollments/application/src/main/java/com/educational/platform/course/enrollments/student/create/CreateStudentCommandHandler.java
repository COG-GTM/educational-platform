package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for {@link CreateStudentCommand} creates a student.
 */
@Component
@Transactional
public class CreateStudentCommandHandler {

    private final StudentRepository studentRepository;

    public CreateStudentCommandHandler(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Creates student from command. If a student with the same username already exists,
     * no new student is created; instead its uuid is reconciled with the one from the
     * command so the cross-module identity always matches the user's uuid.
     *
     * @param command command
     */
    public void handle(CreateStudentCommand command) {
        final Student existing = studentRepository.findByUsername(command.username());
        if (existing != null) {
            existing.assignUuid(command.uuid());
            studentRepository.save(existing);
            return;
        }

        final Student student = new Student(command);
        studentRepository.save(student);
    }
}
