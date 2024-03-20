package refraff.parser.function;

import refraff.parser.type.Type;

import java.util.Objects;

public class FunctionName extends Type {

    private static final String NODE_TYPE_DESCRIPTOR = "function name";

    public final String functionName;

    public FunctionName(final String structName) {
        super(NODE_TYPE_DESCRIPTOR);

        this.functionName = structName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), functionName);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof FunctionName otherFunctionName
                && Objects.equals(functionName, otherFunctionName.functionName);
    }

}
