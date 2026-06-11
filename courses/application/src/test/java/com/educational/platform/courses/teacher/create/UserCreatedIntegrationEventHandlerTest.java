package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand createTeacherCommand = argument.getValue();
        assertThat(createTeacherCommand)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handleUserCreatedEvent_handlerCalledExactlyOnce() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler).handle(org.mockito.ArgumentMatchers.any(CreateTeacherCommand.class));
        verifyNoMoreInteractions(createTeacherCommandHandler);
    }

    @Test
    void handleUserCreatedEvent_onlyUsernamePassedToCommand_emailNotIncluded() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("alice", "alice@school.edu");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand command = argument.getValue();
        assertThat(command.username()).isEqualTo("alice");
    }

    @Test
    void handleUserCreatedEvent_specialCharactersInUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user.name+tag@org", "email@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("user.name+tag@org");
    }

    @Test
    void handleUserCreatedEvent_emptyUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("", "empty@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEmpty();
    }

    @Test
    void handleUserCreatedEvent_nullUsername_nullPreservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, "email@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isNull();
    }

    @Test
    void handleUserCreatedEvent_whitespaceOnlyUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("   ", "ws@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("   ");
    }

    @Test
    void handleUserCreatedEvent_unicodeUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("用户名αβγ", "unicode@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("用户名αβγ");
    }

    @Test
    void handleUserCreatedEvent_multipleEventsProcessed_handlerCalledForEach() {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user1", "user1@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user2", "user2@example.com");

        // when
        sut.handleUserCreatedEvent(event1);
        sut.handleUserCreatedEvent(event2);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(2)).handle(argument.capture());
        assertThat(argument.getAllValues())
                .extracting(CreateTeacherCommand::username)
                .containsExactly("user1", "user2");
    }

    @Test
    void handleUserCreatedEvent_longUsername_preservedInCommand() {
        // given
        final String longUsername = "a".repeat(500);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(longUsername, "long@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo(longUsername);
        assertThat(argument.getValue().username()).hasSize(500);
    }

    @Test
    void handleUserCreatedEvent_nullEvent_throwsNullPointerException() {
        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handleUserCreatedEvent_eachCallConstructsNewCommand() {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user1", "u1@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user1", "u2@example.com");

        // when
        sut.handleUserCreatedEvent(event1);
        sut.handleUserCreatedEvent(event2);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(2)).handle(argument.capture());
        final CreateTeacherCommand cmd1 = argument.getAllValues().get(0);
        final CreateTeacherCommand cmd2 = argument.getAllValues().get(1);
        assertThat(cmd1).isNotSameAs(cmd2);
        assertThat(cmd1.username()).isEqualTo("user1");
        assertThat(cmd2.username()).isEqualTo("user1");
    }

    @Test
    void handleUserCreatedEvent_differentEmails_sameUsernamePassedToCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "different@email.org");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("teacher");
    }

    @Test
    void handleUserCreatedEvent_usernameWithNewlineChars_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("line1\nline2\ttab", "nl@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("line1\nline2\ttab");
    }

    @Test
    void handleUserCreatedEvent_nullEmail_usernameStillPassedToCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", null);

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("teacher");
    }

    @Test
    void handleUserCreatedEvent_commandContainsOnlyUsername_noEmailField() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("bob", "bob@school.edu");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand command = argument.getValue();
        assertThat(command.username()).isEqualTo("bob");
        assertThat(command).hasNoNullFieldsOrProperties();
    }

    @Test
    void handleUserCreatedEvent_commandHandlerThrowsException_exceptionPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");
        doThrow(new RuntimeException("db error")).when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    void handleUserCreatedEvent_bothNullUsernameAndEmail_usernameStillPassedAsNull() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, null);

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isNull();
    }

    @Test
    void handleUserCreatedEvent_singleCharUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("x", "x@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("x");
    }

    @Test
    void handleUserCreatedEvent_singleEvent_noMoreInteractions() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));
        verifyNoMoreInteractions(createTeacherCommandHandler);
    }

    @Test
    void handleUserCreatedEvent_usernameWithUrlChars_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(
                "user?param=value&other=123#fragment", "url@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("user?param=value&other=123#fragment");
    }

    @Test
    void handleUserCreatedEvent_commandHandlerThrowsIllegalStateException_exceptionPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");
        doThrow(new IllegalStateException("duplicate teacher")).when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("duplicate teacher");
    }

    @Test
    void handleUserCreatedEvent_emojiUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("\uD83C\uDF93teacher\uD83D\uDCDA", "emoji@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("\uD83C\uDF93teacher\uD83D\uDCDA");
    }

    @Test
    void handleUserCreatedEvent_usernameWithLeadingTrailingSpaces_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("  alice  ", "alice@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("  alice  ");
    }

    @Test
    void handleUserCreatedEvent_threeSequentialEvents_allProcessedInOrder() {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user1", "u1@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user2", "u2@example.com");
        final UserCreatedIntegrationEvent event3 = new UserCreatedIntegrationEvent("user3", "u3@example.com");

        // when
        sut.handleUserCreatedEvent(event1);
        sut.handleUserCreatedEvent(event2);
        sut.handleUserCreatedEvent(event3);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(3)).handle(argument.capture());
        assertThat(argument.getAllValues())
                .extracting(CreateTeacherCommand::username)
                .containsExactly("user1", "user2", "user3");
    }

    @Test
    void handleUserCreatedEvent_usernameWithSqlInjectionChars_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(
                "'; DROP TABLE teachers; --", "sql@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("'; DROP TABLE teachers; --");
    }

    @Test
    void handleUserCreatedEvent_usernameWithBackslashes_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(
                "domain\\user\\name", "bs@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("domain\\user\\name");
    }

    @Test
    void handleUserCreatedEvent_multipleEventsAlternatingNullAndNonNull_allProcessed() {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent(null, "e1@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("teacher", "e2@example.com");
        final UserCreatedIntegrationEvent event3 = new UserCreatedIntegrationEvent(null, "e3@example.com");

        // when
        sut.handleUserCreatedEvent(event1);
        sut.handleUserCreatedEvent(event2);
        sut.handleUserCreatedEvent(event3);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(3)).handle(argument.capture());
        assertThat(argument.getAllValues())
                .extracting(CreateTeacherCommand::username)
                .containsExactly(null, "teacher", null);
    }

    @Test
    void handleUserCreatedEvent_commandHandlerThrowsNullPointerException_exceptionPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");
        doThrow(new NullPointerException("null command")).when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("null command");
    }

    @Test
    void handleUserCreatedEvent_sameEventObjectProcessedTwice_handlerCalledTwice() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");

        // when
        sut.handleUserCreatedEvent(event);
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(2)).handle(argument.capture());
        assertThat(argument.getAllValues())
                .extracting(CreateTeacherCommand::username)
                .containsExactly("teacher", "teacher");
        assertThat(argument.getAllValues().get(0)).isNotSameAs(argument.getAllValues().get(1));
    }

    @Test
    void handleUserCreatedEvent_usernameWithHtmlChars_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(
                "<script>alert('xss')</script>", "html@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("<script>alert('xss')</script>");
    }

    @Test
    void handleUserCreatedEvent_usernameIdenticalToEmail_onlyUsernamePassed() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("shared@example.com", "shared@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("shared@example.com");
    }

    @Test
    void handleUserCreatedEvent_afterExceptionNextCallStillProcessed() {
        // given
        final UserCreatedIntegrationEvent failEvent = new UserCreatedIntegrationEvent("fail", "fail@example.com");
        final UserCreatedIntegrationEvent successEvent = new UserCreatedIntegrationEvent("success", "success@example.com");
        doThrow(new RuntimeException("first fails"))
                .doNothing()
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(failEvent))
                .isInstanceOf(RuntimeException.class);
        sut.handleUserCreatedEvent(successEvent);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(2)).handle(argument.capture());
        assertThat(argument.getAllValues().get(1).username()).isEqualTo("success");
    }

    @Test
    void handleUserCreatedEvent_zeroWidthCharsInUsername_preservedInCommand() {
        // given
        final String zeroWidthUsername = "user\u200B\u200Cname\u200D\uFEFF";
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(zeroWidthUsername, "zw@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo(zeroWidthUsername);
    }

    @Test
    void handleUserCreatedEvent_veryLongEmail_onlyUsernamePassed() {
        // given
        final String longEmail = "a".repeat(1000) + "@" + "b".repeat(1000) + ".com";
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", longEmail);

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("teacher");
    }

    @Test
    void handleUserCreatedEvent_nullByteInUsername_preservedInCommand() {
        // given
        final String nullByteUsername = "user\u0000name";
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(nullByteUsername, "nb@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo(nullByteUsername);
    }

    @Test
    void handleUserCreatedEvent_highFrequencySequentialCalls_allProcessed() {
        // given
        final int count = 50;
        final java.util.List<UserCreatedIntegrationEvent> events = java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new UserCreatedIntegrationEvent("user" + i, "user" + i + "@example.com"))
                .toList();

        // when
        events.forEach(sut::handleUserCreatedEvent);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(count)).handle(argument.capture());
        assertThat(argument.getAllValues()).hasSize(count);
        assertThat(argument.getAllValues())
                .extracting(CreateTeacherCommand::username)
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.rangeClosed(1, count)
                                .mapToObj(i -> "user" + i)
                                .toList());
    }

    @Test
    void handleUserCreatedEvent_surrogateCharactersInUsername_preservedInCommand() {
        // given
        final String surrogate = "user\uD83D\uDE00\uD83D\uDE01name";
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(surrogate, "surr@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo(surrogate);
    }

    @Test
    void handleUserCreatedEvent_commandHandlerThrowsOutOfMemoryError_errorPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");
        doThrow(new OutOfMemoryError("test OOM")).when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(OutOfMemoryError.class)
                .hasMessage("test OOM");
    }

    @Test
    void handleUserCreatedEvent_emptyStringEmail_usernameStillPassed() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("teacher");
    }

    @Test
    void handleUserCreatedEvent_numericOnlyUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("1234567890", "num@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("1234567890");
    }

    @Test
    void handleUserCreatedEvent_capturedCommandIsExactType_notSubclass() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "t@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().getClass()).isEqualTo(CreateTeacherCommand.class);
    }

    @Test
    void handleUserCreatedEvent_bothEmptyUsernameAndEmptyEmail_emptyUsernamePassedToCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("", "");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEmpty();
    }

    @Test
    void handleUserCreatedEvent_commandHandlerThrowsStackOverflowError_errorPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");
        doThrow(new StackOverflowError("test stack overflow")).when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(StackOverflowError.class)
                .hasMessage("test stack overflow");
    }

    @Test
    void handleUserCreatedEvent_controlCharactersInUsername_preservedInCommand() {
        // given
        final String controlCharsUsername = "\u0001\u0002\u0003\u001F";
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(controlCharsUsername, "ctrl@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo(controlCharsUsername);
    }

    @Test
    void handleUserCreatedEvent_commandHandlerThrowsRuntimeExceptionWithNullMessage_exceptionPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@example.com");
        doThrow(new RuntimeException((String) null)).when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(null);
    }

    @Test
    void handleUserCreatedEvent_usernameStringReferencePreservedInCommand() {
        // given
        final String username = new String("uniqueRef");
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(username, "ref@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isSameAs(username);
    }

    @Test
    void handleUserCreatedEvent_hasEventListenerAndAsyncAnnotations() throws NoSuchMethodException {
        // given
        final java.lang.reflect.Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);

        // then
        assertThat(method.isAnnotationPresent(org.springframework.context.event.EventListener.class)).isTrue();
        assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class)).isTrue();
    }

}
