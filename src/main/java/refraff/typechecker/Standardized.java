package refraff.typechecker;

import refraff.Source;
import refraff.SourcePosition;
import refraff.parser.Node;
import refraff.parser.Variable;
import refraff.parser.struct.StructName;

import java.util.Objects;
import java.util.function.Function;

public class Standardized<T extends Node> {

    private final T standardizedNode;

    private Standardized(T original, Function<String, T> nodeConstructor) {
        String originalSourceString = original.getSource().getSourceString();

        this.standardizedNode = nodeConstructor.apply(originalSourceString);
        this.standardizedNode.setSource(getStandardizedSource(originalSourceString));
    }

    private Source getStandardizedSource(String originalSourceString) {
        return new Source(
                originalSourceString,
                SourcePosition.DEFAULT_SOURCE_POSITION,
                SourcePosition.DEFAULT_SOURCE_POSITION
        );
    }

    public T getStandardizedNode() {
        return this.standardizedNode;
    }

    @Override
    public int hashCode() {
        return getStandardizedNode().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Standardized<?> otherStandardized
                && Objects.equals(getStandardizedNode(), otherStandardized.getStandardizedNode());
    }

    @Override
    public String toString() {
        return "Standardized position node: " + standardizedNode.toString();
    }

    public static Standardized<Variable> of(Variable variable) {
        return new Standardized<>(variable, Variable::new);
    }

    public static boolean standardizedEquals(Variable variable1, Variable variable2) {
        Standardized<Variable> standardized1 = Standardized.of(variable1);
        Standardized<Variable> standardized2 = Standardized.of(variable2);

        return standardized1.equals(standardized2);
    }

    public static Standardized<StructName> of(StructName structName) {
        return new Standardized<>(structName, StructName::new);
    }

}
