package refraff.typechecker;

import refraff.SourcePosition;
import refraff.parser.AbstractSyntaxTreeNode;
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

    private final Map<Standardized<StructName>, StructDef> structNameToDef;

    private Typechecker(Program program) {
        this.program = program;

        this.structNameToDef = new HashMap<>();
    }

    public static void typecheckProgram(Program program) throws TypecheckerException {
        new Typechecker(program).typecheckProgram();
    }

    private void typecheckProgram() throws TypecheckerException {
        // Create an environment map
        Map<Standardized<Variable>, Type> typeEnv = new HashMap<>();
        typecheckStructDefs(typeEnv);
        typecheckStatements(typeEnv);
    }

    private void throwTypecheckerExceptionOnVariableExists(String beingParsed, AbstractSyntaxTreeNode parent, Variable variable,
                                                                  Map<Standardized<Variable>, Type> typeEnv) throws TypecheckerException {
        if (!typeEnv.containsKey(Standardized.of(variable))) {
            return;
        }

        final String errorSuffix = "variable `" + variable.getName() + "` is already defined in this scope";
        throwTypecheckerException(beingParsed, parent, variable, errorSuffix);
    }

    private Type throwTypecheckerExceptionOnVariableNotExists(String beingParsed, AbstractSyntaxTreeNode parent, Variable variable,
                                                                     Map<Standardized<Variable>, Type> typeEnv) throws TypecheckerException {
        Standardized<Variable> standardizedVariable = Standardized.of(variable);
        if (typeEnv.containsKey(standardizedVariable)) {
            return typeEnv.get(standardizedVariable);
        }

        final String errorSuffix = "variable `" + variable.getName() + "` is not defined in this scope";
        throwTypecheckerException(beingParsed, parent, variable, errorSuffix);

        return null;
    }

    private void throwTypecheckerExceptionOnMismatchedTypes(String beingParsed, AbstractSyntaxTreeNode parent,
                                                            AbstractSyntaxTreeNode rightHandChild,
                                                            Type leftHand, Type rightHand)
            throws TypecheckerException {
        if (leftHand.hasTypeEquality(rightHand)) {
            return;
        }

        final String errorSuffix = rightHandChild.getNodeTypeDescriptor() + " does not match expected type " +
                leftHand.getSource().getSourceString();
        throwTypecheckerException(beingParsed, parent, rightHandChild, errorSuffix);
    }

    private void throwTypecheckerException(String beingParsed, AbstractSyntaxTreeNode parent,
                                           AbstractSyntaxTreeNode child, String errorMessage) throws TypecheckerException {
        SourcePosition childStartPosition = child.getSource().getStartPosition();

        StringBuilder errorMessageBuilder = new StringBuilder();

        errorMessageBuilder.append("Typechecker error when evaluating ");
        errorMessageBuilder.append(beingParsed);
        errorMessageBuilder.append(" at ");
        errorMessageBuilder.append(childStartPosition.toString());
        errorMessageBuilder.append(":\n");

        String parentNodeString = parent.getSource().getSourceString();

        SourcePosition parentStartPosition = parent.getSource().getStartPosition();
        SourcePosition childEndPosition = child.getSource().getEndPosition();

        int numberOfLinesToPrint = (childEndPosition.getLinePosition() - parentStartPosition.getLinePosition()) + 1;

        // Append all the lines until the child is fully displayed
        parentNodeString.lines()
                .limit(numberOfLinesToPrint)
                .forEach(line -> {
                    errorMessageBuilder.append(line);
                    errorMessageBuilder.append('\n');
                });

        int numberOfSpacesToPrint = numberOfLinesToPrint == 1
                ? childStartPosition.getColumnPosition() - parentStartPosition.getColumnPosition()
                : childStartPosition.getColumnPosition() - 1;

        String spacePrefix = " ".repeat(numberOfSpacesToPrint);
        String caretPointer = "^".repeat(childEndPosition.getColumnPosition() - childStartPosition.getColumnPosition());

        // Add spaces until we're under the child, then use the caret pointer to point to the child and new line
        errorMessageBuilder.append(spacePrefix);
        errorMessageBuilder.append(caretPointer);
        errorMessageBuilder.append('\n');

        // Add space until we're under the child, then print the error message below the caret pointer and new line
        errorMessageBuilder.append(spacePrefix);
        errorMessageBuilder.append("    ");
        errorMessageBuilder.append(errorMessage);
        errorMessageBuilder.append('\n');

        String builtErrorMessage = errorMessageBuilder.toString();
        throw new TypecheckerException(builtErrorMessage);
    }

    private void typecheckStructDefs(Map<Standardized<Variable>, Type> typeEnv) throws TypecheckerException {
        final String typeErrorInStructMessageFormat = "struct definition for `%s`";
        
        // Add all the structs manually before trying to parse a struct's fields (allow recursion/reference other structs)

        // Map all the struct definitions names to their AST definitions
        for (StructDef structDef : program.getStructDefs()) {
            StructName structName = structDef.getStructName();
            Standardized<StructName> standardizedStructName = Standardized.of(structName);
            
            // If we don't already have a struct definition defined, add it to the map
            if (!structNameToDef.containsKey(standardizedStructName)) {
                structNameToDef.put(standardizedStructName, structDef);
                continue;
            }
            
            String stringStructName = structName.getName();
            throwTypecheckerException(String.format(typeErrorInStructMessageFormat, stringStructName),
                    structDef, structName, "struct `" + stringStructName + "` has already been defined");
        }
        
        for (StructDef structDef : program.getStructDefs()) {
            final String typeErrorInStructMessage = "struct definition for `" + structDef.getStructName().structName + "`";
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
            Set<Standardized<Variable>> standardizedVariables = new HashSet<>();

            for (Param param : structDef.getParams()) {
                // If we add a variable, and it already exists, we have a duplicate
                if (!standardizedVariables.add(Standardized.of(param.variable))) {
                    throwTypecheckerException(typeErrorInStructMessage, structDef, param,
                            param.variable.getName() + " is already defined in this struct");
                }

                // Else, check that all variable declarations for the struct do not: have a return type of void
                // and that any struct name does exist
                final String onVariableDeclaration = typeErrorInStructMessage + " on parameter `" +
                        param.variable.getName() + "` declaration";
                typecheckTypeNotVoidAndStructNameMustExist(onVariableDeclaration, structDef, param.getType());
            }
        }
    }

    private void typecheckTypeNotVoidAndStructNameMustExist(String error, AbstractSyntaxTreeNode parent,
                                                            Type type) throws TypecheckerException {
        // If we are a void type, throw an error
        if (type instanceof VoidType) {
            throwTypecheckerException(error, parent, type, "`void` is only a valid type for function return values");
        }

        // If we aren't a struct type, we don't care
        if (!(type instanceof StructType)) {
            return;
        }

        // If we are a struct type, make sure we have a valid struct definition
        StructType paramStructType = (StructType) type;
        StructName paramStructName = paramStructType.getStructName().get();

        if (structNameToDef.containsKey(Standardized.of(paramStructName))) {
            return;
        }

        String detailedErrorMessage = String.format("struct type `%s` is not defined", paramStructName.getName());
        throwTypecheckerException(error, parent, type, detailedErrorMessage);
    }

    // Map of statements to their typechecking functions
    private static final Map<Class<? extends Statement>, 
            TypecheckingVoidFunction<Typechecker, Statement, Map<Standardized<Variable>, Type>>> STMT_TO_TYPE_FUNC = Map.of(
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

    private void typecheckStatements(Map<Standardized<Variable>, Type> typeEnv) throws TypecheckerException {
        
        for (Statement stmt : program.getStatements()) {          
            // Get the statements class
            Class<? extends Statement> stmtClass = stmt.getClass();

            if (!STMT_TO_TYPE_FUNC.containsKey(stmtClass)) {
                // Isn't a statement?
                throw new UnsupportedOperationException("Map did not contain mapping function for: " + stmtClass);
            }

            // These functions will throw exceptions if there are type errors
            STMT_TO_TYPE_FUNC.get(stmtClass).apply(this, stmt, typeEnv);
        }
    }

    public void typecheckAssignStmt(final Statement stmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        final String beingParsed = "assignment statement";

        AssignStmt assignStmt = (AssignStmt) stmt;
        Variable variable = assignStmt.variable;

        Type variableType = throwTypecheckerExceptionOnVariableNotExists(beingParsed, stmt, variable, typeEnv);
        Type expressionType = typecheckExp(assignStmt.expression, typeEnv);

        throwTypecheckerExceptionOnMismatchedTypes(beingParsed, stmt, expressionType, variableType, expressionType);

        // Set the expression type to be type of the variable
        assignStmt.expression.setExpressionType(variableType);
    }

    public void typecheckBreakStmt(final Statement breakStmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckExpStmt(final Statement expStmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        ExpressionStmt castExpStmt = (ExpressionStmt)expStmt;
        // Get expression from the expression statement, typecheck that
        typecheckExp(castExpStmt.getExpression(), typeEnv);
    }

    public void typecheckIfElseStmt(final Statement ifElseStmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckPrintlnStmt(final Statement printLnStmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckReturnStmt(final Statement returnStmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckStmtBlock(final Statement stmtBlock, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public void typecheckVardecStmt(final Statement vardecStmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        VardecStmt castVardecStmt = (VardecStmt)vardecStmt;

        // Get type and make sure it's not void or a struct name that does not exist
        Type type = castVardecStmt.getType();
        typecheckTypeNotVoidAndStructNameMustExist("vardec statement", vardecStmt, type);

        // Compare to expression
        Type expType = typecheckExp(castVardecStmt.getExpression(), typeEnv);

        // Throw if these aren't the same type
        throwTypecheckerExceptionOnMismatchedTypes("vardec statement", vardecStmt, expType, type, expType);

        // Add variable to map (throw if already exists)
        if (typeEnv.put(Standardized.of(castVardecStmt.getVariable()), type) != null) {
            throwTypecheckerExceptionOnVariableExists("vardec statement", vardecStmt, castVardecStmt.getVariable(),
                    typeEnv);
        }

        // Set the expression type to be type of the variable
        castVardecStmt.getExpression().setExpressionType(type);
    }

    public void typecheckWhileStmt(final Statement whileStmt, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    // Map of Expression classes to functions that return their types
    private static final Map<Class<? extends Expression>, 
            TypecheckingFunction<Typechecker, Expression, Map<Standardized<Variable>, Type>, Type>> EXP_TO_TYPE_FUNC = Map.of(
                    // We already handle the expression types in the parser for literal values
                    BoolLiteralExp.class, (typechecker, exp, typeEnv) -> exp.getExpressionType(),
                    IntLiteralExp.class, (typechecker, exp, typeEnv) -> exp.getExpressionType(),
                    NullExp.class, (typechecker, exp, typeEnv) -> exp.getExpressionType(),

                    // We need to typecheck these manually
                    FuncCallExp.class, Typechecker::typecheckFuncCallExp,
                    ParenExp.class, Typechecker::typecheckParenExp,
                    StructAllocExp.class, Typechecker::typecheckStructAllocExp,
                    VariableExp.class, Typechecker::typecheckVarExp,
                    BinaryOpExp.class, Typechecker::typecheckerBinOpExp,
                    DotExp.class, Typechecker::typecheckDotExp,
                    UnaryOpExp.class, Typechecker::typecheckUnaryOpExp
    );

    public Type typecheckFuncCallExp(final Expression funcCallExp, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        throw new TypecheckerException("Not implemented yet!");
        // Type test = new VoidType();
        // return test;
    }

    public Type typecheckParenExp(final Expression parenExp, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        ParenExp castParenExp = (ParenExp)parenExp;
        // Get expression in the parentheses, typecheck that
        return typecheckExp(castParenExp.getExp(), typeEnv);
    }

    public Type typecheckStructAllocExp(final Expression exp, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        final String beingParsed = "struct allocation expression";
        StructAllocExp structAllocExp = (StructAllocExp) exp;

        // Struct name should be a safe unwrap - the parser will have looked for an identifier after new, not a null token
        StructType structType = structAllocExp.getStructType();
        StructName structName = structType.getStructName().get();

        final String structWhereWeAre = "struct " + structName;
        StructDef structDef = structNameToDef.get(Standardized.of(structName));

        if (structDef == null) {
            throwTypecheckerException(beingParsed, exp, structType, structWhereWeAre + " is not defined");
        }

        List<Param> structDefinedParams = structDef.getParams();
        List<StructActualParam> structAllocParams = structAllocExp.getParams().params;

        int definedParameters = structDefinedParams.size();
        int actualParameters = structAllocParams.size();

        if (definedParameters != actualParameters) {
            final String errorSuffixFormat = "expected exactly %d allocation parameters for " + structWhereWeAre
                    + " but received %d allocation parameters";
            String errorSuffix = String.format(errorSuffixFormat, definedParameters, actualParameters);

            AbstractSyntaxTreeNode child = actualParameters == 0 ? exp : structAllocParams.get(actualParameters - 1);
            throwTypecheckerException(beingParsed, exp, child, errorSuffix);
        }

        for (int i = 0; i < definedParameters; i++) {
            Param definedParam = structDefinedParams.get(i);
            StructActualParam allocationParam = structAllocParams.get(i);

            Variable definedVariable = definedParam.variable;

            // Check that the variable names match (in order)
            if (!Standardized.standardizedEquals(definedVariable, allocationParam.var)) {
                throwTypecheckerException(beingParsed, exp, allocationParam, "expected allocation for variable `"
                        + definedVariable.name + "` but received allocation for variable `" + allocationParam.var.name + "`");
            }

            Type definedType = definedParam.type;

            Expression allocationExp = allocationParam.exp;
            Type allocationExpType = typecheckExp(allocationExp, typeEnv);

            // Check that the type defined matches the allocation expression's type
            throwTypecheckerExceptionOnMismatchedTypes(beingParsed + " " + structWhereWeAre + " for allocation variable `"
                    + definedVariable.name + "`", exp, allocationExpType, definedType, allocationExpType);

            // This parameter is safe!
        }

        // All parameters are safe!
        structAllocExp.setExpressionType(structAllocExp.getStructType());
        return structAllocExp.getStructType();
    }

    public Type typecheckVarExp(final Expression expression, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        VariableExp variableExp = (VariableExp) expression;

        return throwTypecheckerExceptionOnVariableNotExists("variable expression", variableExp, variableExp.getVar(),
                typeEnv);
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
        for (Type validType : validTypes) {
            if (validType.hasTypeEquality(type)) {
                return true;
            }
        }

        return false;
    }

    // In all cases, we need our types to match - what if LHT and RHT are both in the valid type list, but aren't the same?
    public boolean operandsAreValidType(List<Type> validTypes, Type leftHandType, Type rightHandType) {
        return operandsAreValidType(validTypes, leftHandType) && leftHandType.hasTypeEquality(rightHandType);
    }

    public Type typecheckerBinOpExp(final Expression binaryOpExp, final Map<Standardized<Variable>, Type> typeEnv)
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
            throw new UnsupportedOperationException("Map did not find binary operator for: " + op.toString());
        }

        // Use the operator to decide what type the expressions should be
        List<Type> validOperandTypes = OP_TO_OPERAND_TYPE.get(op);

        final String error = op.getSymbol() + " expression";

        // Throw exception if the operands are invalid types
        throwTypecheckerExceptionOnMismatchedTypes(error, binaryOpExp, castBinOpExp.getRightExp(),
                leftHandType, rightHandType);

        if (!operandsAreValidType(validOperandTypes, leftHandType, rightHandType)) {
            // This should really be a more descriptive error message depending on the list of valid types
            String errorMessage = String.format("expected one type of: {%s}", validOperandTypes.stream()
                    .map(type -> "`" + type.getNodeTypeDescriptor() + "`")
                    .collect(Collectors.joining(", ")));

            throwTypecheckerException(error, binaryOpExp, binaryOpExp, errorMessage);
        }

        // Return the binary operation's evaluation type and set our type for the next expression
        Type evalType = OP_TO_EVAL_TYPE.get(op);
        binaryOpExp.setExpressionType(evalType);

        return evalType;
    }

    public Type typecheckDotExp(final Expression exp, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        final String beingParsed = "dot expression";

        DotExp dotExp = (DotExp) exp;
        Type leftHandType = typecheckExp(dotExp.getLeftExp(), typeEnv);

        if (!(leftHandType instanceof StructType)) {
            final String errorSuffix = "expected a struct reference but received a type of `"
                    + leftHandType.getParsedValue() + "`";
            throwTypecheckerException(beingParsed, exp, leftHandType, errorSuffix);
        }

        StructType structType = (StructType) leftHandType;
        if (structType.isNullStruct()) {
            final String errorSuffix = "expected a struct reference but received raw `null` reference";
            throwTypecheckerException(beingParsed, exp, leftHandType, errorSuffix);
        }

        // By this point, if we evaluated an expression that is a type of struct, that struct should absolutely exist
        StructName structName = structType.getStructName().get();
        StructDef structDef = structNameToDef.get(Standardized.of(structName));

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
            final String errorSuffix = "parameter `" + structField.name + "` is not defined on struct `" + structType + "`";
            throwTypecheckerException(beingParsed, exp, structField, errorSuffix);
        }

        dotExp.setExpressionType(structFieldType);
        return structFieldType;
    }

    public Type typecheckUnaryOpExp(final Expression exp, final Map<Standardized<Variable>, Type> typeEnv)
            throws TypecheckerException {
        // This is VERY similar to parsing binary ops, probably could refactor at some point
        UnaryOpExp unaryOpExp = (UnaryOpExp) exp;
        OperatorEnum op = unaryOpExp.getOp();

        Expression expression = unaryOpExp.getExp();
        Type expressionType = typecheckExp(expression, typeEnv);

        List<Type> validTypes = OP_TO_OPERAND_TYPE.get(op);
        if (!operandsAreValidType(validTypes, expressionType)) {
            // This should really be a more descriptive error message depending on the list of valid types
            String errorMessage = String.format("expected one type of: {%s}", validTypes.stream()
                            .map(type -> "`" + type.getNodeTypeDescriptor() + "`")
                            .collect(Collectors.joining(", ")));

            throwTypecheckerException(op.getSymbol() + " expression", exp, expressionType, errorMessage);
        }

        Type evalType = OP_TO_EVAL_TYPE.get(op);
        unaryOpExp.setExpressionType(evalType);

        return evalType;
    }

    public Type typecheckExp(final Expression exp,
                                    final Map<Standardized<Variable>, Type> typeEnv) throws TypecheckerException {

        // Get the expression's class
        Class<? extends Expression> expClass = exp.getClass();

        if (!EXP_TO_TYPE_FUNC.containsKey(expClass)) {
            // Do I throw an error here? This should be an expression and it isn't? IDK
            // Maybe an "unsupported exception" letting us know we did development bad is good here
            throw new UnsupportedOperationException("Map did not contain mapping function for: " + expClass);
        }

        Type type = EXP_TO_TYPE_FUNC.get(expClass).apply(this, exp, typeEnv);
        return type;
    }

}
