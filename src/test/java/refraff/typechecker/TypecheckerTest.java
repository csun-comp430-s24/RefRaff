package refraff.typechecker;

import org.junit.Test;
import refraff.parser.Program;
import refraff.parser.Variable;
import refraff.parser.struct.Param;
import refraff.parser.struct.StructDef;
import refraff.parser.struct.StructName;
import refraff.parser.type.BoolType;
import refraff.parser.type.IntType;
import refraff.parser.type.StructType;
import refraff.parser.type.VoidType;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TypecheckerTest {

    // Test valid inputs

    private void testDoesNotThrowTypecheckerException(Program program) {
        assertDoesNotThrow(() -> Typechecker.typecheckProgram(program));
    }

    @Test
    public void testStructDefWithRecursiveStructVariableType() {
        /*
         * struct A {
         *   A a;
         * }
         */
        StructDef structDef = new StructDef(new StructName("A"), List.of(
                new Param(new StructType(new StructName("A")), new Variable("a"))
        ));

        Program program = new Program(List.of(structDef), List.of(), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testStructDefWithOtherStructVariableType() {
        /*
         * struct A {
         *   int foo;
         *   bool bar;
         * }
         */
        StructDef structDef1 = new StructDef(new StructName("A"), List.of(
                new Param(new IntType(), new Variable("foo")),
                new Param(new BoolType(), new Variable("bar"))
        ));

        StructDef structDef2 = new StructDef(new StructName("B"), List.of(
                new Param(new StructType(new StructName("A")), new Variable("a"))
        ));

        Program program = new Program(List.of(structDef1, structDef2), List.of(), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    

    // Test invalid inputs

    private void testThrowsTypecheckerException(Program invalidProgram) {
        assertThrows(TypecheckerException.class, () -> Typechecker.typecheckProgram(invalidProgram));
    }

    @Test
    public void testStructDefWithVoidTypeVariable() {
        /*
         * struct A {
         *   Void b;
         * }
         */
        StructDef invalidStructDef = new StructDef(new StructName("A"), List.of(
                new Param(new VoidType(), new Variable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructDefWithUndefinedStructTypeVariable() {
        /*
         * struct A {
         *   B b;
         * }
         */
        StructDef invalidStructDef = new StructDef(new StructName("A"), List.of(
                new Param(new StructType(new StructName("B")), new Variable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructDefWithRepeatedVariableName() {
        /*
         * struct A {
         *   int b;
         *   bool b;
         * }
         */
        StructDef invalidStructDef = new StructDef(new StructName("A"), List.of(
                new Param(new IntType(), new Variable("b")),
                new Param(new BoolType(), new Variable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

}
