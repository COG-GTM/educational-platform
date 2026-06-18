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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateTeacherCommandHandlerTest {

    @Mock
    private TeacherRepository repository;

    @InjectMocks
    private CreateTeacherCommandHandler sut;


    @Test
    void handle_validTeacher_saveExecuted() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("username");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Teacher> argument = ArgumentCaptor.forClass(Teacher.class);
        verify(repository).save(argument.capture());
        final Teacher saved = argument.getValue();
        assertThat(saved)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrProperty("id").isNotNull();
    }

    @Test
    void handle_emptyUsername_teacherSavedWithEmptyUsername() {
        // given - the handler performs no validation; an empty username is persisted verbatim
        final CreateTeacherCommand command = new CreateTeacherCommand("");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Teacher> argument = ArgumentCaptor.forClass(Teacher.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "");
    }

    @Test
    void handle_nullUsername_teacherSavedWithNullUsername() {
        // given - a null username is forwarded verbatim to the persisted teacher
        final CreateTeacherCommand command = new CreateTeacherCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Teacher> argument = ArgumentCaptor.forClass(Teacher.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", null);
    }

    @Test
    void handle_repositoryThrows_exceptionPropagated() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("username");
        doThrow(new RuntimeException("teacher could not be saved"))
                .when(repository).save(any(Teacher.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("teacher could not be saved");
    }
}
