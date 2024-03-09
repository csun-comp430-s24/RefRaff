package refraff.parser.type;

public class FunctionName extends Type {

    private static final String FUNCTION_NAME = "function name";

    public final String functionName;

    public FunctionName(final String structName) {
        super(FUNCTION_NAME);

        this.functionName = structName;
    }

    @Override
    public String getParsedValue() {
        return this.functionName;
    }

}
