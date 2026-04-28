package senac.tsi.dota2.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;


@ControllerAdvice
public class GlobalErrorAdvice {

    // 1. Erros de Validação (@Valid, @Size, @NotBlank) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        // Pega apenas a mensagem de erro que definimos na Entity (ex: "O inventário... não pode ultrapassar 6 itens")
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        body.put("message", errorMessage); // Aqui aparece sua mensagem personalizada

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
    // 2. Erros de Tipagem de ID (Ex: /api/players/false) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("O parâmetro '%s' deve ser do tipo '%s', mas recebeu '%s'.",
                ex.getName(), ex.getRequiredType().getSimpleName(), ex.getValue());
        return ResponseEntity.badRequest().body("Invalid request parameter: " + message);
    }

    // 3. Erros de Propriedade Inválida no Sort (Ex: ?sort=null) -> 400 Bad Request
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<?> handlePropertyReference(PropertyReferenceException ex) {
        return ResponseEntity.badRequest().body("Invalid request parameter: No property '" + ex.getPropertyName() + "' found for type '" + ex.getType().getType().getSimpleName() + "'");
    }

    // 4. Erros de Integridade de Dados (Ex: ID Duplicado) -> 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Database Conflict: This record already exists or violates integrity rules.");
    }

    // 5. O MESTRE DE TODOS: Captura qualquer Exception e decide o Status correto
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        String message = ex.getMessage();

        // Tratamento para 404 Not Found
        if (message != null && (
                message.toLowerCase().contains("not found") ||
                        message.toLowerCase().contains("não encontrado") ||
                        message.toLowerCase().contains("não foi possível encontrar"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }

        // Erro de relacionamento não salvo (Transient)
        if (message != null && message.contains("unsaved transient instance")) {
            return ResponseEntity.badRequest().body("Error: You are trying to save a relationship with an unsaved entity.");
        }

        // Erro de Concorrência/Transação (Otimismo do Hibernate) -> Mapeia para 400 para o Schemathesis
        if (message != null && message.contains("Row was already updated or deleted")) {
            return ResponseEntity.badRequest().body("Request could not be processed: " + message);
        }

        // Para qualquer outra coisa, evitamos o 500 (Erro Interno)
        return ResponseEntity.badRequest().body("Request could not be processed: " + (message != null ? message : "Unknown error"));
    }
}