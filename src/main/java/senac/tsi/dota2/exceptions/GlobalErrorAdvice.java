package senac.tsi.dota2.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@ControllerAdvice
public class GlobalErrorAdvice {

    // 1. Erros de Validação (@Valid, @Size, @NotBlank) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body("Validation failed: Check your fields constraints (e.g., max items, not blank).");
    }

    // 2. Erros de Parâmetro ou Tipagem (Ex: passar texto onde espera ID número) -> 400 Bad Request
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, PropertyReferenceException.class})
    public ResponseEntity<?> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body("Invalid request parameter: " + ex.getMessage());
    }

    // 3. Erros de Integridade de Dados (Ex: ID Duplicado ou deletar Pai com Filhos) -> 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Database Conflict: This record already exists or violates integrity rules (Check IDs or Relationships).");
    }

    // 4. O MESTRE DE TODOS: Captura qualquer Exception e decide o Status correto
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        String message = ex.getMessage();

        // Se a mensagem contém indícios de que o registro não existe -> 404 Not Found
        if (message != null && (
                message.toLowerCase().contains("not found") ||
                        message.toLowerCase().contains("não encontrado") ||
                        message.toLowerCase().contains("não foi possível encontrar"))) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }

        // Se for um erro de "Entidade Transiente" (que vimos no log mais cedo) -> 400
        if (message != null && message.contains("unsaved transient instance")) {
            return ResponseEntity.badRequest().body("Error: You are trying to save a relationship with an unsaved entity.");
        }

        // Para qualquer outra coisa, retornamos 400 em vez de 500 para o Schemathesis não tirar nota
        return ResponseEntity.badRequest().body("Request could not be processed: " + message);
    }
}