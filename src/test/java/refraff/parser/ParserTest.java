package refraff.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import refraff.Source;
import refraff.SourcePosition;
import refraff.Sourced;
import refraff.parser.function.*;
import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.*;
import refraff.parser.type.*;
import refraff.parser.expression.*;
import refraff.parser.statement.*;
import refraff.parser.struct.*;
import refraff.parser.operator.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.typechecker.Typechecker;
import refraff.typechecker.TypecheckerException;
import refraff.util.ResourceUtil;


public class ParserTest {

    private Token[] toArray(Token... tokens) {
        return tokens;
    }

    private List<Sourced<Token>> toSourcedList(Token... tokens) {
        return Arrays.stream(tokens)
                .map(token -> new Sourced<>(Source.DEFAULT_TESTING_SOURCE, token))
                .toList();
    }

    private ParseResult<Program> parseProgram(Token... tokens) throws ParserException {
        return parseProgram(toSourcedList(tokens));
    }

    private ParseResult<Program> parseProgram(List<Sourced<Token>> sourcedTokens) throws ParserException {
        return new ParseResult<>(Parser.parseProgram(sourcedTokens), sourcedTokens.size());
    }

    private <T extends AbstractSyntaxTreeNode> Optional<ParseResult<T>> testParsesWithoutException(
            ParsingFunction<Parser, Optional<ParseResult<T>>> parsingFunction, List<Sourced<Token>> defaultSourcedTokens) {
        try {
            final Parser parser = new Parser(defaultSourcedTokens);
            return parsingFunction.apply(parser);
        } catch (ParserException ex) {
            fail(ex);
        }

        throw new IllegalStateException("This will never occur.");
    }

    private void testProgramMatchesExpectedResult(Program expectedValue, Token... tokens) {
        testMatchesExpectedResult(parser -> Optional.of(parseProgram(tokens)), expectedValue, tokens);
    }

    private void testProgramMatchesExpectedResult(Program expectedValue, List<Sourced<Token>> sourcedTokens) {
        testMatchesExpectedResult(parser -> Optional.of(parseProgram(sourcedTokens)), expectedValue, sourcedTokens);
    }

    private <T extends AbstractSyntaxTreeNode> void testMatchesExpectedResult(ParsingFunction<Parser,
            Optional<ParseResult<T>>> parsingFunction, T expectedValue, Token... tokens) {
        testMatchesExpectedResult(parsingFunction, expectedValue, toSourcedList(tokens));
    }

    private <T extends AbstractSyntaxTreeNode> void testMatchesExpectedResult(ParsingFunction<Parser,
            Optional<ParseResult<T>>> parsingFunction, T expectedValue, List<Sourced<Token>> sourcedTokens) {
        Optional<ParseResult<T>> optionalParseResult = testParsesWithoutException(parsingFunction, sourcedTokens);
        assertFalse("Parse result must contain something", optionalParseResult.isEmpty());

        ParseResult<T> expectedResult = new ParseResult<>(expectedValue, sourcedTokens.size());
        ParseResult<T> actualResult = optionalParseResult.get();

        assertEquals(expectedResult, actualResult);
    }
    
    // Test valid inputs

    private void testExpressionMatchesExpectedResult(Expression expression, Token... inputs) {
        testMatchesExpectedResult(parser -> parser.parseExp(0), expression, toSourcedList(inputs));
    }

    @Test
    public void testTrueBoolLiteralExp() {
        testExpressionMatchesExpectedResult(new BoolLiteralExp(true), new TrueToken());
    }

    @Test
    public void testFalseBoolLiteralExp() {
        testExpressionMatchesExpectedResult(new BoolLiteralExp(false), new FalseToken());
    }

    @Test
    public void testIntLiteralExp() {
        testExpressionMatchesExpectedResult(new IntLiteralExp(7), new IntLiteralToken("7"));
    }

    private void testTypeMatchesExpectedResult(Type expected, Token input) {
        testMatchesExpectedResult(parser -> parser.parseType(0), expected, input);
    }

    @Test
    public void testTypeEquals() {
        assertEquals(new IntType(), new IntType());
    }

    @Test // For coverage, honestly
    public void testVariableEquals() {
        assertEquals(new Variable("example"), new Variable("example"));
    }

    @Test
    public void testParseIntType() {
        testTypeMatchesExpectedResult(new IntType(), new IntToken());
    }

    @Test
    public void testParseBoolType() {
        testTypeMatchesExpectedResult(new BoolType(), new BoolToken());
    }

    @Test
    public void testParseVoidType() {
        testTypeMatchesExpectedResult(new VoidType(), new VoidToken());
    }

    @Test
    public void testParseStructNameType() {
        testTypeMatchesExpectedResult(new StructType(new StructName("a")), new IdentifierToken("a"));
    }

    private void testFunctionDef(FunctionName functionName, List<Param> params, Type returnType,
                                 StmtBlock stmtBlock, Token... tokens) {
        FunctionDef expectedFunctionDef = new FunctionDef(functionName, params, returnType, stmtBlock);
        Program expectedProgram = new Program(List.of(), List.of(expectedFunctionDef), List.of());

        testProgramMatchesExpectedResult(expectedProgram, tokens);
    }

    @Test
    public void testFunctionDefWithNoParams() {
        // func a() : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new RightParenToken(), new ColonToken(),
                new IntToken(), new LeftBraceToken(), new RightBraceToken());

        testFunctionDef(new FunctionName("a"), List.of(), new IntType(), new StmtBlock(List.of()), input);
    }

    @Test
    public void testFunctionDefWithOneParam() {
        // func a(int b) : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new ColonToken(), new IntToken(), new LeftBraceToken(), new RightBraceToken());

        testFunctionDef(new FunctionName("a"), List.of(
                new Param(new IntType(), new Variable("b"))
        ), new IntType(), new StmtBlock(List.of()), input);
    }

    @Test
    public void testFunctionDefWithMultipleParams() {
        // func a(int b, custom c) : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"), new LeftParenToken(),
                new IntToken(), new IdentifierToken("b"), new CommaToken(),
                new IdentifierToken("custom"), new IdentifierToken("c"), new RightParenToken(),
                new ColonToken(), new IntToken(), new LeftBraceToken(), new RightBraceToken());

        testFunctionDef(new FunctionName("a"), List.of(
                new Param(new IntType(), new Variable("b")),
                new Param(new StructType(new StructName("custom")), new Variable("c"))
        ), new IntType(), new StmtBlock(List.of()), input);
    }

    private void testStatementMatchesExpected(Statement statement, Token... input) {
        Program expectedProgram = new Program(List.of(), List.of(), List.of(statement));
        testProgramMatchesExpectedResult(expectedProgram, input);
    }

    @Test
    public void testParseVardec() throws ParserException {
        Token[] input = toArray(
                new IntToken(),
                new IdentifierToken("variableName"),
                new AssignmentToken(),
                new IntLiteralToken("6"),
                new SemicolonToken()
        );

        final Parser parser = new Parser(toSourcedList(input));
        assertEquals(Optional.of(new ParseResult<VardecStmt>(new VardecStmt(
                                    new IntType(),
                                    new Variable("variableName"),
                                    new IntLiteralExp(6)
                                ), 5)),
                    parser.parseVardec(0));
    }

    @Test
    public void testParseProgramWithVardec() throws ParserException {
        /*
         * int variableName = 6;
         * just a different entry point
         */
        Token[] input = toArray(
            new IntToken(),
            new IdentifierToken("variableName"),
            new AssignmentToken(),
            new IntLiteralToken("6"),
            new SemicolonToken()
        );
        final Parser parser = new Parser(toSourcedList(input));
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        statements.add(new VardecStmt(
            new IntType(),
            new Variable("variableName"),
            new IntLiteralExp(6)
            )
        );

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 5),
                    parser.parseProgram(0));
    }

    @Test
    public void testParseIfNoElseStatement() {
        // if (3)
        //    x = false;
        Token[] input = toArray(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken(),
                new IdentifierToken("x"), new AssignmentToken(), new FalseToken(), new SemicolonToken());

        AssignStmt ifBody = new AssignStmt(new Variable("x"), new BoolLiteralExp(false));
        IfElseStmt ifElseStmt = new IfElseStmt(new IntLiteralExp(3), ifBody);

        testStatementMatchesExpected(ifElseStmt, input);
    }

    @Test
    public void testParseIfElseStatement() {
        // if (3)
        //    x = false;
        // else
        //    x = true;
        Token[] input = toArray(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken(),
                new IdentifierToken("x"), new AssignmentToken(), new FalseToken(), new SemicolonToken(), new ElseToken(),
                new IdentifierToken("x"), new AssignmentToken(), new TrueToken(), new SemicolonToken());


        AssignStmt ifBody = new AssignStmt(new Variable("x"), new BoolLiteralExp(false));
        AssignStmt elseBody = new AssignStmt(new Variable("x"), new BoolLiteralExp(true));
        IfElseStmt ifElseStmt = new IfElseStmt(new IntLiteralExp(3), ifBody, elseBody);

        testStatementMatchesExpected(ifElseStmt, input);
    }

    @Test
    public void testWhileStatement() {
        // while (true) {}
        Token[] input = toArray(new WhileToken(), new LeftParenToken(), new TrueToken(), new RightParenToken(),
                new LeftBraceToken(), new RightBraceToken());

        WhileStmt whileStmt = new WhileStmt(new BoolLiteralExp(true), new StmtBlock());
        testStatementMatchesExpected(whileStmt, input);
    }

    @Test
    public void testBreakStatement() {
        // break;
        testStatementMatchesExpected(new BreakStmt(), new BreakToken(), new SemicolonToken());
    }

    @Test
    public void testPrintlnStatement() {
        // println(null);
        Token[] input = toArray(new PrintlnToken(), new LeftParenToken(), new NullToken(), new RightParenToken(),
                new SemicolonToken());

        PrintlnStmt printlnStmt = new PrintlnStmt(new NullExp());
        testStatementMatchesExpected(printlnStmt, input);
    }

    @Test
    public void testReturnStatementWithoutReturnValue() {
        // return;
        testStatementMatchesExpected(new ReturnStmt(), new ReturnToken(), new SemicolonToken());
    }

    @Test
    public void testReturnStatementWithReturnValue() {
        // return false;
        ReturnStmt returnStmt = new ReturnStmt(new BoolLiteralExp(false));
        testStatementMatchesExpected(returnStmt, new ReturnToken(), new FalseToken(), new SemicolonToken());
    }

    @Test
    public void testParseEmptyStatementBlock() {
        // {}
        Token[] input = toArray(new LeftBraceToken(), new RightBraceToken());

        StmtBlock stmtBlock = new StmtBlock(List.of());
        testStatementMatchesExpected(stmtBlock, input);
    }

    @Test
    public void testParseNonEmptyStatementBlock() {
        // { int variableName = 6; }
        Token[] input = toArray(new LeftBraceToken(),
                new IntToken(),
                new IdentifierToken("variableName"),
                new AssignmentToken(),
                new IntLiteralToken("6"),
                new SemicolonToken(),
                new RightBraceToken());

        StmtBlock stmtBlock = new StmtBlock(List.of(
                new VardecStmt(new IntType(), new Variable("variableName"), new IntLiteralExp(6)))
        );
        testStatementMatchesExpected(stmtBlock, input);
    }

    @Test
    public void testExpressionStatement() {
        // 3;
        ExpressionStmt expressionStmt = new ExpressionStmt(new IntLiteralExp(3));
        testStatementMatchesExpected(expressionStmt, new IntLiteralToken("3"), new SemicolonToken());
    }

    @Test
    public void testParseEqualityStatement() {
        // isTrue = (count == 6);
        Token[] input = toArray(
                new IdentifierToken("isTrue"), new AssignmentToken(), new LeftParenToken(),
                new IdentifierToken("count"), new DoubleEqualsToken(), new IntLiteralToken("6"),
                new RightParenToken(), new SemicolonToken()
        );

        Expression intLiteral6 = new IntLiteralExp(6);
        Expression varCount = new VariableExp(new Variable("count"));
        Expression binOpDoubleEquals = new BinaryOpExp(varCount, OperatorEnum.DOUBLE_EQUALS, intLiteral6);
        Expression parenExp = new ParenExp(binOpDoubleEquals);
        Variable varIsTrue = new Variable("isTrue");
        AssignStmt assign = new AssignStmt(varIsTrue, parenExp);

        testStatementMatchesExpected(assign, input);
    }

    @Test
    public void testParseProgramWithStructDef() throws ParserException {
        /*
         * struct Node {
         *    int value;
         *    Node rest;
         * }
         */
        final List<Sourced<Token>> input = toSourcedList(
                new StructToken(),
                new IdentifierToken("Node"),
                new LeftBraceToken(),
                new IntToken(),
                new IdentifierToken("value"),
                new SemicolonToken(),
                new IdentifierToken("Node"),
                new IdentifierToken("rest"),
                new SemicolonToken(),
                new RightBraceToken()
        );
        final Parser parser = new Parser(input);

        final List<Param> params = new ArrayList<>();

        params.add(new Param(
            new IntType(),
            new Variable("value")
        ));

        params.add(new Param(
            new StructType(new StructName("Node")),
            new Variable("rest")
        ));

        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<StructDef> structDefs = new ArrayList<>();

        structDefs.add(new StructDef(
                new StructName("Node"),
                params
            )
        );

        final List<Statement> statements = new ArrayList<>();

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 10),
                parser.parseProgram(0));
    }

    @Test
    public void testProgramWithDotOpExpression() throws ParserException {
        // retval = example.result;
        final List<Sourced<Token>> input = toSourcedList(
                new IdentifierToken("retval"),
                new AssignmentToken(),
                new IdentifierToken("example"),
                new DotToken(),
                new IdentifierToken("result"),
                new SemicolonToken()
        );
        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        DotExp dotExp = new DotExp(new VariableExp(new Variable("example")), new Variable("result"));

        statements.add(new AssignStmt(
                new Variable("retval"),
                dotExp));

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 6),
                parser.parseProgram(0));
    }

    @Test
    public void testProgramWithSequentialDotOpExpression() throws ParserException {
        // retval = example.result.value.rest.next;
        final List<Sourced<Token>> input = toSourcedList(
                new IdentifierToken("retval"),
                new AssignmentToken(),
                new IdentifierToken("example"),
                new DotToken(),
                new IdentifierToken("result"),
                new DotToken(),
                new IdentifierToken("value"),
                new DotToken(),
                new IdentifierToken("rest"),
                new DotToken(),
                new IdentifierToken("next"),
                new SemicolonToken()
        );
        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        DotExp dotExp0 = new DotExp(new VariableExp(new Variable("example")), new Variable("result"));
        DotExp dotExp1 = new DotExp(dotExp0, new Variable("value"));
        DotExp dotExp2 = new DotExp(dotExp1, new Variable("rest"));
        DotExp dotExp3 = new DotExp(dotExp2, new Variable("next"));

        statements.add(new AssignStmt(
                new Variable("retval"),
                dotExp3));

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 12),
                parser.parseProgram(0));
    }

    @Test
    public void testProgramWithMultOpExpression() throws ParserException {
        // retval = retval * 2;
        final List<Sourced<Token>> input = toSourcedList(
                new IdentifierToken("retval"),
                new AssignmentToken(),
                new IdentifierToken("retval"),
                new MultiplyToken(),
                new IntLiteralToken("2"),
                new SemicolonToken()
        );
        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        Expression leftExp = new VariableExp(new Variable("retval"));
        Expression rightExp = new IntLiteralExp(2);

        BinaryOpExp multExp = new BinaryOpExp(leftExp, OperatorEnum.MULTIPLY, rightExp);

        statements.add(new AssignStmt(
                new Variable("retval"),
                multExp)
        );

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 6),
                parser.parseProgram(0));
    }

    private void testCompareExpWithOperator(OperatorEnum operator, Token tokenOperator) {
        // 3 <op> 4;
        Token[] input = toArray(new IntLiteralToken("3"), tokenOperator, new IntLiteralToken("4"), new SemicolonToken());

        BinaryOpExp binaryOpExp = new BinaryOpExp(new IntLiteralExp(3), operator, new IntLiteralExp(4));
        ExpressionStmt expressionStmt = new ExpressionStmt(binaryOpExp);

        testStatementMatchesExpected(expressionStmt, input);
    }

    @Test
    public void testCompareExpWithLessThanEquals() {
        testCompareExpWithOperator(OperatorEnum.LESS_THAN_EQUALS, new LessThanEqualsToken());
    }

    @Test
    public void testCompareExpWithGreaterThanEquals() {
        testCompareExpWithOperator(OperatorEnum.GREATER_THAN_EQUALS, new GreaterThanEqualsToken());
    }

    @Test
    public void testCompareExpWithLessThan() {
        testCompareExpWithOperator(OperatorEnum.LESS_THAN, new LessThanToken());
    }

    @Test
    public void testCompareExpWithGreaterThan() {
        testCompareExpWithOperator(OperatorEnum.GREATER_THAN, new GreaterThanToken());
    }

    @Test
    public void testParseStatementWithDifferentOperatorPrecedence() {
        /*
         * bool result = (!isTrue && (4 + 3 * 7 >= count - otherCount.value / 5)) || false;
         */

        Token[] input = toArray(
            new BoolToken(), new IdentifierToken("result"), new AssignmentToken(),
            new LeftParenToken(), new NotToken(), new IdentifierToken("isTrue"), new AndToken(), new LeftParenToken(),
            new IntLiteralToken("4"), new PlusToken(), new IntLiteralToken("3"), new MultiplyToken(), new IntLiteralToken("7"),
            new GreaterThanEqualsToken(), new IdentifierToken("count"), new MinusToken(), new IdentifierToken("otherCount"),
            new DotToken(), new IdentifierToken("value"), new DivisionToken(), new IntLiteralToken("5"), new RightParenToken(),
            new RightParenToken(), new OrToken(), new FalseToken(), new SemicolonToken()
        );

        Expression varExpOtherCount = new VariableExp(new Variable("otherCount"));
        Variable varValue = new Variable("value");
        DotExp dotExp = new DotExp(varExpOtherCount, varValue);
        Expression intLiteral5 = new IntLiteralExp(5);
        Expression binOpDivide = new BinaryOpExp(dotExp, OperatorEnum.DIVISION, intLiteral5);
        Expression varExpCount = new VariableExp(new Variable("count"));
        Expression binOpMinus = new BinaryOpExp(varExpCount, OperatorEnum.MINUS, binOpDivide);

        Expression intLiteral3 = new IntLiteralExp(3);
        Expression intLiteral7 = new IntLiteralExp(7);
        Expression binOpMult = new BinaryOpExp(intLiteral3, OperatorEnum.MULTIPLY, intLiteral7);
        Expression intLiteral4 = new IntLiteralExp(4);
        Expression binOpAdd = new BinaryOpExp(intLiteral4, OperatorEnum.PLUS, binOpMult);

        Expression binOpGte = new BinaryOpExp(binOpAdd, OperatorEnum.GREATER_THAN_EQUALS, binOpMinus);
        Expression parenGteExp = new ParenExp(binOpGte);
        Expression varIsTrue = new VariableExp(new Variable("isTrue"));
        Expression notOpExp = new UnaryOpExp(OperatorEnum.NOT, varIsTrue);

        Expression andExp = new BinaryOpExp(notOpExp, OperatorEnum.AND, parenGteExp);
        Expression falseExp = new BoolLiteralExp(false);
        Expression parenAndExp = new ParenExp(andExp);
        Expression orExp = new BinaryOpExp(parenAndExp, OperatorEnum.OR, falseExp);

        Statement statement = new VardecStmt(new BoolType(), new Variable("result"), orExp);

        testStatementMatchesExpected(statement, input);
    }

    @Test
    public void testStructDefWithNoParams() {
        // struct A {}
        Token[] input = toArray(new StructToken(), new IdentifierToken("a"), new LeftBraceToken(), new RightBraceToken());
        StructDef structDef = new StructDef(new StructName("a"), List.of());
        Program program = new Program(List.of(structDef), List.of(), List.of());

        testProgramMatchesExpectedResult(program, input);
    }

    @Test
    public void testStructAllocationWithNoParams() {
        // new A {};
        Token[] input = toArray(new NewToken(), new IdentifierToken("A"), new LeftBraceToken(), new RightBraceToken(),
                new SemicolonToken());

        StructAllocExp structAllocExp = new StructAllocExp(new StructType(new StructName("A")),
                new StructActualParams(List.of()));
        ExpressionStmt expressionStmt = new ExpressionStmt(structAllocExp);

        testStatementMatchesExpected(expressionStmt, input);
    }

    @Test
    public void testParseProgramWithStructAllocation() throws ParserException {
        /*
         * Node list =
         *   new Node {
         *     value: 0,
         *     rest: new Node {
         *       value: 1,
         *       rest: new Node {
         *         value: 2,
         *         rest: null
         *       }
         *     }
         *   };
         */

        final List<Sourced<Token>> input = toSourcedList(
                new IdentifierToken("Node"), new IdentifierToken("list"), new AssignmentToken(),
                new NewToken(), new IdentifierToken("Node"), new LeftBraceToken(),
                new IdentifierToken("value"), new ColonToken(), new IntLiteralToken("0"),
                new CommaToken(), new IdentifierToken("rest"), new ColonToken(), new NewToken(),
                new IdentifierToken("Node"), new LeftBraceToken(), new IdentifierToken("value"),
                new ColonToken(), new IntLiteralToken("1"), new CommaToken(), new IdentifierToken("rest"),
                new ColonToken(), new NewToken(), new IdentifierToken("Node"), new LeftBraceToken(),
                new IdentifierToken("value"), new ColonToken(), new IntLiteralToken("2"),
                new CommaToken(), new IdentifierToken("rest"), new ColonToken(), new NullToken(),
                new RightBraceToken(), new RightBraceToken(), new RightBraceToken(),
                new SemicolonToken()
        );
        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        StructActualParam paramRest3 = new StructActualParam(new Variable("rest"), new NullExp());
        StructActualParam paramValue3 = new StructActualParam(new Variable("value"), new IntLiteralExp(2));
        List<StructActualParam> paramList3 = Arrays.asList(
                paramValue3,
                paramRest3);
        StructActualParams structActParams3 = new StructActualParams(paramList3);
        StructType structType3 = new StructType(new StructName("Node"));
        StructAllocExp structAlloc3 = new StructAllocExp(structType3, structActParams3);

        StructActualParam paramRest2 = new StructActualParam(new Variable("rest"), structAlloc3);
        StructActualParam paramValue2 = new StructActualParam(new Variable("value"), new IntLiteralExp(1));
        List<StructActualParam> paramList2 = Arrays.asList(
                paramValue2,
                paramRest2);
        StructActualParams structActParams2 = new StructActualParams(paramList2);
        StructType structType2 = new StructType(new StructName("Node"));
        StructAllocExp structAlloc2 = new StructAllocExp(structType2, structActParams2);

        StructActualParam paramRest1 = new StructActualParam(new Variable("rest"), structAlloc2);
        StructActualParam paramValue1 = new StructActualParam(new Variable("value"), new IntLiteralExp(0));
        List<StructActualParam> paramList1 = Arrays.asList(
            paramValue1,
            paramRest1
        );
        StructActualParams structActParams1 = new StructActualParams(paramList1);
        StructType structType1 = new StructType(new StructName("Node"));
        Expression expStructAlloc1 = new StructAllocExp(structType1, structActParams1);

        StructType structType0 = new StructType(new StructName("Node"));
        Statement vardecStmt = new VardecStmt(structType0, new Variable("list"), expStructAlloc1);

        statements.add(vardecStmt);

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 35),
                parser.parseProgram(0));
    }

    @Test
    public void testParseProgramWithFunctionCall() throws ParserException {
        /*
         * println(length(list));
         */

        final List<Sourced<Token>> input = toSourcedList(
                new PrintlnToken(), new LeftParenToken(), new IdentifierToken("length"),
                new LeftParenToken(), new IdentifierToken("list"), new RightParenToken(),
                new RightParenToken(), new SemicolonToken()
        );

        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        VariableExp variableList = new VariableExp(new Variable("list"));
        CommaExp commaExp = new CommaExp(Arrays.asList(variableList));
        FunctionName funcNameLength = new FunctionName("length"); 
        FuncCallExp funcCallExp = new FuncCallExp(funcNameLength, commaExp);
        PrintlnStmt printlnStmt = new PrintlnStmt(funcCallExp);

        statements.add(printlnStmt);

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), input.size()),
                parser.parseProgram(0));
    }


    @Test
    public void testParseProgramWithMultipleArgsFunctionCall() throws ParserException {
        /*
         * length(node.next, index, true);
         */

        final List<Sourced<Token>> input = toSourcedList(
                new IdentifierToken("length"), new LeftParenToken(), new IdentifierToken("node"),
                new DotToken(), new IdentifierToken("next"), new CommaToken(), 
                new IdentifierToken("index"), new CommaToken(), new TrueToken(), 
                new RightParenToken(), new SemicolonToken()
        );

        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();


        BoolLiteralExp boolLitTrue = new BoolLiteralExp(true);
        VariableExp varExpIndex = new VariableExp(new Variable("index"));
        DotExp dotExp = new DotExp(new VariableExp(new Variable("node")), new Variable("next"));
        List<Expression> argsList = Arrays.asList(dotExp, varExpIndex, boolLitTrue);
        CommaExp commaExp = new CommaExp(argsList);
        FunctionName funcNameLength = new FunctionName("length");
        FuncCallExp funcCallExp = new FuncCallExp(funcNameLength, commaExp);
        ExpressionStmt expStmt = new ExpressionStmt(funcCallExp);

        statements.add(expStmt);

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), input.size()),
                parser.parseProgram(0));
    }

    // Invalid tests

    // Test invalid inputs

    private void testProgramParsesWithException(Token... tokens) {
        assertThrows(ParserMalformedException.class, () -> parseProgram(tokens));
    }

    @Test
    public void testStructDefWithNoParamNameThrowsException() {
        // struct A { int; }
        testProgramParsesWithException(new StructToken(), new IdentifierToken("A"), new LeftBraceToken(),
                new IntToken(), new SemicolonToken(), new RightBraceToken());
    }

    @Test
    public void testFunctionDefWithNoFunctionNameThrowsException() {
        // func
        testProgramParsesWithException(new FuncToken());
    }

    @Test
    public void testFunctionDefWithNoLeftParenThrowsException() {
        // func a
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"));
    }

    @Test
    public void testFunctionDefWithNoParameterAfterCommaThrowsException() {
        // func a(int b, )
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new CommaToken(), new RightParenToken());
    }

    @Test
    public void testFunctionDefWithNoRightParenThrowsException() {
        // func a(int b :
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new ColonToken());
    }


    @Test
    public void testFunctionDefWithNoColonThrowsException() {
        // func a(int b) int
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new IntToken());
    }

    @Test
    public void testFunctionDefWithNoReturnTypeThrowsException() {
        // func a(int b) :
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new ColonToken());
    }

    @Test
    public void testFunctionDefWithNoFunctionBodyThrowsException() {
        // func a(int b) : int
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new ColonToken(), new IntToken());
    }

    @Test
    public void testIfWithNoLeftParenThrowsException() {
        // if 3
        testProgramParsesWithException(new IfToken(), new IntLiteralToken("3"));
    }

    @Test
    public void testIfWithNoConditionThrowsException() {
        // if ()
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new RightParenToken());
    }

    @Test
    public void testIfWithNoRightParenThrowsException() {
        // if (3
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"));
    }

    @Test
    public void testIfWithNoIfStatementBodyThrowsException() {
        // if (3)
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken());
    }

    @Test
    public void testIfWithNoElseStatementBodyThrowsException() {
        // if (3)
        //   x = false;
        // else
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken(),
                new IdentifierToken("x"), new AssignmentToken(), new FalseToken(), new SemicolonToken(), new ElseToken());

    }

    @Test
    public void testWhileWithNoLeftParenThrowsException() {
        // while
        testProgramParsesWithException(new WhileToken());
    }

    @Test
    public void testWhileWithNoConditionThrowsException() {
        // while (
        testProgramParsesWithException(new WhileToken(), new LeftParenToken());
    }

    @Test
    public void testWhileWithNoRightParenThrowsException() {
        // while (true
        testProgramParsesWithException(new WhileToken(), new LeftParenToken(), new TrueToken());
    }

    @Test
    public void testWhileWithNoBodyThrowsException() {
        // while (true)
        testProgramParsesWithException(new WhileToken(), new LeftParenToken(), new TrueToken(), new RightParenToken());
    }

    @Test
    public void testBreakWithNoSemicolonThrowsException() {
        // break
        testProgramParsesWithException(new BreakToken());
    }

    @Test
    public void testPrintlnWithNoLeftParenThrowsException() {
        // println
        testProgramParsesWithException(new PrintlnToken());
    }

    @Test
    public void testPrintlnWithNoExpressionThrowsException() {
        // println(
        testProgramParsesWithException(new PrintlnToken(), new LeftParenToken());
    }

    @Test
    public void testPrintlnWithNoRightParenThrowsException() {
        // println(x
        testProgramParsesWithException(new PrintlnToken(), new LeftParenToken(), new IdentifierToken("x"));
    }

    @Test
    public void testPrintlnWithNoSemicolonThrowsException() {
        // println(x)
        testProgramParsesWithException(new PrintlnToken(), new LeftParenToken(), new IdentifierToken("x"),
                new RightParenToken());
    }

    @Test
    public void testReturnNoSemicolonThrowsException() {
        // return
        testProgramParsesWithException(new ReturnToken());
    }

    @Test
    public void testReturnWithReturnValueNoSemicolonThrowsException() {
        // return 2
        testProgramParsesWithException(new ReturnToken(), new IntLiteralToken("2"));
    }

    @Test
    public void testStatementBlockWithoutClosingBraceThrowsException() {
        // {
        testProgramParsesWithException(new LeftBraceToken());
    }

    @Test
    public void testExpressionStatementWithoutSemicolonThrowsException() {
        // 9
        testProgramParsesWithException(new IntLiteralToken("9"));
    }

    @Test
    public void testExtraTokenThrowsException() {
        // var = newVar; int
        testProgramParsesWithException(
            new IdentifierToken("var"), new AssignmentToken(), new IdentifierToken("newVar"),
            new SemicolonToken(), new IntToken()
        );
    }

    @Test
    public void testNoStructNameThrowsException() {
        // struct = {};
        testProgramParsesWithException(
            new StructToken(), new AssignmentToken(), new LeftBraceToken(),
            new RightBraceToken(), new SemicolonToken()
        );
    }

    @Test
    public void testStructAllocationWithIntTokenInsteadOfStructNameThrowsException() {
        // new int {};
        testProgramParsesWithException(new NewToken(), new IntToken(), new LeftBraceToken(), new RightBraceToken(),
                new SemicolonToken());
    }

    @Test
    public void testNoVardecVariableNameThrowsException() {
        // int = 6;
        testProgramParsesWithException(
                new IntToken(), new AssignmentToken(), new IntLiteralToken("6"), new SemicolonToken());
    }

    @Test // For coverage
    public void testDifferentParseResultDoesNotEquals() {
        assertNotEquals(
            new ParseResult<StructDef>(
                new StructDef(
                    new StructName("struct1"),
                    new ArrayList<Param>() {{
                        add(new Param(new IntType(), new Variable("var")));
                    }}
                ), 7
            ),
            new ParseResult<StructDef>(
                new StructDef(
                    new StructName("struct2"),
                    new ArrayList<>() {{
                        add(new Param(new IntType(), new Variable("var")));
                    }}
                ), 7
            )
        );
    }

    // Source tests

    @Test
    public void testSourcedTokens() {
        // Kill me please - this test should be very sufficient for all cases

        String input = """
                func foo(int a): int {
                  return a + 2;
                }""";

        // func foo(int a): int {
        Source funcSource = new Source("func", new SourcePosition(1, 1), new SourcePosition(1, 5));
        Sourced<Token> funcSourcedToken = new Sourced<>(funcSource, new FuncToken());

        Source fooSource = new Source("foo", new SourcePosition(1, 6), new SourcePosition(1, 9));
        Sourced<Token> fooSourcedToken = new Sourced<>(fooSource, new IdentifierToken("foo"));

        Source leftParenSource = new Source("(", new SourcePosition(1, 9), new SourcePosition(1, 10));
        Sourced<Token> leftParenSourcedToken = new Sourced<>(leftParenSource, new LeftParenToken());

        Source intParamSource = new Source("int", new SourcePosition(1, 10), new SourcePosition(1, 13));
        Sourced<Token> intParamSourcedToken = new Sourced<>(intParamSource, new IntToken());

        Source aParamSource = new Source("a", new SourcePosition(1, 14), new SourcePosition(1, 15));
        Sourced<Token> aParamSourcedToken = new Sourced<>(aParamSource, new IdentifierToken("a"));

        Source rightParenSource = new Source(")", new SourcePosition(1, 15), new SourcePosition(1, 16));
        Sourced<Token> rightParenSourcedToken = new Sourced<>(rightParenSource, new RightParenToken());

        Source colonSource = new Source(":", new SourcePosition(1, 16), new SourcePosition(1, 17));
        Sourced<Token> colonSourcedToken = new Sourced<>(colonSource, new ColonToken());

        Source intReturnSource = new Source("int", new SourcePosition(1, 18), new SourcePosition(1, 21));
        Sourced<Token> intReturnSourcedToken = new Sourced<>(intReturnSource, new IntToken());

        Source leftBraceSource = new Source("{", new SourcePosition(1, 22), new SourcePosition(1, 23));
        Sourced<Token> leftBraceSourcedToken = new Sourced<>(leftBraceSource, new LeftBraceToken());

        // <space><space>return a + 2;
        Source returnSource = new Source("return", new SourcePosition(2, 3), new SourcePosition(2, 9));
        Sourced<Token> returnSourcedToken = new Sourced<>(returnSource, new ReturnToken());

        Source aSource = new Source("a", new SourcePosition(2, 10), new SourcePosition(2, 11));
        Sourced<Token> aSourcedToken = new Sourced<>(aSource, new IdentifierToken("a"));

        Source plusSource = new Source("+", new SourcePosition(2, 12), new SourcePosition(2, 13));
        Sourced<Token> plusSourcedToken = new Sourced<>(plusSource, new PlusToken());

        Source twoSource = new Source("2", new SourcePosition(2, 14), new SourcePosition(2, 15));
        Sourced<Token> twoSourcedToken = new Sourced<>(twoSource, new IntLiteralToken("2"));

        Source semicolonSource = new Source(";", new SourcePosition(2, 15), new SourcePosition(2, 16));
        Sourced<Token> semicolonSourcedToken = new Sourced<>(semicolonSource, new SemicolonToken());

        // }
        Source rightBraceSource = new Source("}", new SourcePosition(3, 1), new SourcePosition(3, 2));
        Sourced<Token> rightBraceSourcedToken = new Sourced<>(rightBraceSource, new RightBraceToken());

        List<Sourced<Token>> sourcedTokens = List.of(funcSourcedToken, fooSourcedToken, leftParenSourcedToken,
                intParamSourcedToken, aParamSourcedToken, rightParenSourcedToken, colonSourcedToken,
                intReturnSourcedToken, leftBraceSourcedToken, returnSourcedToken, aSourcedToken, plusSourcedToken,
                twoSourcedToken, semicolonSourcedToken, rightBraceSourcedToken);

        // Start defining all the required function definition parameters with correct sourcing

        FunctionName functionName = new FunctionName("foo");
        functionName.setSource(fooSource);

        IntType paramType = new IntType();
        paramType.setSource(intParamSource);

        Variable aParamVariable = new Variable("a");
        aParamVariable.setSource(aParamSource);

        // int a
        Param aParam = new Param(paramType, aParamVariable);
        aParam.setSource(new Source("int a", intParamSource.getStartPosition(),
                aParamVariable.getSource().getEndPosition()));

        IntType returnType = new IntType();
        returnType.setSource(intReturnSource);

        Variable aBodyVariable = new Variable("a");
        aBodyVariable.setSource(aSource);

        VariableExp aBodyVariableExp = new VariableExp(aBodyVariable);
        aBodyVariableExp.setSource(aSource);

        IntLiteralExp twoExp = new IntLiteralExp(2);
        twoExp.setSource(twoSource);

        BinaryOpExp addExp = new BinaryOpExp(aBodyVariableExp, OperatorEnum.PLUS, twoExp);
        addExp.setSource(new Source("a + 2", aSource.getStartPosition(), twoSource.getEndPosition()));

        ReturnStmt returnStmt = new ReturnStmt(addExp);
        returnStmt.setSource(new Source("return a + 2;", returnSource.getStartPosition(),
                semicolonSource.getEndPosition()));

        StmtBlock body = new StmtBlock(List.of(returnStmt));
        body.setSource(new Source("{\n  return a + 2;\n}", leftBraceSource.getStartPosition(),
                rightBraceSource.getEndPosition()));

        FunctionDef functionDef = new FunctionDef(functionName, List.of(aParam), returnType, body);
        functionDef.setSource(new Source(input, funcSource.getStartPosition(), rightBraceSource.getEndPosition()));

        Program program = new Program(List.of(), List.of(functionDef), List.of());
        program.setSource(functionDef.getSource());

        testProgramMatchesExpectedResult(program, sourcedTokens);
    }


    // Integration test

    @Test
    public void testTokenizeParseProgramWithoutException() {
        String input = ResourceUtil.readProgramInputFile();
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Parser.parseProgram(sourcedTokens);
        } catch (TokenizerException | ParserException ex) {
            Assert.fail(ex.toString());
        }
    }

}
