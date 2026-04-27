package com.taskapi.exception;

import com.taskapi.ai.AiNotConfiguredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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

    // ─── Erros de segurança ────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Invalid email or password", 401));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getMessage(), 401));
    }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getMessage(), 401));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("You do not have permission to access this resource", 403));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse(ex.getMessage(), 429));
    }

    // ─── Erros de IA ──────────────────────────────────────────────────────────

    @ExceptionHandler(AiNotConfiguredException.class)
    ResponseEntity<ErrorResponse> handleAiNotConfigured(AiNotConfiguredException ex) {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(ex.getMessage(), 503));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage(), 400));
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
