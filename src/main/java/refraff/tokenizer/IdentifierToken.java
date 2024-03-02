package refraff.tokenizer;

public class IdentifierToken extends AbstractToken {

    private static final String IDENTIFIER_TOKEN_DESCRIPTOR = "identifier token";

    public IdentifierToken(String tokenizedValue) {
        super(tokenizedValue);
    }

    @Override
    public String getTokenTypeDescriptor() {
        return IDENTIFIER_TOKEN_DESCRIPTOR;
    }

}
