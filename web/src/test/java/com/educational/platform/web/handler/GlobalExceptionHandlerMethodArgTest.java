package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerMethodArgTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void onMethodArgumentNotValidException_returnsBadRequestWithFieldErrors() {
        // given
        final MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        final BindingResult bindingResult = mock(BindingResult.class);
        final FieldError fieldError = new FieldError("object", "name", "must not be blank");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsExactly("must not be blank");
    }

    @Test
    void onMethodArgumentNotValidException_multipleFieldErrors_returnsAll() {
        // given
        final MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        final BindingResult bindingResult = mock(BindingResult.class);
        final FieldError error1 = new FieldError("object", "name", "must not be blank");
        final FieldError error2 = new FieldError("object", "email", "invalid format");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsExactly("must not be blank", "invalid format");
    }

    @Test
    void onMethodArgumentNotValidException_noFieldErrors_returnsEmptyErrors() {
        // given
        final MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        final BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).isEmpty();
    }
}
