package refraff.tokenizer.reserved;

public class PrintlnToken extends AbstractReservedToken {

    private static final String PRINTLN_RESERVED_WORD = "println";
    
    public PrintlnToken() {
        super(PRINTLN_RESERVED_WORD);
    }

}
