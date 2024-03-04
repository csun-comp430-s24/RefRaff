package refraff.tokenizer.reserved;

import refraff.tokenizer.AbstractToken;

public class AbstractReservedToken extends AbstractToken {

    private static final String RESERVED_WORD_DESCRIPTOR = "reserved word";

    public AbstractReservedToken(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getTokenTypeDescriptor() {
        return RESERVED_WORD_DESCRIPTOR;
    }
}
