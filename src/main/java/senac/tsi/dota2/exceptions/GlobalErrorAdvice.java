package senac.tsi.dota2.exceptions;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
public class GlobalErrorAdvice {

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<?> handleSortError(PropertyReferenceException ex) {
        return ResponseEntity.badRequest().body("Invalid sort parameter: " + ex.getPropertyName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body("Invalid parameter value: " + ex.getValue());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body("Validation failed for some fields.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDatabaseIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body("Database error: Check if related entities exist.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        return ResponseEntity.badRequest().body("Request could not be processed: " + ex.getMessage());
    }
}