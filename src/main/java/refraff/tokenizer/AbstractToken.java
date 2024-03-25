package refraff.tokenizer;

import refraff.Source;
import refraff.SourcePosition;

public abstract class AbstractToken implements Token {

    /*
      Format for printing the token nicely: "<descriptor> '<tokenizedValue>'"
      e.g. "token '<='"
           "identifier token 'bobby1'"
           "int literal '123'"
           "reserved word 'return'"

      This may be more useful when debugging the compiler and getting helpful error messages.
     */
    private static final String TO_STRING_FORMAT = "%s '%s'";

    private final String tokenizedValue;

    public AbstractToken(String tokenizedValue) {
        this.tokenizedValue = tokenizedValue;
    }

    @Override
    public String getTokenizedValue() {
        return this.tokenizedValue;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_FORMAT, getTokenTypeDescriptor(), getTokenizedValue());
    }

    @Override
    public int hashCode() {
        return tokenizedValue.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Token otherToken && getTokenizedValue().equals(otherToken.getTokenizedValue());
    }

}
