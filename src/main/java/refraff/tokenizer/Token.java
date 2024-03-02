package refraff.tokenizer;

public interface Token {

    /**
     * A description of the token's type. For example, 'int literal' or 'reserved word'.
     *
     * @return a description of the token's type
     */
    String getTokenTypeDescriptor();

    /**
     * The string that obtained by the tokenizer.
     *
     * @return the string that was obtained by the tokenizer
     */
    String getTokenizedValue();

}
