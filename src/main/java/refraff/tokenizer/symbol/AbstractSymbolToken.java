package refraff.tokenizer.symbol;

import refraff.tokenizer.AbstractToken;

public class AbstractSymbolToken extends AbstractToken {

    private static final String SYMBOL_TYPE_DESCRIPTOR = "token";

    public AbstractSymbolToken(String tokenizedValue) {
        super(tokenizedValue);
    }

    @Override
    public String getTokenTypeDescriptor() {
        return SYMBOL_TYPE_DESCRIPTOR;
    }

}
