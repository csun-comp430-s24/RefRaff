package refraff.typechecker;

@FunctionalInterface
public interface TypecheckingFunction<T, U, R> {
    
    R apply(T t, U u) throws TypecheckerException;

}

