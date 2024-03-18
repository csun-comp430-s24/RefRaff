package refraff.typechecker;

// import java.util.Map;

// import refraff.parser.Variable;
// import refraff.parser.expression.Expression;
// import refraff.parser.type.Type;

@FunctionalInterface
public interface TypecheckingFunction<T, U, R> {
    
    R apply(T t, U u) throws TypecheckerException;

}

