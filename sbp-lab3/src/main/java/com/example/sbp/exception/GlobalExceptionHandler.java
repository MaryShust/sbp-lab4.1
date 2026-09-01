package com.example.sbp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.BpmnError;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler({
            BankBicFormatException.class,
            PhoneNumberFormatException.class,
            OwnerNameFormatException.class,
            MessageFormatException.class
    })
    public ResponseEntity<Map<String, String>> handleFormatException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({
            UsernameNotFoundException.class,
            BankAccountNotFoundException.class,
            BillNotFoundException.class,
            TransactionNotFoundException.class,
            RoleNotFoundException.class,
            ExchangeRateNotFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleNotFoundException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({
            BankAccountInactiveException.class,
            BankAccountAlreadyExistsException.class,
            UserAlreadyExistsException.class,
            BillInactiveException.class
    })
    public ResponseEntity<Map<String, String>> handleConflictException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({
            InsufficientFundsException.class
    })
    public ResponseEntity<Map<String, String>> handlePaymentException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            LoginException.class
    })
    public ResponseEntity<Map<String, String>> handleAuthException(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({
            AccessDeniedException.class
    })
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({
            FileParseException.class,
            ExchangeRateParseException.class,
            BillNotBelongAccountExeption.class,
            Exception.class,
    })
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", ex.getMessage()));
    }

    @ExceptionHandler(BpmnError.class)
    public ResponseEntity<Map<String, String>> handleBpmnError(BpmnError ex) {
        log.info("TEST 1");
        String errorCode = ex.getErrorCode();
        String errorMessage = ex.getMessage();

        log.info("TEST 2=" + errorCode);
        HttpStatus status = switch (errorCode) {
            case "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            case "TRANSACTION_NOT_FOUND", "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "INACTIVE" -> HttpStatus.CONFLICT;
            case "INSUFFICIENT" -> HttpStatus.PAYMENT_REQUIRED;
            default -> HttpStatus.BAD_REQUEST;
        };
        log.info("TEST 3");

        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", errorMessage,
                        "code", errorCode
                ));
    }
}