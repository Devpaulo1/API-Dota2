package senac.tsi.dota2.exceptions;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(Long id) {
        super("Player not found with ID: " + id);
    }
}