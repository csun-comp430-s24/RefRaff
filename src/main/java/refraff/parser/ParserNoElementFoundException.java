package refraff.parser;

// Thrown when a parser cannot find the specific type
public class ParserNoElementFoundException extends ParserException {

    public ParserNoElementFoundException(String message) {
        super("Parser exception: " + message + " could not be parsed");
    }

}
