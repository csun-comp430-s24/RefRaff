package refraff.tokenizer.token;

public abstract class AbstractToken implements Token {

    private static final String DEFAULT_TOKEN_DESCRIPTOR = "token";

    /*
      Format for printing the token nicely: "<descriptor> '<parsedValue>'"
      e.g. "token '<='" or "identifier token 'bobby1'" or "integer literal '123'"

      May be more useful when debugging the compiler and getting useful error messages
     */
    private static final String TO_STRING_FORMAT = "%s '%s'";

    private final String parsedValue;

    public AbstractToken(String parsedValue) {
        this.parsedValue = parsedValue;
    }

    @Override
    public String getTokenDescriptor() {
        return DEFAULT_TOKEN_DESCRIPTOR;
    }

    @Override
    public String getParsedToken() {
        return this.parsedValue;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_FORMAT, getTokenDescriptor(), getParsedToken());
    }

}
