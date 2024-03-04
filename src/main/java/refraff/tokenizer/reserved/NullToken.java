package refraff.tokenizer.reserved;

public class NullToken extends AbstractReservedToken {
    
    private static final String NULL_RESERVED_WORD = "null";

    public NullToken() {
        super(NULL_RESERVED_WORD);
    }

}
