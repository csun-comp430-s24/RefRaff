package refraff.typechecker;

import refraff.parser.ParseResult;
import refraff.parser.Program;
import refraff.parser.Variable;
import refraff.parser.struct.Param;
import refraff.parser.struct.StructDef;
import refraff.parser.struct.StructName;
import refraff.parser.type.*;
import refraff.tokenizer.IdentifierToken;
import refraff.tokenizer.Token;
import refraff.tokenizer.reserved.BoolToken;
import refraff.tokenizer.reserved.IntToken;
import refraff.tokenizer.reserved.VoidToken;
import refraff.tokenizer.symbol.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.operator.OperatorEnum;
import refraff.parser.expression.*;
import refraff.parser.statement.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Typechecker {

    private final Program program;

    private final Map<StructName, StructDef> structNameToDef;

    private Typechecker(Program program) {
        this.program = program;

        // Map all the struct definitions names to their AST definitions
        this.structNameToDef = program.getStructDefs().stream()
                .collect(Collectors.toMap(StructDef::getStructName, structDef -> structDef));
    }

    public static void typecheckProgram(Program program) throws TypecheckerException {
        new Typechecker(program).typecheckProgram();
    }

    private void typecheckProgram() throws TypecheckerException {
        typecheckStructDefs();
        typecheckStatements();
    }

    private void typecheckStructDefs() throws TypecheckerException {
        for (StructDef structDef : program.getStructDefs()) {
            final String typeErrorInStructMessage = "Type error in struct definition for "
                    + structDef.getStructName().structName;
            // Should this be global? We'll need to check other vardecs against it
            final Set<Variable> variables = new HashSet<>(); 

            for (Param param : structDef.getParams()) {
                // If we add a variable, and it already exists, we have a duplicate
                if (!variables.add(param.variable)) {
                    throw new TypecheckerException(typeErrorInStructMessage + "duplicate variables with same name exist");
                }

                // Else, check that all variable declarations for the struct do not: have a return type of void
                // and that any struct name does exist
                typecheckParamNoVoidAndStructNameMustExist(typeErrorInStructMessage, param);
            }
        }
    }

    private void typecheckParamNoVoidAndStructNameMustExist(String prefix, Param param) throws TypecheckerException {
        final String onVariableDeclaration = String.format(prefix + ", on variable declaration for %s: ",
                param.variable.name);
        Type paramType = param.type;

        if (paramType instanceof VoidType) {
            throw new TypecheckerException(onVariableDeclaration + "void is not a valid variable type");
        }

        if (!(paramType instanceof StructType)) {
            return;
        }

        StructType paramStructType = (StructType) paramType;
        StructName paramStructName = paramStructType.getStructName().get();

        if (structNameToDef.containsKey(paramStructName)) {
            return;
        }

        String errorMessage = String.format("struct type %s is not defined", paramStructName.structName);
        throw new TypecheckerException(onVariableDeclaration + errorMessage);
    }

    // Map of statements to their typechecking functions
    private static final Map<Class<? extends Statement>, 
            TypecheckingFunction<Statement, Map<Variable, Type>, Type>> STMT_TO_TYPE_FUNC = Map.of(
        AssignStmt.class, Typechecker::typecheckAssignStmt,
        ExpressionStmt.class, Typechecker::typecheckExpStmt,
        IfElseStmt.class, Typechecker::typecheckIfElseStmt,
        PrintlnStmt.class, Typechecker::typecheckPrintlnStmt,
        ReturnStmt.class, Typechecker::typecheckReturnStmt,
        StmtBlock.class, Typechecker::typecheckStmtBlock,
        VardecStmt.class, Typechecker::typecheckVardecStmt,
        WhileStmt.class, Typechecker::typecheckerWhileStmt
    );

    private void typecheckStatements() throws TypecheckerException {
        // Create an environment map
        Map<Variable, Type> typeEnv = new HashMap<>();

        for (Statement stmt : program.getStatements()) {          
            // Get the statements class
            Class<? extends Statement> stmtClass = stmt.getClass();

            if (!STMT_TO_TYPE_FUNC.containsKey(stmtClass)) {
                // Isn't a statement?
                throw new TypecheckerException("Expected statement, got: " + stmtClass.toString());
            }

            // These functions will throw exceptions if there are type errors
            STMT_TO_TYPE_FUNC.get(stmtClass).apply(stmt, typeEnv);
        }
    }

    public static Type typecheckAssignStmt(final Statement assigStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckExpStmt(final Statement expStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        ExpressionStmt castExpStmt = (ExpressionStmt)expStmt;
        // Get expression from the expression statement, typecheck that
        return typecheckExp(castExpStmt.getExpression(), typeEnv);
    }

    public static Type typecheckIfElseStmt(final Statement ifElseStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckPrintlnStmt(final Statement printLnStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckReturnStmt(final Statement returnStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckStmtBlock(final Statement stmtBlock, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckVardecStmt(final Statement vardecStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckerWhileStmt(final Statement whileStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    // Map of Expression classes to functions that return their types
    private static final Map<Class<? extends Expression>, 
            TypecheckingFunction<Expression, Map<Variable, Type>, Type>> EXP_TO_TYPE_FUNC = Map.of(
        BoolLiteralExp.class, (exp, typeEnv) -> new BoolType(),
        FuncCallExp.class, Typechecker::typecheckFuncCallExp,
        IntLiteralExp.class, (exp, typeEnv) -> new IntType(),
        NullExp.class, (exp, typeEnv) -> new VoidType(),
        ParenExp.class, Typechecker::typecheckParenExp,
        StructAllocExp.class, Typechecker::typecheckStructAllocExp,
        VariableExp.class, Typechecker::typecheckVarExp,
        BinaryOpExp.class, Typechecker::typecheckerBinOpExp,
        DotExp.class, Typechecker::typecheckDotExp,
        UnaryOpExp.class, Typechecker::typecheckUnaryOpExp
    );

    public static Type typecheckFuncCallExp(final Expression funcCallExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckParenExp(final Expression parenExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        ParenExp castParenExp = (ParenExp)parenExp;
        // Get expression in the parentheses, typecheck that
        return typecheckExp(castParenExp.getExp(), typeEnv);
    }

    public static Type typecheckStructAllocExp(final Expression structAllocExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckVarExp(final Expression variableExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        VariableExp castVarExp = (VariableExp)variableExp;
        return typeEnv.get(castVarExp.getVar());
    }

    // Map of Enums to the types they can operate on
    private static final Map<OperatorEnum, List<Type>> BIN_OP_TO_OPERAND_TYPE = new HashMap<>() {{
        put(OperatorEnum.OR, Arrays.asList(new BoolType()));
        put(OperatorEnum.AND, Arrays.asList(new BoolType()));
        put(OperatorEnum.DOUBLE_EQUALS, Arrays.asList(new BoolType(), new IntType()));
        put(OperatorEnum.NOT_EQUALS, Arrays.asList(new BoolType(), new IntType()));
        put(OperatorEnum.LESS_THAN_EQUALS, Arrays.asList(new IntType()));
        put(OperatorEnum.GREATER_THAN_EQUALS, Arrays.asList(new IntType()));
        put(OperatorEnum.PLUS, Arrays.asList(new IntType()));
        put(OperatorEnum.MINUS, Arrays.asList(new IntType()));
        put(OperatorEnum.MULTIPLY, Arrays.asList(new IntType()));
        put(OperatorEnum.DIVISION, Arrays.asList(new IntType()));
    }};

    // Map of Enums to the types they evaluate to
    private static final Map<OperatorEnum, Type> BIN_OP_TO_EVAL_TYPE = new HashMap<>() {{
        put(OperatorEnum.OR, new BoolType());
        put(OperatorEnum.AND, new BoolType());
        put(OperatorEnum.DOUBLE_EQUALS, new BoolType());
        put(OperatorEnum.NOT_EQUALS, new BoolType());
        put(OperatorEnum.LESS_THAN_EQUALS, new BoolType());
        put(OperatorEnum.GREATER_THAN_EQUALS, new BoolType());
        put(OperatorEnum.PLUS, new IntType());
        put(OperatorEnum.MINUS, new IntType());
        put(OperatorEnum.MULTIPLY, new IntType());
        put(OperatorEnum.DIVISION, new IntType());
    }};

    public static boolean operandsAreValidType(List<Type> validTypes, Type leftHandType, Type rightHandType) {
        return validTypes.contains(leftHandType) && validTypes.contains(rightHandType);
    }

    public static Type typecheckerBinOpExp(final Expression binaryOpExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        // Get the left and right hand sides
        BinaryOpExp castBinOpExp = (BinaryOpExp)binaryOpExp;
        Type leftHandType = typecheckExp(castBinOpExp.getLeftExp(), typeEnv);
        Type rightHandType = typecheckExp(castBinOpExp.getRightExp(), typeEnv);
        OperatorEnum op = castBinOpExp.getOp();

        // This may be unnecessary, but check that the operand is in the map
        if (!BIN_OP_TO_OPERAND_TYPE.containsKey(op) || !BIN_OP_TO_EVAL_TYPE.containsKey(op)) {
            throw new TypecheckerException("Expected operator, but found: " + op.toString());
        }
        // Use the operator to decide what type the expressions should be
        List<Type> validOperandTypes = BIN_OP_TO_OPERAND_TYPE.get(op);

        // Throw exception if the operands are invalid types
        if (!operandsAreValidType(validOperandTypes, leftHandType, rightHandType)) {
            throw new TypecheckerException("Operands are of invalid type: " + validOperandTypes.toString()
                    + ", " + leftHandType.getNodeTypeDescriptor() + " "
                    + rightHandType.getNodeTypeDescriptor());
        }

        // Return the binary operation's evaluation type
        return BIN_OP_TO_EVAL_TYPE.get(op);
    }

    public static Type typecheckDotExp(final Expression dotExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckUnaryOpExp(final Expression unaryOpExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckExp(final Expression exp,
                                    final Map<Variable, Type> typeEnv) throws TypecheckerException {

        // Get the expression's class
        Class<? extends Expression> expClass = exp.getClass();

        if (!EXP_TO_TYPE_FUNC.containsKey(expClass)) {
            // Do I throw an error here? This should be an expression and it isn't? IDK
            throw new TypecheckerException("Expected expression, got: " + expClass.toString());
        }

        Type type = EXP_TO_TYPE_FUNC.get(expClass).apply(exp, typeEnv);
        return type;
    }
}
