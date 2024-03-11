package refraff.parser;

public interface Node {
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

}
