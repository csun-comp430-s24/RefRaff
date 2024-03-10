package refraff.parser;

@FunctionalInterface
public interface ParsingFunction<T, R> {

    R apply(T t) throws ParserException;

}
