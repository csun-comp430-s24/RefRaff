package refraff.typechecker;

@FunctionalInterface
public interface TypecheckingFunction<T, U, V, R> {
    
    R apply(T t, U u, V v) throws TypecheckerException;

}

