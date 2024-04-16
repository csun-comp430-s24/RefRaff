package refraff.codegen;

@FunctionalInterface
public interface CodegenVoidFunction<T, U> {

    void apply(T t, U u) throws CodegenException;

}
