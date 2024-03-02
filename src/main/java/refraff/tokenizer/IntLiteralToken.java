package refraff.tokenizer;

public class IntLiteralToken extends AbstractToken {

    private static final String INT_LITERAL_DESCRIPTOR = "int literal";

    public IntLiteralToken(String tokenizedValue) {
        super(tokenizedValue);
    }

    @Override
    public String getTokenTypeDescriptor() {
        return INT_LITERAL_DESCRIPTOR;
    }

}
