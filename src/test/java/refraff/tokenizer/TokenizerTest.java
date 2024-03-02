package refraff.tokenizer;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TokenizerTest {

    @Test
    public void testUniqueSymbolCharacters() {
        String uniqueSymbolCharacters = Tokenizer.UNIQUE_SYMBOL_CHARACTERS;

        for (Token token : Tokenizer.SYMBOL_TOKENS) {
            for (char individualSymbol : token.getTokenizedValue().toCharArray()) {
                String individualSymbolString = String.valueOf(individualSymbol);
                boolean containsIndividualSymbol = uniqueSymbolCharacters.contains(individualSymbolString);

                assertTrue("Unique symbol characters did not contain individual symbol " + individualSymbol,
                        containsIndividualSymbol);
            }
        }
    }

}
