package refraff.tokenizer.reserved;

public class FuncToken extends AbstractReservedToken {
    private static final String INT_RESERVED_WORD = "func";

    public FuncToken() {
        super(INT_RESERVED_WORD);
    }
}
