package refraff.parser;

// Thrown when a parser finds something that is explicitly malformed
public class ParserMalformedException extends ParserException {

    public ParserMalformedException(String message) {
        super(message);
    }

}
