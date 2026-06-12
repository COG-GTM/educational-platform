package com.educational.platform.courses.teacher.create;

import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateTeacherCommandHandlerEdgeCaseTest {

    @Mock
    private TeacherRepository repository;

    @InjectMocks
    private CreateTeacherCommandHandler sut;

    @Test
    void handle_twoDifferentTeachers_bothSaved() {
        // given
        final CreateTeacherCommand command1 = new CreateTeacherCommand("teacher1");
        final CreateTeacherCommand command2 = new CreateTeacherCommand("teacher2");

        // when
        sut.handle(command1);
        sut.handle(command2);

        // then
        final ArgumentCaptor<Teacher> argument = ArgumentCaptor.forClass(Teacher.class);
        verify(repository, times(2)).save(argument.capture());
        assertThat(argument.getAllValues()).hasSize(2);
        assertThat(argument.getAllValues().get(0)).hasFieldOrPropertyWithValue("username", "teacher1");
        assertThat(argument.getAllValues().get(1)).hasFieldOrPropertyWithValue("username", "teacher2");
    }
}
