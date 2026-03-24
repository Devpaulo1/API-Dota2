package senac.tsi.dota2.exceptions;

public class HeroNotFoundException extends RuntimeException{
    public HeroNotFoundException(Long id) {

        super("Não foi possivel encontrar o herói com o ID: " + id);
    }
}
