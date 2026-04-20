package com.taskapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Tratamento CENTRALIZADO de erros.
 *
 * Sem isso, o Spring retorna stack traces ou respostas genéricas.
 * Com isso, toda API retorna erros consistentes e legíveis.
 *
 * Isso é um critério eliminatório em code reviews europeus.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Erros de negócio ────────────────────────────────────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage(), 404));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage(), 404));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> handleEmailConflict(EmailAlreadyExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getMessage(), 409));
    }

    // ─── Erros de validação (@Valid) ─────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Streams! — junta todos os erros de campo em uma mensagem
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(message, 400));
    }

    // ─── Erro genérico (safety net) ──────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Nunca exponha a mensagem real de exceptions genéricas em produção
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An unexpected error occurred", 500));
    }

    // ─── DTO de erro padronizado ─────────────────────────────────────────────

    // Record para a resposta de erro — imutável, sem boilerplate
    public record ErrorResponse(
        String message,
        int status,
        LocalDateTime timestamp
    ) {
        // Construtor conveniente que já preenche o timestamp
        public ErrorResponse(String message, int status) {
            this(message, status, LocalDateTime.now());
        }
    }
}
