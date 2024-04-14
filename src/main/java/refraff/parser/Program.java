package refraff.parser;

import refraff.parser.function.FunctionDef;
import refraff.parser.statement.Statement;
import refraff.parser.struct.StructDef;

import java.util.List;
import java.util.Objects;

public class Program extends AbstractSyntaxTreeNode {

    private static final String NODE_TYPE_DESCRIPTOR = "program";

    private final List<StructDef> structDefs;
    private final List<FunctionDef> functionDefs;
    private final List<Statement> statements;

    public Program(final List<StructDef> structDefs,
                   final List<FunctionDef> functionDefs,
                   final List<Statement> statements) {
        super(NODE_TYPE_DESCRIPTOR);

        this.statements = statements;
        this.functionDefs = functionDefs;
        this.structDefs = structDefs;
    }

    public List<StructDef> getStructDefs() {
        return structDefs;
    }

    public List<FunctionDef> getFunctionDefs() {
        return functionDefs;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getStructDefs(), getFunctionDefs(), getStatements());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof Program otherProgram
                && structDefs.equals(otherProgram.structDefs)
                && functionDefs.equals(otherProgram.functionDefs)
                && statements.equals(otherProgram.statements);
    }

}
