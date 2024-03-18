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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    }

    private void typecheckStructDefs() throws TypecheckerException {
        for (StructDef structDef : program.getStructDefs()) {
            final String typeErrorInStructMessage = "Type error in struct definition for "
                    + structDef.getStructName().structName;
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

    // Map of Expression classes to functions that return their types
    private static final Map<Class<? extends Expression>, 
            TypecheckingFunction<Expression, Map<Variable, Type>, Type>> EXP_TO_TYPE = Map.of(
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

    public static Type typecheckFuncCallExp(final Expression binExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckParenExp(final Expression binExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckStructAllocExp(final Expression binExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckVarExp(final Expression binExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckerBinOpExp(final Expression binExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckDotExp(final Expression binExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckUnaryOpExp(final Expression binExp, final Map<Variable, Type> typeEnv)
            throws TypecheckerException {
        Type test = new VoidType();
        return test;
    }

    public static Type typecheckExp(final Expression exp,
                                    final Map<Variable, Type> typeEnv) throws TypecheckerException {

        // Get the expression's class
        Class<? extends Expression> expClass = exp.getClass();

        if (!EXP_TO_TYPE.containsKey(expClass)) {
            // Do I throw an error here? This should be an expression and it isn't? IDK
            throw new TypecheckerException("Expected expression, got: " + expClass.toString());
        }

        Type type = EXP_TO_TYPE.get(expClass).apply(exp, typeEnv);
        return type;
    

        // if (exp instanceof IntLiteralExp) {
        //     return new IntType();
        // } else if (exp instanceof BoolLiteralExp) {
        //     return new BoolType();
        // } else if (exp instanceof VariableExp) {
        //     final Variable variable = ((VariableExp)exp).getVar();
        //     if (typeEnv.get(variable) {
        //         return typeEnv.get(variable);
        //     } else {
        //         throw new TypecheckerException("Variable not in scope: " + variable.name);
        //     }
        // } else if (exp instanceof BinaryOpExp) {
        //     final BinaryOpExp asBin = (BinaryOpExp)asBin;
        //     return typecheckBin(asBin, typeEnv);
        // } else {
        //     assert(false);
        //     throw new TypecheckerException("Haven't implemented expression typechecking yet");
        // }
    }






    /*
     * private final static Map<Token, OperatorEnum> TOKEN_TO_OP = Map.ofEntries(
        new SimpleImmutableEntry<>(new OrToken(), OperatorEnum.OR),
        new SimpleImmutableEntry<>(new AndToken(), OperatorEnum.AND),
        new SimpleImmutableEntry<>(new DoubleEqualsToken(), OperatorEnum.DOUBLE_EQUALS),
        new SimpleImmutableEntry<>(new NotEqualsToken(), OperatorEnum.NOT_EQUALS),
        new SimpleImmutableEntry<>(new LessThanEqualsToken(), OperatorEnum.LESS_THAN_EQUALS),
        new SimpleImmutableEntry<>(new GreaterThanEqualsToken(), OperatorEnum.GREATER_THAN_EQUALS),
        new SimpleImmutableEntry<>(new PlusToken(), OperatorEnum.PLUS),
        new SimpleImmutableEntry<>(new MinusToken(), OperatorEnum.MINUS),
        new SimpleImmutableEntry<>(new MultiplyToken(), OperatorEnum.MULTIPLY),
        new SimpleImmutableEntry<>(new DivisionToken(), OperatorEnum.DIVISION),
        new SimpleImmutableEntry<>(new NotToken(), OperatorEnum.NOT),
        new SimpleImmutableEntry<>(new DotToken(), OperatorEnum.DOT)
    );
     */
}
