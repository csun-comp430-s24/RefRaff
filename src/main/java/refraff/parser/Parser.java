package refraff.parser;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import refraff.parser.function.*;
import refraff.parser.statement.*;
import refraff.parser.type.*;
import refraff.parser.operator.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.structure.*;
import refraff.parser.expression.*;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.reserved.*;
import refraff.tokenizer.*;

public class Parser {
    
    public final Token[] tokens;

    public Parser(final Token[] tokens) {
        this.tokens = tokens;
    }


    // Returns an optional token or emtpy if we've reached the end of tokens
    public Optional<Token> getToken(final int position) {
        if (position >= 0 && position < tokens.length) {
            return Optional.of(tokens[position]);
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
        final String exceptionMessage = "Error parsing %s: expected %s but received: %s";
        String actualTokenValue = getToken(position).map(Token::toString).orElse("none");

        String formattedExceptionMessage = String.format(exceptionMessage, beingParsed, expected, actualTokenValue);
        throw new ParserException(formattedExceptionMessage);
    }


    // Attempts to parse token array
    public static Program parseProgram(Token[] tokens) throws ParserException {
        final Parser parser = new Parser(tokens);
        final ParseResult<Program> program = parser.parseProgram(0);
        if (program.nextPosition == tokens.length) {
            return program.result;
        } else {
            throw new ParserException("Remaining tokens at end, starting with: " +
                                      parser.getToken(program.nextPosition).toString());
        }
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

        return new ParseResult<>(new Program(structDefs, functionDefs, statements), currentPosition);
    }

    /**
     * Note: this is a generic version of code previously written to avoid code duplication.
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
            Optional<ParseResult<T>> optionalParseResult = parseFunction.apply(currentPosition);
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
        if (opStructName.isEmpty() || !(opStructName.get().result instanceof StructName)) {
            throwParserException(structDefinition, "struct name", currentPosition);
        }

        ParseResult<Type> parsedTypeResult = opStructName.get();
        StructName structName = (StructName) parsedTypeResult.result;
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
        return Optional.of(
            new ParseResult<>(
                new StructDef(structName, params),
                currentPosition
            )
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
        return Optional.of(
            new ParseResult<>(
                new Param(type.result, var.result),
                currentPosition
            )
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
        FunctionName functionName = new FunctionName(getToken(currentPosition).get().getTokenizedValue());

        currentPosition += 1;

        // Create the function name, update error message for this function
        final String functionDefinitionWithName = functionDefinition + " for function " + functionName.getParsedValue();

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
        return Optional.of(new ParseResult<>(functionDef, currentPosition));
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
    // But for now, it's just stmt ::= type var '=' exp ';' |
    //                                 var '=' exp ';' |
    //                                 `if` `(` exp `)` stmt [`else` stmt] |
    //                                 `{` stmt* `}`
    public Optional<ParseResult<Statement>> parseStatement(final int position) throws ParserException {
        // (Makes it easier to add more in the future without code repeat)
        return parseStatement(List.of(
                // We need to parse assignment first, otherwise vardec will eat our assignment and throw an exception
                this::parseAssign,
                this::parseVardec,
                this::parseIfElse,
                this::parseStatementBlock
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

            // Downcast the parsed statement result to just being a statement
            ParseResult<? extends Statement> realResult = optionalParseResult.get();
            ParseResult<Statement> statementResult = new ParseResult<>(realResult.result, realResult.nextPosition);

            // Return the downcasted version
            return Optional.of(statementResult);
        }

        // Else return an empty if we found no statements
        return Optional.empty();
    }


    // vardec ::= type var '=' exp ';'
    public Optional<ParseResult<VardecStmt>> parseVardec(final int position) throws ParserException {
        int currentPosition = position;

        // Try to parse the type
        Optional<ParseResult<Type>> opType = parseType(currentPosition);
        if (opType.isEmpty()) {
            return Optional.empty();
        }

        // If we have a type, we must be trying to parse a vardec. Should be throwing errors
        ParseResult<Type> type = opType.get();
        currentPosition = type.nextPosition;

        // Try to parse assignment statement, throwing if we do not have an identifier or if variable assignment fails
        Optional<ParseResult<AssignStmt>> opAssign = parseAssign(currentPosition, true);

        // Safe unwrap since we know we throw if we didn't successfully parse a variable assignment
        ParseResult<AssignStmt> assign = opAssign.get();
        currentPosition = assign.nextPosition;

        // Return the variable declaration
        return Optional.of(
            new ParseResult<>(
                new VardecStmt(type.result, assign.result.variable, 
                                assign.result.expression),
                currentPosition
            )
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
        Optional<ParseResult<Variable>> opVar = parseVar(currentPosition);
        if (opVar.isEmpty()) {
            if (shouldThrowIfNoIdentifier) {
                throwParserException(variableAssignment, "a variable name", currentPosition);
            }

            return Optional.empty();
        }

        // We now know we are parsing an assignment variable, so going further we must throw an exception
        ParseResult<Variable> var = opVar.get();
        currentPosition = var.nextPosition;

        // Ensure that there's an assignment operator here
        throwParserExceptionOnUnexpected(variableAssignment, AssignmentToken.class, "assignment operator =",
                currentPosition);
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
        return Optional.of(
            new ParseResult<>(
                new AssignStmt(var.result, exp.result),
                currentPosition
            )
        );
    }


    private Optional<ParseResult<IfElseStmt>> parseIfElse(final int position) throws ParserException {
        if (!isExpectedToken(position, IfToken.class)) {
            return Optional.empty();
        }

        // We know we're parsing an if/else now
        final String ifStatement = "if statement";
        int currentPosition = position + 1;

        throwParserExceptionOnUnexpected(ifStatement, LeftParenToken.class, "(", currentPosition);
        currentPosition += 1;

        Optional<ParseResult<Expression>> optionalConditionExpression = parseExp(currentPosition);
        throwParserExceptionOnEmptyOptional("if statement condition", optionalConditionExpression, "a valid expression",
                currentPosition);

        ParseResult<Expression> parsedCondition = optionalConditionExpression.get();

        Expression condition = parsedCondition.result;
        currentPosition = parsedCondition.nextPosition;

        throwParserExceptionOnUnexpected(ifStatement, RightParenToken.class, ")", currentPosition);
        currentPosition += 1;

        Optional<ParseResult<Statement>> optionalIfBody = parseStatement(currentPosition);
        throwParserExceptionOnEmptyOptional("if statement body", optionalIfBody, "a statement", currentPosition);

        ParseResult<Statement> parsedIfBody = optionalIfBody.get();

        Statement ifBody = parsedIfBody.result;
        currentPosition = parsedIfBody.nextPosition;

        // [`else` stmt]: if we don't hit an else, then just return the if statement so far
        if (!isExpectedToken(currentPosition, ElseToken.class)) {
            return Optional.of(new ParseResult<>(new IfElseStmt(condition, ifBody), currentPosition));
        }

        currentPosition += 1;

        // If we did hit an else, the stmt becomes mandatory
        Optional<ParseResult<Statement>> optionalElseBody = parseStatement(currentPosition);
        throwParserExceptionOnEmptyOptional("else statement body", optionalElseBody, "a statement", currentPosition);

        ParseResult<Statement> parsedElseBody = optionalElseBody.get();

        Statement elseBody = parsedElseBody.result;
        currentPosition = parsedElseBody.nextPosition;

        return Optional.of(new ParseResult<>(new IfElseStmt(condition, ifBody, elseBody), currentPosition));
    }


    private Optional<ParseResult<StmtBlock>> parseStatementBlock(final int position) throws ParserException {
        if (!isExpectedToken(position, LeftBraceToken.class)) {
            return Optional.empty();
        }

        // We are now definitely parsing a statement block
        int currentPosition = position + 1;

        List<Statement> blockBody = new ArrayList<>();
        currentPosition = parseZeroOrMore(this::parseStatement, blockBody::add, currentPosition);

        throwParserExceptionOnUnexpected("statement block", RightBraceToken.class, "}", currentPosition);
        currentPosition += 1;

        return Optional.of(new ParseResult<>(new StmtBlock(blockBody), currentPosition));
    }


    private final static Map<Token, OperatorEnum> TOKEN_TO_OP = Map.of(
        new OrToken(), OperatorEnum.OR,
        new AndToken(), OperatorEnum.AND,
        new DoubleEqualsToken(), OperatorEnum.DOUBLE_EQUALS,
        new NotEqualsToken(), OperatorEnum.NOT_EQUALS
    );
    


    // exp ::= or_exp
    public Optional<ParseResult<Expression>> parseExp(final int position) throws ParserException {
        // Try to parse an or expression - I'm just following the grammar for now, but I dont' think we need this
        return parseOrExp(position);
    }


    // or_exp ::= and_exp (`||` and_exp)*
    public Optional<ParseResult<Expression>> parseOrExp(final int position) throws ParserException {
        int currentPosition = position;
        // Try to parse an and expression
        Optional<ParseResult<Expression>> returnValue = parseAndExp(position);
        throwParserExceptionOnEmptyOptional("and expression", returnValue, "an expression", currentPosition);
        currentPosition = returnValue.get().nextPosition;

        // While there are more expressions to parse
        while (isExpectedToken(currentPosition, OrToken.class)) {
            // Eat the or token
            currentPosition += 1;
            // Try to parse an and expression
            Optional<ParseResult<Expression>> rightAndExp = parseAndExp(position);
            throwParserExceptionOnEmptyOptional("and expression", rightAndExp, "an expression", currentPosition);
            // Create binary operator, downcast to expression
            BinaryOpExp binOpExp = new BinaryOpExp(returnValue.get().result, OperatorEnum.OR, rightAndExp.get().result);
            returnValue = Optional.of(new ParseResult<Expression>(binOpExp, currentPosition));
            currentPosition = returnValue.get().nextPosition;
        }
        return returnValue;
    }


    // and_exp ::= equals_exp (`&&` equals_exp)*
    // Hmmmm... This is starting to look familiar
    public Optional<ParseResult<Expression>> parseAndExp(final int position) throws ParserException {
        int currentPosition = position;
        // Try to parse an equals expression
        Optional<ParseResult<Expression>> returnValue = parseEqualsExp(position);
        throwParserExceptionOnEmptyOptional("equals expression", returnValue, "an expression", currentPosition);
        currentPosition = returnValue.get().nextPosition;

        // While there are more expressions to parse
        while (isExpectedToken(currentPosition, AndToken.class)) {
            // Skip and token
            currentPosition += 1;
            // Try to parse an equals expression
            Optional<ParseResult<Expression>> rightEqualsExp = parseEqualsExp(position);
            throwParserExceptionOnEmptyOptional("equals expression", rightEqualsExp, "an expression", currentPosition);
            // Create binary operator, downcast to expression
            BinaryOpExp binOpExp = new BinaryOpExp(returnValue.get().result, OperatorEnum.AND, rightEqualsExp.get().result);
            returnValue = Optional.of(new ParseResult<Expression>(binOpExp, currentPosition));
            currentPosition = returnValue.get().nextPosition;
        }
        return returnValue;
    }

    // equals_exp ::= lte_gte_exp ((`==` | `!=`) lte_gte_exp)*
    public Optional<ParseResult<Expression>> parseEqualsExp(final int position) throws ParserException {
        int currentPosition = position;
        // Try to parse an inequality expression
        Optional<ParseResult<Expression>> returnValue = parseInequalityExp(position);
        throwParserExceptionOnEmptyOptional("inequality expression", returnValue, "an expression", currentPosition);
        currentPosition = returnValue.get().nextPosition;

        // While there are more expressions to parse
        while (isExpectedToken(currentPosition, DoubleEqualsToken.class) 
                || isExpectedToken(currentPosition, NotEqualsToken.class)) {
            // Get the operator
            OperatorEnum op = TOKEN_TO_OP.get(getToken(currentPosition));
            currentPosition += 1;
            // Try to parse an inequlality expression
            Optional<ParseResult<Expression>> rightInequalityExp = parseInequalityExp(position);
            throwParserExceptionOnEmptyOptional("inequality expression", rightInequalityExp, "an expression", currentPosition);
            // Create binary operator, downcast to expression
            BinaryOpExp binOpExp = new BinaryOpExp(returnValue.get().result, op, rightInequalityExp.get().result);
            returnValue = Optional.of(new ParseResult<Expression>(binOpExp, currentPosition));
            currentPosition = returnValue.get().nextPosition;
        }
        return returnValue;
    }

    // equals_exp ::= lte_gte_exp ((`==` | `!=`) lte_gte_exp)*
    public Optional<ParseResult<Expression>> parseEqualsExp(final int position) throws ParserException {
        int currentPosition = position;
        // Try to parse an inequality expression
        Optional<ParseResult<Expression>> returnValue = parseInequalityExp(position);
        throwParserExceptionOnEmptyOptional("inequality expression", returnValue, "an expression", currentPosition);
        currentPosition = returnValue.get().nextPosition;

        // While there are more expressions to parse
        while (isExpectedToken(currentPosition, DoubleEqualsToken.class)
                || isExpectedToken(currentPosition, NotEqualsToken.class)) {
            // Get the operator
            OperatorEnum op = TOKEN_TO_OP.get(getToken(currentPosition));
            currentPosition += 1;
            // Try to parse an inequlality expression
            Optional<ParseResult<Expression>> rightInequalityExp = parseInequalityExp(position);
            throwParserExceptionOnEmptyOptional("inequality expression", rightInequalityExp, "an expression",
                    currentPosition);
            // Create binary operator, downcast to expression
            BinaryOpExp binOpExp = new BinaryOpExp(returnValue.get().result, op, rightInequalityExp.get().result);
            returnValue = Optional.of(new ParseResult<Expression>(binOpExp, currentPosition));
            currentPosition = returnValue.get().nextPosition;
        }
        return returnValue;
    }


    // May be better to have primitive types in an enum, so we're not creating so
    // many instances in big programs
    // Create a mapping from the token's class to its type
    private static final Map<Class<? extends Token>, Function<Token, PrimaryExpression>> TOKEN_TO_PRIMITIVE = Map.of(
            IntLiteralToken.class, (token) -> new IntLiteralExp(Integer.parseInt(token.getTokenizedValue())),
            TrueToken.class, (token) -> new BoolLiteralExp(true),
            FalseToken.class, (token) -> new BoolLiteralExp(false),
            IdentifierToken.class, (token) -> new VariableExp(token.getTokenizedValue()),
            NullToken.class, (token) -> new NullExp()
    );

    /*
     * Try to parse primary expression
     * primary_exp ::= i | `true` | `false` | var | 
     * `null` | `(` exp `)`
     */
    public Optional<ParseResult<PrimaryExpression>> parsePrimaryExp(final int position) throws ParserException {
        // Get the token
        final Optional<Token> maybeToken = getToken(position);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        Class<? extends Token> tokenClass = token.getClass();

        if (!TOKEN_CLASS_TO_TYPE.containsKey(tokenClass)) {
            int currentPosition = position;
            // Try to parse parenthesitized expression (this is our only option, now)
            String parenExpString = "primary parenthesitized exression";
            // There should be a left paren here
            throwParserExceptionOnUnexpected(parenExpString, LeftParenToken.class, "left paren (", currentPosition);
            currentPosition += 1;

            // Try to parse expression
            Optional<ParseResult<Expression>> optionalExpression = parseExp(currentPosition);
            throwParserExceptionOnEmptyOptional("if expression", optionalExpression, "an expression", currentPosition);

            // Make sure there's a right paren here
            throwParserExceptionOnUnexpected(parenExpString, LeftParenToken.class, "left paren (", currentPosition);
            currentPosition += 1;

            // Create new paren expression, downcast the paren expression result to a primary statement, and return
            // Am I doing extra steps here?
            ParseResult<? extends PrimaryExpression> parsedExpression = new ParseResult<ParenExp>(
                    new ParenExp(optionalExpression.get().result), currentPosition);;
            ParseResult<PrimaryExpression> primaryExpressionResult = new ParseResult<>(
                    parsedExpression.result, currentPosition);

            // Return the downcasted version
            return Optional.of(primaryExpressionResult);
        }

        PrimaryExpression exp = TOKEN_TO_PRIMITIVE.get(tokenClass).apply(token);
        return Optional.of(new ParseResult<>(exp, position + 1));
    }


    // Maybe worthwhile to have Operator be an enum in this case, instead of a ton of classes
    // private static final Map<Class<? extends Token>, OpEnum>


    // Try to parse an operators
    public Optional<ParseResult<Operator>> parseOp(final int position) throws ParserException {
        // Get the token
        final Optional<Token> maybeToken = getToken(position);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        // Check against operators - there must be a better way!
        // Suggested way in comments above function definition
        if (token instanceof PlusToken) {
            return Optional.of(new ParseResult<>(new PlusOp(), position + 1));
        } else if (token instanceof MinusToken) {
            return Optional.of(new ParseResult<>(new PlusOp(), position + 1));
        } else {
            return Optional.empty();
        }
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
            return Optional.of(
                new ParseResult<>(
                    new Variable(token.getTokenizedValue()),
                    position + 1
                )
            );
        }

        return Optional.empty();
    }

    // May be better to have primitive types in an enum, so we're not creating so many instances in big programs
    // Create a mapping from the token's class to its type
    private static final Map<Class<? extends Token>, Function<Token, Type>> TOKEN_CLASS_TO_TYPE = Map.of(
            IntToken.class, (token) -> new IntType(),
            BoolToken.class, (token) -> new BoolType(),
            VoidToken.class, (token) -> new VoidType(),
            IdentifierToken.class, (token) -> new StructName(token.getTokenizedValue())
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
        return Optional.of(new ParseResult<>(type, position + 1));
    }

}
