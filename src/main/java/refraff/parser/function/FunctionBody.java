package refraff.parser.function;

import java.util.List;

import refraff.parser.statement.StmtBlock;
import refraff.parser.statement.Statement;

public class FunctionBody extends StmtBlock {

    public FunctionBody() {
        super(List.of());
    }

    public FunctionBody(List<Statement> blockBody) {
        super(blockBody);
    }
    
}
