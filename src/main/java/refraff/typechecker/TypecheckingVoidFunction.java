package refraff.typechecker;

@FunctionalInterface
public interface TypecheckingVoidFunction<T, U> {

    void apply(T t, U u) throws TypecheckerException;

}
