package senac.tsi.dota2.exceptions;

public class PlayerProfileNotFoundException extends RuntimeException {
    public PlayerProfileNotFoundException(Long id) {
        super("Player profile not found with ID: " + id);
    }
}