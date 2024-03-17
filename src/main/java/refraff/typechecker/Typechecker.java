package refraff.typechecker;

import refraff.parser.Program;
import refraff.parser.Variable;
import refraff.parser.struct.Param;
import refraff.parser.struct.StructDef;
import refraff.parser.struct.StructName;
import refraff.parser.type.StructType;
import refraff.parser.type.Type;
import refraff.parser.type.VoidType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

}
