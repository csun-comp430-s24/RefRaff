package refraff.parser;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.AbstractMap.SimpleImmutableEntry;

import refraff.Source;
import refraff.Sourced;
import refraff.parser.function.*;
import refraff.parser.statement.*;
import refraff.parser.struct.*;
import refraff.parser.type.*;
import refraff.parser.operator.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.expression.*;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.reserved.*;
import refraff.tokenizer.*;

public class Parser {
    
    public final List<Sourced<Token>> sourcedTokens;

    public Parser(final List<Sourced<Token>> sourcedTokens) {
        this.sourcedTokens = sourcedTokens;
    }

    // Returns an optional token or emtpy if we've reached the end of tokens
    public Optional<Token> getToken(final int position) {
        // If we somehow go below position 0, we did something REAL bad in the parser
        assert(position >= 0);

        if (position < sourcedTokens.size()) {
            return Optional.of(sourcedTokens.get(position).getValue());
        } else {
            return Optional.empty();
        }
    }

    // Returns true if there are more tokens and they are the same, otherwise empty
    // Refactor: change expected to the token's class - no longer creating new tokens
    public boolean isExpectedToken(final int position, final Class<? extends Token> expected) {
        final Optional<Token> received = getToken(position);
        return received.isPresent() && received.get().getClass().equals(expected);
    }

    private void throwParserExceptionOnNoSemicolon(String beingParsed, int position) throws ParserException {
        throwParserExceptionOnUnexpected(beingParsed, SemicolonToken.class, "terminating `;`", position);
    }

    private void throwParserExceptionOnUnexpected(String beingParsed, Class<? extends Token> tokenClass,
                                                  String tokenRepresentation, int position) throws ParserException {
        if (isExpectedToken(position, tokenClass)) {
            return;
        }

        throwParserException(beingParsed, tokenRepresentation, position);
    }

    private <T> void throwParserExceptionOnEmptyOptional(String beingParsed, Optional<T> optional,
                                                         String expected, int position) throws ParserException {
        if (optional.isPresent()) {
            return;
        }

        throwParserException(beingParsed, expected, position);
    }

    private void throwParserException(String beingParsed, String expected, int position) throws ParserException {
        final String exceptionMessage = "Error parsing %s at %s: expected %s but received: %s";
        String actualTokenValue = getToken(position).map(Token::toString).orElse("none");
        String linePosition;
        if (position >= sourcedTokens.size()) {
            linePosition = "end of file";
        } else {
            linePosition = sourcedTokens.get(position).getSource().toPositionString();
        }

        String formattedExceptionMessage = String.format(exceptionMessage, beingParsed, linePosition, expected,
                actualTokenValue);
        throw new ParserMalformedException(formattedExceptionMessage);
    }

    // Attempts to parse token array
    public static Program parseProgram(List<Sourced<Token>> tokens) throws ParserException {
        final Parser parser = new Parser(tokens);
        final ParseResult<Program> program = parser.parseProgram(0);
        return program.result;

//        If we have more tokens remaining, we will either: parse something successfully, or throw an error
//        if (program.nextPosition == tokens.length) {
//            return program.result;
//        } else {
//            throw new ParserException("Remaining tokens at end, starting with: " +
//                                      parser.getToken(program.nextPosition).toString());
//        }
    }

    // program ::= structdef* fdef* stmt*
    public ParseResult<Program> parseProgram(final int position) throws ParserException {
        int currentPosition = position;
        List<StructDef> structDefs = new ArrayList<>();
        List<FunctionDef> functionDefs = new ArrayList<>();
        List<Statement> statements = new ArrayList<>();

        currentPosition = parseZeroOrMore(this::parseStructDef, structDefs::add, currentPosition);
        currentPosition = parseZeroOrMore(this::parseFunctionDef, functionDefs::add, currentPosition);
        currentPosition = parseZeroOrMore(this::parseStatement, statements::add, currentPosition);
        
        return getSourcedParseResult(new Program(structDefs, functionDefs, statements), position, currentPosition);
    }

    /**
     * Parses zero or more of type T. Returns the next position for the parser.
     *
     * @param parseFunction the parsing function to parse a T, starting at the specified position
     * @param parsedValueConsumer what to do with T, if one is parsed
     * @param position the initial parsing position
     * @return the next position for the parser to go to
     * @param <T> a type that can be parsed
     */
    private <T> int parseZeroOrMore(ParsingFunction<Integer, Optional<ParseResult<T>>> parseFunction,
                                    Consumer<T> parsedValueConsumer, int position) throws ParserException {
        int currentPosition = position;

        while (true) {
            Optional<ParseResult<T>> optionalParseResult;

            try {
                optionalParseResult = parseFunction.apply(currentPosition);
            } catch (ParserMalformedException ex) {
                // Rethrow a malformed parse
                throw ex;
            } catch (ParserNoElementFoundException ex) {
                // If we didn't find an element, this is completely okay - we are parsing zero or more
                break;
            }

            if (optionalParseResult.isEmpty()) {
                break;
            }

            ParseResult<T> parseResult = optionalParseResult.get();

            parsedValueConsumer.accept(parseResult.result);
            currentPosition = parseResult.nextPosition;
        }

        return currentPosition;
    }

    // structdef ::= `struct` structname `{` (param `;`)* `}`
    public Optional<ParseResult<StructDef>> parseStructDef(final int position) throws ParserException {
        final String structDefinition = "struct definition";
        int currentPosition = position;

        // Check that there's a struct token here
        if (!isExpectedToken(currentPosition, StructToken.class)) {
            return Optional.empty();
        }

        // We are absolutely trying to parse a struct definition now, we should throw exceptions
        currentPosition += 1;

        // Try to parse the struct name
        Optional<ParseResult<Type>> opStructName = parseType(currentPosition);
        if (opStructName.isEmpty() || !(opStructName.get().result instanceof StructType)) {
            throwParserException(structDefinition, "struct name", currentPosition);
        }

        ParseResult<Type> parsedTypeResult = opStructName.get();
        StructName structName = ((StructType) parsedTypeResult.result).getStructName()
                .orElseThrow(() -> new IllegalStateException("Name of struct is somehow not defined from valid identifier."));

        currentPosition = parsedTypeResult.nextPosition;

        // Ensure that there's a left brace here
        throwParserExceptionOnUnexpected(structDefinition, LeftBraceToken.class, "{", currentPosition);
        currentPosition += 1;

        List<Param> params = new ArrayList<>();

        // Check for (param `;`)*
        while (true) {
            // Get the param
            Optional<ParseResult<Param>> paramResult = parseParam(currentPosition);
            if (paramResult.isEmpty())
                break; // Exit if no more statements

            ParseResult<Param> param = paramResult.get();
            currentPosition = param.nextPosition;

            // Ensure that if we do have a param, we have a semicolon here
            throwParserExceptionOnNoSemicolon(structDefinition + " variable declaration", currentPosition);
            currentPosition += 1;

            params.add(param.result);
        }

        // Ensure that there's a right brace here
        throwParserExceptionOnUnexpected(structDefinition, RightBraceToken.class, "}", currentPosition);
        currentPosition += 1;

        // Return the variable declaration
        return getOptionalSourcedParseResult(
                new StructDef(structName, params),
                position,
                currentPosition
        );
    }

    // param :: = type var
    public Optional<ParseResult<Param>> parseParam(final int position) throws ParserException {
        int currentPosition = position;

        // Try to parse the type
        Optional<ParseResult<Type>> optionalType = parseType(currentPosition);
        if (optionalType.isEmpty()) {
            return Optional.empty();
        }

        ParseResult<Type> type = optionalType.get();
        currentPosition = type.nextPosition;

        // Try to parse the variable
        Optional<ParseResult<Variable>> optionalVar = parseVar(currentPosition);
        if (optionalVar.isEmpty()) {
            return Optional.empty();
        }

        ParseResult<Variable> var = optionalVar.get();
        currentPosition = var.nextPosition;

        // Return the variable declaration
        return getOptionalSourcedParseResult(
                new Param(type.result, var.result),
                position,
                currentPosition
        );
    }

    // fdef ::= `func` funcname `(` comma_param `)` `:` type
    //         `{` stmt* `}`
    public Optional<ParseResult<FunctionDef>> parseFunctionDef(final int position) throws ParserException {
        if (!isExpectedToken(position, FuncToken.class)) {
            return Optional.empty();
        }

        int currentPosition = position + 1;

        // We are definitely parsing a function definition now, we must throw moving forward
        final String functionDefinition = "function definition";

        // Throw an exception if no function name identifier
        throwParserExceptionOnUnexpected(functionDefinition, IdentifierToken.class, "a function name", currentPosition);
        FunctionName functionName = getSourcedNode(currentPosition, (token) -> new FunctionName(token.getTokenizedValue()));

        currentPosition += 1;

        // Create the function name, update error message for this function
        final String functionDefinitionWithName = functionDefinition + " for function " + functionName.functionName;

        // Ensure we have a left paren (begin function params)
        throwParserExceptionOnUnexpected(functionDefinitionWithName, LeftParenToken.class, "(", currentPosition);
        currentPosition += 1;

        // Ensure we have the comma params (the function params)
        ParseResult<List<Param>> parsedFunctionParams = parseCommaParam(functionDefinitionWithName, currentPosition);
        List<Param> functionParams = parsedFunctionParams.result;
        currentPosition = parsedFunctionParams.nextPosition;

        // Ensure we have a right paren (end function params)
        throwParserExceptionOnUnexpected(functionDefinitionWithName, RightParenToken.class, ")", currentPosition);
        currentPosition += 1;

        // Ensure we have the colon separator for the return type of the function
        throwParserExceptionOnUnexpected(functionDefinitionWithName, ColonToken.class, ":", currentPosition);
        currentPosition += 1;

        // Ensure we have a valid return type
        Optional<ParseResult<Type>> optionalReturnType = parseType(currentPosition);
        throwParserExceptionOnEmptyOptional(functionDefinitionWithName, optionalReturnType, "a valid return type",
                currentPosition);

        ParseResult<Type> parsedTypeResult = optionalReturnType.get();

        Type functionReturnType = parsedTypeResult.result;
        currentPosition = parsedTypeResult.nextPosition;

        // Ensure we have a statement block at the end
        Optional<ParseResult<StmtBlock>> optionalStatementBlock = parseStatementBlock(currentPosition);
        throwParserExceptionOnEmptyOptional(functionDefinitionWithName, optionalStatementBlock, "a function body",
                currentPosition);

        ParseResult<StmtBlock> parsedStatementBlock = optionalStatementBlock.get();
        StmtBlock functionBody = parsedStatementBlock.result;
        currentPosition = parsedStatementBlock.nextPosition;

        FunctionDef functionDef = new FunctionDef(functionName, functionParams, functionReturnType, functionBody);
        return getOptionalSourcedParseResult(functionDef, position, currentPosition);
    }

    // comma_param ::= [param (`,` param)*]
    private ParseResult<List<Param>> parseCommaParam(String functionDefinitionWithName,
                                                     final int position) throws ParserException {
        int currentPosition = position;
        List<Param> commaParams = new ArrayList<>();

        Optional<ParseResult<Param>> optionalFirstParsedParam = parseParam(currentPosition);

        // If our first param is empty, then we have no parameters
        if (optionalFirstParsedParam.isEmpty()) {
            return new ParseResult<>(List.of(), currentPosition);
        }

        ParseResult<Param> firstParamResult = optionalFirstParsedParam.get();

        commaParams.add(firstParamResult.result);
        currentPosition = firstParamResult.nextPosition;

        // While we can still parse a comma, try to grab the next parameter
        while (isExpectedToken(currentPosition, CommaToken.class)) {
            currentPosition += 1;

            Optional<ParseResult<Param>> optionalParsedParam = parseParam(currentPosition);
            throwParserExceptionOnEmptyOptional(functionDefinitionWithName, optionalParsedParam,
                    "an additional function parameter after comma", currentPosition);

            ParseResult<Param> paramResult = optionalParsedParam.get();

            commaParams.add(paramResult.result);
            currentPosition = paramResult.nextPosition;
        }

        return new ParseResult<>(commaParams, currentPosition);
    }

    // struct_actual_param ::= var `:` exp
    public Optional<ParseResult<StructActualParam>> parseStructActualParam(final int position) throws ParserException {
        int currentPosition = position;
        // Try to parse a variable
        Optional<ParseResult<Variable>> optionalVar = parseVar(currentPosition);
        if (optionalVar.isEmpty()) {
            return Optional.empty();
        }
        currentPosition = optionalVar.get().nextPosition;

        // Make sure there's a colon here - throw exceptions from this point
        throwParserExceptionOnUnexpected("struct actual param", ColonToken.class, "a colon :", currentPosition);
        currentPosition += 1;

        // parse expression
        Optional<ParseResult<Expression>> optionalExp = parseExp(currentPosition);
        throwParserExceptionOnEmptyOptional("struct actual param", optionalExp, "an expression", currentPosition);

        // Create struct actual param and return
        return getOptionalSourcedParseResult(
                new StructActualParam(optionalVar.get().result, optionalExp.get().result),
                position,
                optionalExp.get().nextPosition
        );
    }

    // struct_actual_params ::= [struct_actual_param (`,` struct_actual_param)*]
    public ParseResult<StructActualParams> parseStructActualParams(final int position) throws ParserException {
        final String structParamString = "struct actual params";
        int currentPosition = position;

        // Create a Struct Actual Param list
        List<StructActualParam> listOfActualParams = new ArrayList<>();

        // Try to parse an actual param - there doesn't have to be one
        Optional<ParseResult<StructActualParam>> optionalStructActParam = parseStructActualParam(currentPosition);
        if (optionalStructActParam.isEmpty()) {
            // If we have no params, then we don't need to source this result (nothing to source)
            return new ParseResult<>(new StructActualParams(listOfActualParams), currentPosition);
        }

        // If there is one, add it to the list
        listOfActualParams.add(optionalStructActParam.get().result);
        currentPosition = optionalStructActParam.get().nextPosition;

        // While there's a comma next
        while (isExpectedToken(currentPosition, CommaToken.class)) {
            currentPosition += 1;

            // Parse another param, add it to the param list - there has to be one now
            optionalStructActParam = parseStructActualParam(currentPosition);
            throwParserExceptionOnEmptyOptional(structParamString, optionalStructActParam,
                    "struct actual param", currentPosition);

            listOfActualParams.add(optionalStructActParam.get().result);
            currentPosition = optionalStructActParam.get().nextPosition;
        }
        
        // Create struct actual params with list and return
        return getSourcedParseResult(
                new StructActualParams(listOfActualParams),
                position,
                currentPosition
        );
    }

    public ParseResult<Statement> parseMandatoryStatement(String beingParsed,
                                                          final int position) throws ParserException {
        return parseMandatory(beingParsed, "a statement", this::parseStatement, position);
    }

    private <T> ParseResult<T> parseMandatory(String beingParsed,
                                              final ParsingFunction<Integer, Optional<ParseResult<T>>> parsingFunction,
                                              final int position) throws ParserException {
        return parseMandatory(beingParsed, beingParsed, parsingFunction, position);
    }

    private <T> ParseResult<T> parseMandatory(String beingParsed, String expected,
                                              final ParsingFunction<Integer, Optional<ParseResult<T>>> parsingFunction,
                                              final int position) throws ParserException {
        try {
            Optional<ParseResult<T>> optionalParseResult = parsingFunction.apply(position);

            if (optionalParseResult.isEmpty()) {
                throwParserException(beingParsed, beingParsed, position);
            }

            return optionalParseResult.get();
        } catch (ParserNoElementFoundException ex) {
            throwParserException(beingParsed, beingParsed, position);
        }

        throw new IllegalStateException("Cannot occur.");
    }

    /*
    stmt ::= type var `=` exp `;` | 
             var `=` exp `;` |
             `if` `(` exp `)` stmt [`else` stmt] |
             `while` `(` exp `)` stmt |
             `break` `;` |
             `println` `(` exp `)` |
             `{` stmt* `}` |
             `return` [exp] `;` |
             exp `;`
    */
    public Optional<ParseResult<Statement>> parseStatement(final int position) throws ParserException {
        // (Makes it easier to add more in the future without code repeat)
        return parseStatement(List.of(
                this::parseAssign,
                this::parseVardec,
                this::parseIfElse,
                this::parseWhile,
                this::parseBreak,
                this::parsePrintln,
                this::parseReturn,
                this::parseStatementBlock,
                this::parseExpressionStatement
        ), position);
    }

    // This method is extremely gross and horrible and I hate it - but what can you do with Java?
    private Optional<ParseResult<Statement>> parseStatement(List<ParsingFunction<Integer,
            Optional<? extends ParseResult<? extends Statement>>>> parseResults, int position) throws ParserException {
        // Iterate over all the parsing functions
        for (ParsingFunction<Integer, Optional<? extends ParseResult<? extends Statement>>> parseResult : parseResults) {
            // Try to apply the parsing function the current position
            Optional<? extends ParseResult<? extends Statement>> optionalParseResult = parseResult.apply(position);

            // If we don't parse with the current function, move to the next
            if (optionalParseResult.isEmpty()) {
                continue;
            }

            ParseResult<? extends Statement> realResult = optionalParseResult.get();

            // Downcast the parsed statement result to just being a statement (and our statement is already sourced)
            ParseResult<Statement> statementResult = new ParseResult<>(realResult.result, realResult.nextPosition);

            // Return the downcasted version
            return Optional.of(statementResult);
        }

        // If we couldn't parse any of the statements with supplied parsers, we should throw an exception
        throw new ParserNoElementFoundException("statement");
    }

    // vardec ::= type var '=' exp ';'
    public Optional<ParseResult<VardecStmt>> parseVardec(final int position) throws ParserException {
        int currentPosition = position;

        // Try to parse the type
        Optional<ParseResult<Type>> optionalType = parseType(currentPosition);
        if (optionalType.isEmpty()) {
            return Optional.empty();
        }

        // If we have a type, we must be trying to parse a vardec
        ParseResult<Type> type = optionalType.get();
        currentPosition = type.nextPosition;

        // Try to parse assignment statement, throwing if we do not have an identifier or if variable assignment fails
        // We can only throw an exception if it's an int, bool, or void
        // If we got an identifier, it may be a type or variable (which is a legal expression by itself), 
        // so we can't throw an exception on that. I may be misunderstanding something here, though.
        Optional<ParseResult<AssignStmt>> optionalAssign = parseAssign(currentPosition, type.result.shouldThrowOnAssignment());

        // We still have to check if this was a vardec
        if (optionalAssign.isEmpty()) {
            return Optional.empty();
        }
        // Safe unwrap since we know we throw if we didn't successfully parse a variable assignment
        ParseResult<AssignStmt> assign = optionalAssign.get();
        currentPosition = assign.nextPosition;

        // Return the variable declaration
        return getOptionalSourcedParseResult(
                new VardecStmt(type.result, assign.result.variable, assign.result.expression),
                position,
                currentPosition
        );
    }

    public Optional<ParseResult<AssignStmt>> parseAssign(final int position) throws ParserException {
        return parseAssign(position, false);
    }

    // assignment is var '=' exp ';'
    public Optional<ParseResult<AssignStmt>> parseAssign(final int position, final boolean shouldThrowIfNoIdentifier)
            throws ParserException {
        final String variableAssignment = "variable assignment";
        int currentPosition = position;

        // Try to parse the variable
        Optional<ParseResult<Variable>> optionalVariable = parseVar(currentPosition);
        if (optionalVariable.isEmpty()) {
            if (shouldThrowIfNoIdentifier) {
                throwParserException(variableAssignment, "a variable name", currentPosition);
            }
            return Optional.empty();
        }
        
        ParseResult<Variable> var = optionalVariable.get();
        currentPosition = var.nextPosition;

        // If there's not an assignment operator, return empty (this may be struct vardec or exp)
        if (!isExpectedToken(currentPosition, AssignmentToken.class)) {
            return Optional.empty();
        }
        currentPosition += 1;

        // Ensure that there's an expression
        Optional<ParseResult<Expression>> opExp = parseExp(currentPosition);
        throwParserExceptionOnEmptyOptional(variableAssignment, opExp, "an expression", currentPosition);

        ParseResult<Expression> exp = opExp.get();
        currentPosition = exp.nextPosition;

        // Ensure that there's a semicolon
        throwParserExceptionOnNoSemicolon(variableAssignment, currentPosition);
        currentPosition += 1;

        // Return the assignment statement
        return getOptionalSourcedParseResult(
                new AssignStmt(var.result, exp.result),
                position,
                currentPosition
        );
    }

    // `if` `(` exp `)` stmt [`else` stmt]
    private Optional<ParseResult<IfElseStmt>> parseIfElse(final int position) throws ParserException {
        if (!isExpectedToken(position, IfToken.class)) {
            return Optional.empty();
        }

        // We know we're parsing an if/else now
        final String ifStatement = "if statement";
        int currentPosition = position + 1;

        // Parse the if statement condition
        ParseResult<Expression> parsedCondition = parseExpWithParensAroundItOrThrow(ifStatement + " condition",
                currentPosition);

        Expression condition = parsedCondition.result;
        currentPosition = parsedCondition.nextPosition;

        ParseResult<Statement> parsedIfBody = parseMandatoryStatement("if statement body", currentPosition);

        Statement ifBody = parsedIfBody.result;
        currentPosition = parsedIfBody.nextPosition;

        // [`else` stmt]: if we don't hit an else, then just return the if statement so far
        if (!isExpectedToken(currentPosition, ElseToken.class)) {
            return getOptionalSourcedParseResult(new IfElseStmt(condition, ifBody), position, currentPosition);
        }

        currentPosition += 1;

        // If we did hit an else, the stmt becomes mandatory
        ParseResult<Statement> parsedElseBody = parseMandatoryStatement("else statement body", currentPosition);

        Statement elseBody = parsedElseBody.result;
        currentPosition = parsedElseBody.nextPosition;

        return getOptionalSourcedParseResult(new IfElseStmt(condition, ifBody, elseBody), position, currentPosition);
    }

    // Parses `(` exp `)` in the context of a statement: so this is NOT a paren expression
    private ParseResult<Expression> parseExpWithParensAroundItOrThrow(final String where,
                                                                      final int position) throws ParserException {
        throwParserExceptionOnUnexpected(where, LeftParenToken.class, "(", position);
        int currentPosition = position + 1;

        ParseResult<Expression> parsedExpressionResult = parseMandatoryExp(currentPosition);

        Expression expression = parsedExpressionResult.result;
        currentPosition = parsedExpressionResult.nextPosition;

        throwParserExceptionOnUnexpected(where, RightParenToken.class, ")", currentPosition);
        currentPosition += 1;

        // Expression has already been sourced by now
        return new ParseResult<>(expression, currentPosition);
    }

    // `while` `(` exp `)` stmt
    private Optional<ParseResult<WhileStmt>> parseWhile(final int position) throws ParserException {
        if (!isExpectedToken(position, WhileToken.class)) {
            return Optional.empty();
        }

        // We are now definitely parsing a while block
        final String whileStatement = "while statement";
        int currentPosition = position + 1;

        // Parse the while condition
        ParseResult<Expression> parsedConditionResult = parseExpWithParensAroundItOrThrow(whileStatement + " condition",
                currentPosition);

        Expression condition = parsedConditionResult.result;
        currentPosition = parsedConditionResult.nextPosition;

        ParseResult<Statement> parsedBodyResult = parseMandatoryStatement(whileStatement + " body", currentPosition);

        Statement body = parsedBodyResult.result;
        currentPosition = parsedBodyResult.nextPosition;

        WhileStmt whileStmt = new WhileStmt(condition, body);
        return getOptionalSourcedParseResult(whileStmt, position, currentPosition);
    }

    // `break` `;`
    private Optional<ParseResult<BreakStmt>> parseBreak(final int position) throws ParserException {
        if (!isExpectedToken(position, BreakToken.class)) {
            return Optional.empty();
        }

        // We are definitely parsing a break statement
        int currentPosition = position + 1;

        throwParserExceptionOnNoSemicolon("break statement", currentPosition);
        currentPosition += 1;

        return getOptionalSourcedParseResult(new BreakStmt(), position, currentPosition);
    }

    // `println` `(` exp `)` `;`
    private Optional<ParseResult<PrintlnStmt>> parsePrintln(final int position) throws ParserException {
        if (!isExpectedToken(position, PrintlnToken.class)) {
            return Optional.empty();
        }

        int currentPosition = position + 1;

        final String printlnStatement = "println statement";
        ParseResult<Expression> parsedPrintlnExpression = parseExpWithParensAroundItOrThrow(printlnStatement,
                currentPosition);

        Expression expression = parsedPrintlnExpression.result;
        currentPosition = parsedPrintlnExpression.nextPosition;

        throwParserExceptionOnNoSemicolon(printlnStatement, currentPosition);
        currentPosition += 1;

        return getOptionalSourcedParseResult(new PrintlnStmt(expression), position, currentPosition);
    }

    // `return` [exp] `;`
    private Optional<ParseResult<ReturnStmt>> parseReturn(final int position) throws ParserException {
        if (!isExpectedToken(position, ReturnToken.class)) {
            return Optional.empty();
        }

        // We are now definitely parsing a return statement
        int currentPosition = position + 1;
        Expression returnValue = null;

        try {
            Optional<ParseResult<Expression>> optionalParsedReturnValue = parseExp(currentPosition);
            ParseResult<Expression> parsedReturnValue = optionalParsedReturnValue.get();

            returnValue = parsedReturnValue.result;
            currentPosition = parsedReturnValue.nextPosition;
        } catch (ParserNoElementFoundException ex) {
            // If we have a parser no element found, we couldn't parse the exp that was optional (this is okay)
        }

        throwParserExceptionOnNoSemicolon("return statement", currentPosition);
        currentPosition += 1;

        return getOptionalSourcedParseResult(new ReturnStmt(returnValue), position, currentPosition);
    }

    // `{` stmt* `}`
    private Optional<ParseResult<StmtBlock>> parseStatementBlock(final int position) throws ParserException {
        if (!isExpectedToken(position, LeftBraceToken.class)) {
            return Optional.empty();
        }

        // We are now definitely parsing a statement block
        int currentPosition = position + 1;

        List<Statement> blockBody = new ArrayList<>();
        currentPosition = parseZeroOrMore(this::parseStatement, blockBody::add, currentPosition);

        throwParserExceptionOnUnexpected("statement body", RightBraceToken.class, "}", currentPosition);
        currentPosition += 1;

        return getOptionalSourcedParseResult(new StmtBlock(blockBody), position, currentPosition);
    }

    // exp `;`
    private Optional<ParseResult<ExpressionStmt>> parseExpressionStatement(final int position) throws ParserException {
        Expression expression;
        int currentPosition;

        Optional<ParseResult<Expression>> optionalParsedExpression;

        try {
           optionalParsedExpression = parseExp(position);
        } catch (ParserMalformedException ex) {
            // If our result was malformed, we should rethrow the exception
            throw ex;
        } catch (ParserNoElementFoundException ex) {
            // If we could not parse, treat this as okay and let the caller handle it
            return Optional.empty();
        }

        ParseResult<Expression> parsedExpression = optionalParsedExpression.get();

        expression = parsedExpression.result;
        currentPosition = parsedExpression.nextPosition;

        throwParserExceptionOnNoSemicolon("expression statement", currentPosition);
        currentPosition += 1;

        return getOptionalSourcedParseResult(new ExpressionStmt(expression), position, currentPosition);
    }

    private final static Map<Token, OperatorEnum> TOKEN_TO_OP = Map.ofEntries(
        new SimpleImmutableEntry<>(new OrToken(), OperatorEnum.OR),
        new SimpleImmutableEntry<>(new AndToken(), OperatorEnum.AND),
        new SimpleImmutableEntry<>(new DoubleEqualsToken(), OperatorEnum.DOUBLE_EQUALS),
        new SimpleImmutableEntry<>(new NotEqualsToken(), OperatorEnum.NOT_EQUALS),
        new SimpleImmutableEntry<>(new LessThanEqualsToken(), OperatorEnum.LESS_THAN_EQUALS),
        new SimpleImmutableEntry<>(new GreaterThanEqualsToken(), OperatorEnum.GREATER_THAN_EQUALS),
        new SimpleImmutableEntry<>(new LessThanToken(), OperatorEnum.LESS_THAN),
        new SimpleImmutableEntry<>(new GreaterThanToken(), OperatorEnum.GREATER_THAN),
        new SimpleImmutableEntry<>(new PlusToken(), OperatorEnum.PLUS),
        new SimpleImmutableEntry<>(new MinusToken(), OperatorEnum.MINUS),
        new SimpleImmutableEntry<>(new MultiplyToken(), OperatorEnum.MULTIPLY),
        new SimpleImmutableEntry<>(new DivisionToken(), OperatorEnum.DIVISION),
        new SimpleImmutableEntry<>(new NotToken(), OperatorEnum.NOT),
        new SimpleImmutableEntry<>(new DotToken(), OperatorEnum.DOT)
    );

    public ParseResult<Expression> parseMandatoryExp(final int position) throws ParserException {
        return parseMandatory("an expression", this::parseExp, position);
    }

    // exp ::= or_exp
    public Optional<ParseResult<Expression>> parseExp(final int position) throws ParserException {
        // Try to parse an or expression - I'm just following the grammar for now, but I dont' think we need this
        return parseOrExp(position);
    }

    // Genericized parser binary operator function (does not work for dot op)
    public Optional<ParseResult<Expression>> parseBinaryOpExpression(
            final int position,
            ParsingFunction<Integer, Optional<ParseResult<Expression>>> parseLowerPrecedence,
            String subExpressionName,
            List<Class<? extends Token>> validOperatorClasses) throws ParserException {
        
        int currentPosition = position;
        // Try to parse a lower precedence expression
        Optional<ParseResult<Expression>> returnValue = parseLowerPrecedence.apply(currentPosition);
        throwParserExceptionOnEmptyOptional(subExpressionName, returnValue, "an expression", currentPosition);

        currentPosition = returnValue.get().nextPosition;

        // See if there is an expected operator for this level
        while (isValidOperatorToken(currentPosition, validOperatorClasses)) {
            // Get the operator
            OperatorEnum op = TOKEN_TO_OP.get(getToken(currentPosition).get());
            currentPosition += 1;
            
            // Parse the right hand side of the binary op expression
            Optional<ParseResult<Expression>> rightExp = parseLowerPrecedence.apply(currentPosition);
            throwParserExceptionOnEmptyOptional(subExpressionName, rightExp, "an expression", currentPosition);

            // Create binary op expression
            Expression binOpExp = new BinaryOpExp(returnValue.get().result, op, rightExp.get().result);
            returnValue = getOptionalSourcedParseResult(binOpExp, position, rightExp.get().nextPosition);
            currentPosition = returnValue.get().nextPosition;
        }

        return returnValue;
    }

    // Returns true if token is in list - Putting a lambda in the while guard wasn't working :(
    public boolean isValidOperatorToken(final int position, List<Class<? extends Token>> operatorClasses) {
        for (Class<? extends Token> tokenClass : operatorClasses) {
            if (isExpectedToken(position, tokenClass)) {
                return true;
            }
        }

        return false;
    }

    // or_exp ::= and_exp (`||` and_exp)*
    public Optional<ParseResult<Expression>> parseOrExp(final int position) throws ParserException {
        return parseBinaryOpExpression(position, this::parseAndExp, "and expression", Arrays.asList(OrToken.class));
    }

    // and_exp ::= equals_exp (`&&` equals_exp)*
    public Optional<ParseResult<Expression>> parseAndExp(final int position) throws ParserException {
        return parseBinaryOpExpression(position, this::parseEqualsExp, "equals expression", Arrays.asList(AndToken.class));
    }

    // equals_exp ::= lte_gte_exp ((`==` | `!=`) lte_gte_exp)*
    public Optional<ParseResult<Expression>> parseEqualsExp(final int position) throws ParserException {
        return parseBinaryOpExpression(
            position, this::parseInequalityExp, "inequality expression",
            Arrays.asList(DoubleEqualsToken.class, NotEqualsToken.class));
    }

    // lte_gte_exp ::= add_exp [(`<=` | `>=` | `<` | `>`) add_exp]
    // Because the inequalities, I didn't try to genericize it, but maybe it can be done?
    public Optional<ParseResult<Expression>> parseInequalityExp(final int position) throws ParserException {
        int currentPosition = position;
        // Try to parse an add expression
        Optional<ParseResult<Expression>> returnValue = parseAddExp(currentPosition);
        throwParserExceptionOnEmptyOptional("add expression", returnValue, "an expression", currentPosition);
        currentPosition = returnValue.get().nextPosition;

        // If there is another expression to parse
        if (isExpectedToken(currentPosition, LessThanEqualsToken.class)
                || isExpectedToken(currentPosition, GreaterThanEqualsToken.class)
                || isExpectedToken(currentPosition, LessThanToken.class)
                || isExpectedToken(currentPosition, GreaterThanToken.class)) {
            // Get the operator (it's definitely here, we just checked)
            OperatorEnum op = TOKEN_TO_OP.get(getToken(currentPosition).get());
            currentPosition += 1;
            // Try to parse an add expression
            Optional<ParseResult<Expression>> rightAddExp = parseAddExp(currentPosition);
            throwParserExceptionOnEmptyOptional("add expression", rightAddExp, "an expression", currentPosition);
            // Create binary operator, wrap in expression parse result
            BinaryOpExp binOpExp = new BinaryOpExp(returnValue.get().result, op, rightAddExp.get().result);
            returnValue = getOptionalSourcedParseResult(binOpExp, position, rightAddExp.get().nextPosition);
            currentPosition = returnValue.get().nextPosition;
        }
        return returnValue;
    }

    // add_exp ::= mult_exp ((`+` | `-`) mult_exp)*
    public Optional<ParseResult<Expression>> parseAddExp(final int position) throws ParserException {
        return parseBinaryOpExpression(
            position, this::parseMultExp, "multiplication expression",
            Arrays.asList(PlusToken.class, MinusToken.class));
    }

    // mult_exp ::= not_exp ((`*` | `/`) not_exp)*
    public Optional<ParseResult<Expression>> parseMultExp(final int position) throws ParserException {
        return parseBinaryOpExpression(
            position, this::parseNotExp, "not expression",
            Arrays.asList(MultiplyToken.class, DivisionToken.class));
    }

    // not_exp ::= [`!`]dot_exp
    public Optional<ParseResult<Expression>> parseNotExp(final int position) throws ParserException {
        int currentPosition = position;
        Optional<ParseResult<Expression>> returnValue;

        // If next token is a not token
        if (isExpectedToken(currentPosition, NotToken.class)) {
            currentPosition += 1;
            // Get dot expresionn
            Optional<ParseResult<Expression>> dotExp = parseDotExp(currentPosition);
            throwParserExceptionOnEmptyOptional("dot expression", dotExp, "an expression", currentPosition);
            // Create a not unary expression, wrap in expression optional
            UnaryOpExp unaryOpExp = new UnaryOpExp(OperatorEnum.NOT, dotExp.get().result);
            returnValue = getOptionalSourcedParseResult(unaryOpExp, position, dotExp.get().nextPosition);
        } else {
            // Return a dot expression as an expression
            returnValue = parseDotExp(currentPosition);
            throwParserExceptionOnEmptyOptional("dot expression", returnValue, "an expression", currentPosition);
        }
        return returnValue;
    }

    // dot_exp ::= primary_exp (`.` var)*
    // I restricted the right hand side to variables, but if we want to use parseBinaryOpExpression,
    // We can use expression instead of variable on the right
    public Optional<ParseResult<Expression>> parseDotExp(final int position) throws ParserException {
        final String dotExpString = "dot expression";
        int currentPosition = position;
        // Make sure there's a primary expression here
        Optional<ParseResult<Expression>> returnValue = parsePrimaryExp(currentPosition);
        throwParserExceptionOnEmptyOptional(dotExpString, returnValue, "an expression", currentPosition);
        currentPosition = returnValue.get().nextPosition;

        // While there are other variables to parse
        while (isExpectedToken(currentPosition, DotToken.class)) {
            currentPosition += 1;
            // Try to parse a variable
            Optional<ParseResult<Variable>> variable = parseVar(currentPosition);
            throwParserExceptionOnEmptyOptional(dotExpString, variable, "a variable", currentPosition);
            // Create dot operator, wrap in expression parse result
            DotExp dotExp = new DotExp(returnValue.get().result, variable.get().result);
            returnValue = getOptionalSourcedParseResult(dotExp, position, variable.get().nextPosition);
            currentPosition = returnValue.get().nextPosition;
        }
        return returnValue;
    }

    // Create a mapping from the token's class to its type
    private static final Map<Class<? extends Token>, Function<Token, PrimaryExpression>> TOKEN_TO_PRIMARY = Map.of(
            IntLiteralToken.class, (token) -> new IntLiteralExp(Integer.parseInt(token.getTokenizedValue())),
            TrueToken.class, (token) -> new BoolLiteralExp(true),
            FalseToken.class, (token) -> new BoolLiteralExp(false),
            NullToken.class, (token) -> new NullExp()
    );

    /*
     * Try to parse primary expression
     * primary_exp ::= i | `true` | `false` | var |
     * `null` | `(` exp `)`
     * `new` structname `{` struct_actual_params `}` |
     * funcname `(` comma_exp `)`
     */
    public Optional<ParseResult<Expression>> parsePrimaryExp(final int position) throws ParserException {
        // Try to parse a function call first (because otherwise it could be a variable)
        Optional<ParseResult<Expression>> optionalPrimaryExp = parseFuncCall(position);
        if (!optionalPrimaryExp.isEmpty()) {
            return optionalPrimaryExp;
        }

        // Get the token
        final Optional<Token> maybeToken = getToken(position);
        if (maybeToken.isEmpty()) {
            throw new ParserNoElementFoundException("primary expression");
        }

        Token token = maybeToken.get();
        Class<? extends Token> tokenClass = token.getClass();

        // Then try to parse int, bool, var or null
        if (TOKEN_TO_PRIMARY.containsKey(tokenClass)) {
            Expression exp = TOKEN_TO_PRIMARY.get(tokenClass).apply(token);
            return getOptionalSourcedParseResult(exp, position, position + 1);
        }

        if (isExpectedToken(position, IdentifierToken.class)) {
            return parseVariableExpression(position);
        }

        // Then try to parse struct allocation
        optionalPrimaryExp = parseStructAlloc(position);
        if (!optionalPrimaryExp.isEmpty()) {
            return optionalPrimaryExp;
        }

        // Then try a parenthesitized expression
        optionalPrimaryExp = parseParenExp(position);
        if (optionalPrimaryExp.isPresent()) {
            return optionalPrimaryExp;
        }

        // We could not find a matching primary expression, and what even could it be?
        throw new ParserNoElementFoundException("primary expression");
    }

    private Optional<ParseResult<Expression>> parseVariableExpression(final int position) throws ParserException {
        Optional<ParseResult<Variable>> optionalParsedVar = parseVar(position);
        ParseResult<Variable> parsedVarResult = optionalParsedVar.get();

        Expression expression = new VariableExp(parsedVarResult.result);
        expression.setSource(parsedVarResult.result.getSource());

        return Optional.of(new ParseResult<>(expression, parsedVarResult.nextPosition));
    }

    // `new` structname `{` struct_actual_params `}`
    public Optional<ParseResult<Expression>> parseStructAlloc(final int position) throws ParserException {
        final String structAllocString = "struct allocation";
        int currentPosition = position;

        // Try to parse a new token
        if (isExpectedToken(currentPosition, NewToken.class)) {
            currentPosition += 1;
            // Get the structName - throw exceptions after this point
            Optional<ParseResult<Type>> optionalParsedType = parseType(currentPosition);
            throwParserExceptionOnEmptyOptional(structAllocString, optionalParsedType, "struct name", currentPosition);

            ParseResult<Type> parsedTypeResult = optionalParsedType.get();
            Type parsedType = parsedTypeResult.result;

            if (!(parsedType instanceof StructType)) {
                throwParserException("struct allocation", "a struct name for initialization", position);
            }

            StructType structType = (StructType) parsedType;
            currentPosition = optionalParsedType.get().nextPosition;

            // Skip the left brace
            throwParserExceptionOnUnexpected(structAllocString, LeftBraceToken.class, "left brace {", currentPosition);
            currentPosition += 1;

            // Parse struct actual params
            ParseResult<StructActualParams> structActParams = parseStructActualParams(currentPosition);
            currentPosition = structActParams.nextPosition;

            // Skip right brace
            throwParserExceptionOnUnexpected(structAllocString, RightBraceToken.class, "right brace }", currentPosition);
            currentPosition += 1;

            // Create and return struct alloc
            return getOptionalSourcedParseResult(
                    new StructAllocExp(structType, structActParams.result),
                    position,
                    currentPosition
            );
        }
        // Otherwise, return empty to try something else
        return Optional.empty();
    }

    // funcname `(` comma_exp `)
    public Optional<ParseResult<Expression>> parseFuncCall(final int position) throws ParserException {
        final String funcCallString = "function call";
        int currentPosition = position;
        // Try to parse a function name
        Optional<ParseResult<FunctionName>> optionalFuncName = parseFuncName(currentPosition);
        if (optionalFuncName.isEmpty()) {
            return Optional.empty();
        }
        currentPosition = optionalFuncName.get().nextPosition;

        // If this is an left paren
        if (isExpectedToken(currentPosition, LeftParenToken.class)) {
            currentPosition += 1;
            // Parse comma expression
            ParseResult<CommaExp> commaExp = parseCommaExp(currentPosition);
            currentPosition = commaExp.nextPosition;
            // Parse right paren
            throwParserExceptionOnUnexpected(funcCallString, RightParenToken.class, "right paren )", currentPosition);
            currentPosition += 1;
            // return function call
            return getOptionalSourcedParseResult(
                    new FuncCallExp(optionalFuncName.get().result, commaExp.result),
                    position,
                    currentPosition
            );
        }
        // return empty if not function call
        return Optional.empty();
    }

    // comma_exp ::= [exp (`,` exp)*]
    // Consider genericizing comma_exp, comma_param, struct_actual_params
    public ParseResult<CommaExp> parseCommaExp(final int position) throws ParserException {
        final String commaString = "comma expressions (in function call)";
        int currentPosition = position;
        // Make a list of expressions
        List<Expression> listOfExp = new ArrayList<>();

        // Try to parse an expression, return empty if not there
        Optional<ParseResult<Expression>> optionalExp = parseExp(currentPosition);
        if (!optionalExp.isEmpty()) {
            // If there is one, add it to the list
            currentPosition = optionalExp.get().nextPosition;
            listOfExp.add(optionalExp.get().result);
            // While there's a comma next
            while (isExpectedToken(currentPosition, CommaToken.class)) {
                currentPosition += 1;
                // Parse another exp, add it to the list - there has to be one now
                optionalExp = parseExp(currentPosition);
                throwParserExceptionOnEmptyOptional(commaString, optionalExp,
                        "an expression", currentPosition);
                listOfExp.add(optionalExp.get().result);
                currentPosition = optionalExp.get().nextPosition;
            }
        }

        // Create struct actual params with list and return
        return getSourcedParseResult(new CommaExp(listOfExp), position, currentPosition);
    }

    // `(` exp `)`
    public Optional<ParseResult<Expression>> parseParenExp(final int position) throws ParserException {
        String parenExpString = "primary parenthesized expression";

        if (!isExpectedToken(position, LeftParenToken.class)) {
            return Optional.empty();
        }

        // There was a left paren here
        int currentPosition = position + 1;

        // Try to parse expression
        Optional<ParseResult<Expression>> optionalExpression = parseExp(currentPosition);
        throwParserExceptionOnEmptyOptional("paren expression", optionalExpression, "an expression", currentPosition);
        currentPosition = optionalExpression.get().nextPosition;

        // Make sure there's a right paren here
        throwParserExceptionOnUnexpected(parenExpString, RightParenToken.class, "right paren )", currentPosition);
        currentPosition += 1;

        // Return the Expression
        Expression parenExp = new ParenExp(optionalExpression.get().result);
        return getOptionalSourcedParseResult(parenExp, position, currentPosition);
    }

    // Try to parse a function name
    public Optional<ParseResult<FunctionName>> parseFuncName(final int position) throws ParserException {
        final Optional<Token> maybeToken = getToken(position);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        // If this is an identifier
        if (token instanceof IdentifierToken) {
            // Create a new Optional ParseResult for a Function Name
            return getOptionalSourcedParseResult(
                            new FunctionName(token.getTokenizedValue()),
                            position,
                            position + 1
            );
        }

        return Optional.empty();
    }

    // Tries to parse variable
    public Optional<ParseResult<Variable>> parseVar(final int position) throws ParserException {
        final Optional<Token> maybeToken = getToken(position);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        // If this is an identifier
        if (token instanceof IdentifierToken) {
            // Create a new Optional ParseResult for a Variable
            return getOptionalSourcedParseResult(
                    new Variable(token.getTokenizedValue()),
                    position,
                    position + 1
            );
        }

        return Optional.empty();
    }

    // Create a mapping from the token's class to its type
    private static final Map<Class<? extends Token>, Function<Token, Type>> TOKEN_CLASS_TO_TYPE = Map.of(
            IntToken.class, (token) -> new IntType(),
            BoolToken.class, (token) -> new BoolType(),
            VoidToken.class, (token) -> new VoidType(),
            IdentifierToken.class, (token) -> new StructType(new StructName(token.getTokenizedValue()))
    );

    // type ::= 'int' | 'bool' | 'void' | structname
    public Optional<ParseResult<Type>> parseType(final int position) throws ParserException {
        final Optional<Token> maybeToken = getToken(position);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        Class<? extends Token> tokenClass = token.getClass();

        if (!TOKEN_CLASS_TO_TYPE.containsKey(tokenClass)) {
            return Optional.empty();
        }

        Type type = TOKEN_CLASS_TO_TYPE.get(tokenClass).apply(token);
        return getOptionalSourcedParseResult(type, position, position + 1);
    }

    private <T extends AbstractSyntaxTreeNode> T setSource(T t,
                                                           int inclusiveStartPosition,
                                                           int exclusiveEndPosition) {
        List<Source> tokenSources = new ArrayList<>();
        for (int i = inclusiveStartPosition; i < exclusiveEndPosition; i++) {
            tokenSources.add(sourcedTokens.get(i).getSource());
        }

        Source combinedTokenSources = Source.fromSources(tokenSources);
        t.setSource(combinedTokenSources);

        return t;
    }

    private <T extends AbstractSyntaxTreeNode> ParseResult<T> getSourcedParseResult(T t,
                                                                                    int inclusiveStartPosition,
                                                                                    int exclusiveEndPosition) {
        setSource(t, inclusiveStartPosition, exclusiveEndPosition);
        return new ParseResult<>(t, exclusiveEndPosition);
    }

    private <T extends AbstractSyntaxTreeNode> Optional<ParseResult<T>> getOptionalSourcedParseResult(T t,
                                                                                                      int inclusiveStartPosition,
                                                                                                      int exclusiveEndPosition) {
        return Optional.of(getSourcedParseResult(t, inclusiveStartPosition, exclusiveEndPosition));
    }

    private <T extends AbstractSyntaxTreeNode> T getSourcedNode(int position, Function<Token, T> parseFunction) {
        Sourced<Token> sourcedToken = sourcedTokens.get(position);
        Token token = sourcedToken.getValue();

        T node = parseFunction.apply(token);
        node.setSource(sourcedToken.getSource());

        return node;
    }

}
