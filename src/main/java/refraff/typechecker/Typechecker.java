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

    private Typechecker(Program program) throws TypecheckerException {
        this.program = program;

        // Map all the struct definitions names to their AST definitions
        // This throws an IllegalStateException if two structs share a name
        // Should we catch it and throw a TypecheckerException?
        try {
            this.structNameToDef = program.getStructDefs().stream()
                .collect(Collectors.toMap(StructDef::getStructName, structDef -> structDef));
        } catch (IllegalStateException e) {
            throw new TypecheckerException("Struct declarations share a name");
        }
    }

    public static void typecheckProgram(Program program) throws TypecheckerException {
        new Typechecker(program).typecheckProgram();
    }

    private void typecheckProgram() throws TypecheckerException {
        // Create an environment map
        Map<Variable, Type> typeEnv = new HashMap<>();
        typecheckStructDefs(typeEnv);
        typecheckStatements(typeEnv);
    }

    private void typecheckStructDefs(Map<Variable, Type> typeEnv) throws TypecheckerException {
        for (StructDef structDef : program.getStructDefs()) {
            final String typeErrorInStructMessage = "Type error in struct definition for "
                    + structDef.getStructName().structName;
            // Add struct def variable
            if (typeEnv.put(new Variable(structDef.getStructName().getName()), 
                    new StructType(structDef.getStructName())) != null) {
                throw new TypecheckerException(typeErrorInStructMessage + "duplicate variables with same name exist");
            }

            for (Param param : structDef.getParams()) {
                // If we add a variable, and it already exists, we have a duplicate
                if (typeEnv.put(param.variable, param.getType()) != null) {
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
            TypecheckingVoidFunction<Statement, Map<Variable, Type>>> STMT_TO_TYPE_FUNC = Map.of(
        AssignStmt.class, Typechecker::typecheckAssignStmt,
        BreakStmt.class, Typechecker::typecheckBreakStmt,
        ExpressionStmt.class, Typechecker::typecheckExpStmt,
        IfElseStmt.class, Typechecker::typecheckIfElseStmt,
        PrintlnStmt.class, Typechecker::typecheckPrintlnStmt,
        ReturnStmt.class, Typechecker::typecheckReturnStmt,
        StmtBlock.class, Typechecker::typecheckStmtBlock,
        VardecStmt.class, Typechecker::typecheckVardecStmt,
        WhileStmt.class, Typechecker::typecheckWhileStmt
    );

    private void typecheckStatements(Map<Variable, Type> typeEnv) throws TypecheckerException {
        
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

    public static void typecheckAssignStmt(final Statement assigStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static void typecheckBreakStmt(final Statement breakStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static void typecheckExpStmt(final Statement expStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        ExpressionStmt castExpStmt = (ExpressionStmt)expStmt;
        // Get expression from the expression statement, typecheck that
        typecheckExp(castExpStmt.getExpression(), typeEnv);
    }

    public static void typecheckIfElseStmt(final Statement ifElseStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static void typecheckPrintlnStmt(final Statement printLnStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static void typecheckReturnStmt(final Statement returnStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static void typecheckStmtBlock(final Statement stmtBlock, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static void typecheckVardecStmt(final Statement vardecStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        VardecStmt castVardecStmt = (VardecStmt)vardecStmt;
        // Get type
        Type type = castVardecStmt.getType();
        // Compare to expression
        Type expType = typecheckExp(castVardecStmt.getExpression(), typeEnv);
        // If these are the same type
        if (!type.equals(expType)) {
            throw new TypecheckerException("Vardec declared type is " + type.getParsedValue()
                                           + " but expression is " + expType.getParsedValue());
        }
        // Add variable to map (throw if already exists)
        if (typeEnv.put(castVardecStmt.getVariable(), type) != null) {
            throw new TypecheckerException("Variable " + castVardecStmt.getVariable().getName() 
                                           + " already declared");
        }
    }

    public static void typecheckWhileStmt(final Statement whileStmt, final Map<Variable, Type> typeEnv)
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
        // Use optionals instead of this
        Type type = typeEnv.get(castVarExp.getVar());
        if (type == null) {
            throw new TypecheckerException("Variable " + castVarExp.getVar().getName() 
                                           + " not previously declared");
        } else {
            return type;
        }
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
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public static Type typecheckUnaryOpExp(final Expression unaryOpExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
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
