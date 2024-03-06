package refraff.parser;

import refraff.parser.statement.Statement;
import java.util.List;

public class Program extends AbstractSyntaxTreeNode {
    
    private static final String TYPE = "program";
    private static final String TYPE_DESCRIPTOR = "Program";

    private final List<StructDef> structDefs;
    private final List<Statement> statements;

    public Program(final List<StructDef> structDefs,
                   final List<Statement> statements) {
        super(TYPE);
        this.statements = statements;
        this.structDefs = structDefs;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
