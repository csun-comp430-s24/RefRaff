package refraff.parser;

import refraff.Source;
import refraff.SourcePosition;
import refraff.Sourceable;

public interface Node extends Sourceable {
    /**
     * A description of the nodes's type. For example, 'int type' or 'plus operator'.
     *
     * @return a description of the node's type
     */
    String getNodeTypeDescriptor();

    /**
     * The string that obtained by the parser.
     *
     * @return the string that was obtained by the parser
     */
    String getParsedValue();

    Node setSource(Source source) throws IllegalStateException;

    @SuppressWarnings("unchecked")
    static <T extends Node> T setNodeSource(T t, String sourceString) {
        return (T) t.setSource(
                new Source(
                        sourceString,
                        SourcePosition.DEFAULT_SOURCE_POSITION,
                        SourcePosition.DEFAULT_SOURCE_POSITION)
                );
    }

    @SuppressWarnings("unchecked")
    static <T extends Node> T setNodeSource(T t, Source source) {
        return (T) t.setSource(source);
    }

}
