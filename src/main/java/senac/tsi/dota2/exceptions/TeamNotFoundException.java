package senac.tsi.dota2.exceptions;

public class TeamNotFoundException extends RuntimeException{
    public TeamNotFoundException(Long id){
        super("Não foi possível encontrar o time com o ID: " + id);
    }
}
