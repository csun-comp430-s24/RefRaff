package refraff.typechecker;

import refraff.parser.Program;
import refraff.parser.Variable;
import refraff.parser.struct.*;
import refraff.parser.type.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.operator.OperatorEnum;
import refraff.parser.expression.*;
import refraff.parser.statement.*;

import java.util.*;
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

    private void throwTypecheckerException(String beingParsed, String errorSuffix) throws TypecheckerException {
        final String errorPrefix = "Typechecker error when evaluating " + beingParsed + ": ";
        final String errorMessage = errorPrefix + errorSuffix;

        throw new TypecheckerException(errorMessage);
    }

    private void throwTypecheckerExceptionOnVariableExists(String beingParsed, Variable variable,
                                                                  Map<Variable, Type> typeEnv) throws TypecheckerException {
        if (!typeEnv.containsKey(variable)) {
            return;
        }

        final String errorSuffix = "expected new variable " + variable.name + " but it is already defined in scope";
        throwTypecheckerException(beingParsed, errorSuffix);
    }

    private Type throwTypecheckerExceptionOnVariableNotExists(String beingParsed, Variable variable,
                                                                     Map<Variable, Type> typeEnv) throws TypecheckerException {
        if (typeEnv.containsKey(variable)) {
            return typeEnv.get(variable);
        }

        final String errorSuffix = "expected variable " + variable.name + " to be defined in scope but it is not";
        throwTypecheckerException(beingParsed, errorSuffix);

        return null;
    }

    private void throwTypecheckerExceptionOnMismatchedTypes(String beingParsed, Type leftHand, Type rightHand)
            throws TypecheckerException {
        if (leftHand.equals(rightHand)) {
            return;
        }

        final String errorSuffix = "mismatched types LHS is a(n) " + leftHand.getParsedValue() +
                " RHS is a(n) " + rightHand.getParsedValue();
        throwTypecheckerException(beingParsed, errorSuffix);
    }

    private void typecheckStructDefs(Map<Variable, Type> typeEnv) throws TypecheckerException {
        for (StructDef structDef : program.getStructDefs()) {
            final String typeErrorInStructMessage = "Type error in struct definition for "
                    + structDef.getStructName().structName;
            // Add struct def variable

            // I don't think we should add these to the type environment - that would treat them as variables defined in scope
            // at all times. Take the example (valid C) below:

            /*
             * typedef struct A {
             *   struct A* a;
             * } A;
             *
             * int main() {
             *   A* a = NULL;
             *   a = a->a;
             *   return 0
             * }
             */

            // We wouldn't be able to vardec any struct parameter name in any statement in the function, since
            // the variable would always be in scope. But we would be able to assign a value to those variables,
            // even though that wouldn't make sense from the program's perspective. This could be an issue if we have a
            // struct field called `size` and wanted to use the variable name `size` anywhere else in the program.

            // I think the better solution would be to have the struct fields only be in scope for dot operations.
            // This would allow for programs equivalent to ones we could generate in our target language of C:

            /*
             * struct A {
             *   A a;
             * }
             *
             * A a = null;
             * a = a.a;
             */

            // For struct allocations, that could get a bit weirder, but we could treat the 'actual params' in structs
            // as labels for what we should expect to use as a parameter, instead of defined variables.
            // Something like the following:

            /*
             * struct B {
             *   int value;
             * }
             *
             * int value = 3;
             * new B { value: value; }; // Sets the value field of B to be whatever variable value we have in scope
             */

            // Similarly, we most likely would not want functions to put their variables on the type environment
            // until we enter the body of a function
            Set<Variable> variables = new HashSet<>();

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
            TypecheckingVoidFunction<Typechecker, Statement, Map<Variable, Type>>> STMT_TO_TYPE_FUNC = Map.of(
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
            STMT_TO_TYPE_FUNC.get(stmtClass).apply(this, stmt, typeEnv);
        }
    }

    public void typecheckAssignStmt(final Statement stmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        final String beingParsed = "assignment statement";

        AssignStmt assignStmt = (AssignStmt) stmt;
        Variable variable = assignStmt.variable;

        Type variableType = throwTypecheckerExceptionOnVariableNotExists(beingParsed, variable, typeEnv);
        Type expressionType = typecheckExp(assignStmt.expression, typeEnv);

        throwTypecheckerExceptionOnMismatchedTypes(beingParsed, variableType, expressionType);

        // Set the expression type to be type of the variable
        assignStmt.expression.setExpressionType(variableType);
    }

    public void typecheckBreakStmt(final Statement breakStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckExpStmt(final Statement expStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        ExpressionStmt castExpStmt = (ExpressionStmt)expStmt;
        // Get expression from the expression statement, typecheck that
        typecheckExp(castExpStmt.getExpression(), typeEnv);
    }

    public void typecheckIfElseStmt(final Statement ifElseStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckPrintlnStmt(final Statement printLnStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckReturnStmt(final Statement returnStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckStmtBlock(final Statement stmtBlock, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckVardecStmt(final Statement vardecStmt, final Map<Variable, Type> typeEnv)
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

        // Set the expression type to be type of the variable
        castVardecStmt.getExpression().setExpressionType(type);
    }

    public void typecheckWhileStmt(final Statement whileStmt, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    // Map of Expression classes to functions that return their types
    private static final Map<Class<? extends Expression>, 
            TypecheckingFunction<Typechecker, Expression, Map<Variable, Type>, Type>> EXP_TO_TYPE_FUNC = Map.of(
        BoolLiteralExp.class, (typechecker, exp, typeEnv) -> new BoolType(),
        FuncCallExp.class, Typechecker::typecheckFuncCallExp,
        IntLiteralExp.class, (typechecker, exp, typeEnv) -> new IntType(),
        NullExp.class, (typechecker, exp, typeEnv) -> new StructType(null),
        ParenExp.class, Typechecker::typecheckParenExp,
        StructAllocExp.class, Typechecker::typecheckStructAllocExp,
        VariableExp.class, Typechecker::typecheckVarExp,
        BinaryOpExp.class, Typechecker::typecheckerBinOpExp,
        DotExp.class, Typechecker::typecheckDotExp,
        UnaryOpExp.class, Typechecker::typecheckUnaryOpExp
    );

    public Type typecheckFuncCallExp(final Expression funcCallExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public Type typecheckParenExp(final Expression parenExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        ParenExp castParenExp = (ParenExp)parenExp;
        // Get expression in the parentheses, typecheck that
        return typecheckExp(castParenExp.getExp(), typeEnv);
    }

    public Type typecheckStructAllocExp(final Expression exp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        final String beingParsed = "struct allocation expression";
        StructAllocExp structAllocExp = (StructAllocExp) exp;

        // Struct name should be a safe unwrap - the parser will have looked for an identifier after new, not a null token
        StructType structType = structAllocExp.getStructType();
        StructName structName = structType.getStructName().get();

        final String structWhereWeAre = "struct " + structName.structName;
        StructDef structDef = structNameToDef.get(structName);

        if (structDef == null) {
            throwTypecheckerException(beingParsed, structWhereWeAre + " is not defined");
        }

        List<Param> structDefinedParams = structDef.getParams();
        List<StructActualParam> structAllocParams = structAllocExp.getParams().params;

        int definedParameters = structDefinedParams.size();
        int actualParameters = structAllocParams.size();

        if (definedParameters != actualParameters) {
            final String errorSuffixFormat = "expected exactly %d allocation parameters for " + structWhereWeAre
                    + " but received %d allocation parameters";
            throwTypecheckerException(beingParsed, String.format(errorSuffixFormat, definedParameters, actualParameters));
        }

        for (int i = 0; i < definedParameters; i++) {
            Param definedParam = structDefinedParams.get(i);
            StructActualParam allocationParam = structAllocParams.get(i);

            Variable definedVariable = definedParam.variable;

            // Check that the variable names match (in order)
            if (!definedVariable.equals(allocationParam.var)) {
                throwTypecheckerException(beingParsed, "expected allocation variable " + definedVariable.name
                        + " but received " + allocationParam.var.name);
            }

            Type definedType = definedParam.type;

            Expression allocationExp = allocationParam.exp;
            Type allocationExpType = typecheckExp(allocationExp, typeEnv);

            // Check that the type defined matches the allocation expression's type
            throwTypecheckerExceptionOnMismatchedTypes(beingParsed + " " + structWhereWeAre + " for allocation variable "
                    + definedVariable.name, definedType, allocationExpType);

            // This parameter is safe!
        }

        // All parameters are safe!
        structAllocExp.setExpressionType(structAllocExp.getStructType());
        return structAllocExp.getStructType();
    }

    public Type typecheckVarExp(final Expression variableExp, final Map<Variable, Type> typeEnv)
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
    private static final Map<OperatorEnum, List<Type>> OP_TO_OPERAND_TYPE = new HashMap<>() {{
        put(OperatorEnum.NOT, List.of(new BoolType()));
        put(OperatorEnum.OR, Arrays.asList(new BoolType()));
        put(OperatorEnum.AND, Arrays.asList(new BoolType()));
        put(OperatorEnum.DOUBLE_EQUALS, Arrays.asList(new BoolType(), new IntType()));
        put(OperatorEnum.NOT_EQUALS, Arrays.asList(new BoolType(), new IntType()));
        put(OperatorEnum.LESS_THAN_EQUALS, Arrays.asList(new IntType()));
        put(OperatorEnum.GREATER_THAN_EQUALS, Arrays.asList(new IntType()));
        put(OperatorEnum.LESS_THAN, Arrays.asList(new IntType()));
        put(OperatorEnum.GREATER_THAN, Arrays.asList(new IntType()));
        put(OperatorEnum.PLUS, Arrays.asList(new IntType()));
        put(OperatorEnum.MINUS, Arrays.asList(new IntType()));
        put(OperatorEnum.MULTIPLY, Arrays.asList(new IntType()));
        put(OperatorEnum.DIVISION, Arrays.asList(new IntType()));
    }};

    // Map of Enums to the types they evaluate to
    private static final Map<OperatorEnum, Type> OP_TO_EVAL_TYPE = new HashMap<>() {{
        put(OperatorEnum.NOT, new BoolType());
        put(OperatorEnum.OR, new BoolType());
        put(OperatorEnum.AND, new BoolType());
        put(OperatorEnum.DOUBLE_EQUALS, new BoolType());
        put(OperatorEnum.NOT_EQUALS, new BoolType());
        put(OperatorEnum.LESS_THAN_EQUALS, new BoolType());
        put(OperatorEnum.GREATER_THAN_EQUALS, new BoolType());
        put(OperatorEnum.LESS_THAN, new BoolType());
        put(OperatorEnum.GREATER_THAN, new BoolType());
        put(OperatorEnum.PLUS, new IntType());
        put(OperatorEnum.MINUS, new IntType());
        put(OperatorEnum.MULTIPLY, new IntType());
        put(OperatorEnum.DIVISION, new IntType());
    }};

    public boolean operandsAreValidType(List<Type> validTypes, Type type) {
        return validTypes.contains(type);
    }

    // In all cases, we need our types to match - what if LHT and RHT are both in the valid type list, but aren't the same?
    public boolean operandsAreValidType(List<Type> validTypes, Type leftHandType, Type rightHandType) {
        return validTypes.contains(leftHandType) && validTypes.contains(rightHandType);
    }

    public Type typecheckerBinOpExp(final Expression binaryOpExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        // Get the left and right hand sides
        BinaryOpExp castBinOpExp = (BinaryOpExp)binaryOpExp;
        Type leftHandType = typecheckExp(castBinOpExp.getLeftExp(), typeEnv);
        Type rightHandType = typecheckExp(castBinOpExp.getRightExp(), typeEnv);
        OperatorEnum op = castBinOpExp.getOp();

        if (op == OperatorEnum.DOT) {
            return typecheckDotExp(binaryOpExp, typeEnv);
        }

        // This may be unnecessary, but check that the operand is in the map
        if (!OP_TO_OPERAND_TYPE.containsKey(op) || !OP_TO_EVAL_TYPE.containsKey(op)) {
            throw new TypecheckerException("Expected operator, but found: " + op.toString());
        }
        // Use the operator to decide what type the expressions should be
        List<Type> validOperandTypes = OP_TO_OPERAND_TYPE.get(op);

        // Throw exception if the operands are invalid types
        if (!operandsAreValidType(validOperandTypes, leftHandType, rightHandType)) {
            throw new TypecheckerException("Operands are of invalid type: " + validOperandTypes.toString()
                    + ", " + leftHandType.getNodeTypeDescriptor() + " "
                    + rightHandType.getNodeTypeDescriptor());
        }

        // Return the binary operation's evaluation type and set our type for the next expression
        Type evalType = OP_TO_EVAL_TYPE.get(op);
        binaryOpExp.setExpressionType(evalType);

        return evalType;
    }

    public Type typecheckDotExp(final Expression exp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        final String beingParsed = "dot expression";

        DotExp dotExp = (DotExp) exp;
        Type leftHandType = typecheckExp(dotExp.getLeftExp(), typeEnv);

        if (!(leftHandType instanceof StructType)) {
            final String errorSuffix = "expected a reference to a struct instance but received a type of "
                    + leftHandType.getParsedValue();
            throwTypecheckerException(beingParsed, errorSuffix);
        }

        StructType structType = (StructType) leftHandType;
        if (structType.isNullStruct()) {
            final String errorSuffix = "expected a reference to a struct instance but received raw reference to null";
            throwTypecheckerException(beingParsed, errorSuffix);
        }

        // By this point, if we evaluated an expression that is a type of struct, that struct should absolutely exist
        StructDef structDef = structNameToDef.get(structType.getStructName().get());

        Variable structField = dotExp.getRightVar();
        Type structFieldType = null;

        for (Param param : structDef.getParams()) {
            if (structField.equals(param.variable)) {
                structFieldType = param.type;
                break;
            }
        }

        // Check that the variable we used does exist as a parameter (or field) for the struct
        if (structFieldType == null) {
            final String errorSuffix = "expected a valid struct field but received unknown field " + structField.name;
            throwTypecheckerException(beingParsed, errorSuffix);
        }

        dotExp.setExpressionType(structFieldType);
        return structFieldType;
    }

    public Type typecheckUnaryOpExp(final Expression exp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        // This is VERY similar to parsing binary ops, probably could refactor at some point
        UnaryOpExp unaryOpExp = (UnaryOpExp) exp;
        OperatorEnum op = unaryOpExp.getOp();

        Expression expression = unaryOpExp.getExp();
        Type expressionType = typecheckExp(expression, typeEnv);

        List<Type> validTypes = OP_TO_OPERAND_TYPE.get(op);
        if (!operandsAreValidType(validTypes, expressionType)) {
            // This should really be a more descriptive error message depending on the list of valid types
            throwTypecheckerException(op.getSymbol() + " expression", "type mismatch");
        }

        Type evalType = OP_TO_EVAL_TYPE.get(op);
        unaryOpExp.setExpressionType(evalType);

        return evalType;
    }

    public Type typecheckExp(final Expression exp,
                                    final Map<Variable, Type> typeEnv) throws TypecheckerException {

        // Get the expression's class
        Class<? extends Expression> expClass = exp.getClass();

        if (!EXP_TO_TYPE_FUNC.containsKey(expClass)) {
            // Do I throw an error here? This should be an expression and it isn't? IDK
            throw new TypecheckerException("Expected expression, got: " + expClass.toString());
        }

        Type type = EXP_TO_TYPE_FUNC.get(expClass).apply(this, exp, typeEnv);
        return type;
    }

}
