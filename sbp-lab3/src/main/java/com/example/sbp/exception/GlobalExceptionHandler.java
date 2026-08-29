package com.example.sbp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
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
}