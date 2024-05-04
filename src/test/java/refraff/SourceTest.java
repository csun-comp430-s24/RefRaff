package refraff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SourceTest {

    private static final Class<? extends Exception> SOURCE_EXCEPTION_TYPE = IllegalArgumentException.class;

    @Test
    public void testFromSourceWithZeroSourcesThrowsException() {
        assertThrows(SOURCE_EXCEPTION_TYPE, Source::fromSources);
    }

    @Test
    public void testFromSourceWithOneSourceEqualsOriginalSource() {
        assertDoesNotThrow(() -> {
            Source source = Source.DEFAULT_TESTING_SOURCE;
            assertEquals(source, Source.fromSources(source));
        });
    }

    @Test
    public void testFromSourceWithMultipleSources() {
        // int x = true;
        assertDoesNotThrow(() -> {
            Source source1 = new Source("int", new SourcePosition(1, 1), new SourcePosition(1, 4));
            Source source2 = new Source("x", new SourcePosition(1, 5), new SourcePosition(1, 6));
            Source source3 = new Source("=", new SourcePosition(1, 7), new SourcePosition(1, 8));
            Source source4 = new Source("true", new SourcePosition(1, 9), new SourcePosition(1, 13));
            Source source5 = new Source(";", new SourcePosition(1, 13), new SourcePosition(1, 14));

            Source expectedSource = new Source("int x = true;", source1.getStartPosition(), source5.getEndPosition());
            Source actualSource = Source.fromSources(source1, source2, source3, source4, source5);

            assertEquals(expectedSource, actualSource);
        });
    }

    @Test
    public void testFromSourceWithMultilineSources() {
        String expectedInput = """
                func foo(int a): int {
                  return a + 2;
                }""";


        // func foo(int a): int {
        Source funcSource = new Source("func", new SourcePosition(1, 1), new SourcePosition(1, 5));
        Source fooSource = new Source("foo", new SourcePosition(1, 6), new SourcePosition(1, 9));
        Source leftParenSource = new Source("(", new SourcePosition(1, 9), new SourcePosition(1, 10));
        Source intParamSource = new Source("int", new SourcePosition(1, 10), new SourcePosition(1, 13));
        Source aParamSource = new Source("a", new SourcePosition(1, 14), new SourcePosition(1, 15));
        Source rightParenSource = new Source(")", new SourcePosition(1, 15), new SourcePosition(1, 16));
        Source colonSource = new Source(":", new SourcePosition(1, 16), new SourcePosition(1, 17));
        Source intReturnSource = new Source("int", new SourcePosition(1, 18), new SourcePosition(1, 21));
        Source leftBraceSource = new Source("{", new SourcePosition(1, 22), new SourcePosition(1, 23));

        // <space><space>return a + 2;
        Source returnSource = new Source("return", new SourcePosition(2, 3), new SourcePosition(2, 9));
        Source aSource = new Source("a", new SourcePosition(2, 10), new SourcePosition(2, 11));
        Source plusSource = new Source("+", new SourcePosition(2, 12), new SourcePosition(2, 13));
        Source twoSource = new Source("2", new SourcePosition(2, 14), new SourcePosition(2, 15));
        Source semicolonSource = new Source(";", new SourcePosition(2, 15), new SourcePosition(2, 16));

        // }
        Source rightBrace = new Source("}", new SourcePosition(3, 1), new SourcePosition(3, 2));

        Source expectedSource = new Source(expectedInput, funcSource.getStartPosition(), rightBrace.getEndPosition());
        Source actualSource = Source.fromSources(funcSource, fooSource, leftParenSource, intParamSource,
                aParamSource, rightParenSource, colonSource, intReturnSource, leftBraceSource, returnSource,
                aSource, plusSource, twoSource, semicolonSource, rightBrace);

        assertEquals(expectedSource, actualSource);
    }

}
