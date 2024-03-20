package refraff.typechecker;

@FunctionalInterface
public interface TypecheckingVoidFunction<T, U, V> {

    void apply(T t, U u, V v) throws TypecheckerException;

}
