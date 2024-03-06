package refraff.parser;

import java.util.*;

import refraff.parser.ParserException;
import refraff.parser.statement.*;
import refraff.parser.type.*;
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
    public boolean checkTokenIs(final int position,
                                final Token expected) throws ParserException {
        final Optional<Token> received = getToken(position);
        return received.isPresent() && expected.equals(received.get());
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
    // Well, at the moment it's just program ::= stmt*
    public ParseResult<Program> parseProgram(final int position) throws ParserException {
        int currentPosition = position;
        List<StructDef> structDefs = new ArrayList<>();
        List<Statement> statements = new ArrayList<>();

        // Keep trying to parse struct definitions until
        while (true) {
            Optional<ParseResult<StructDef>> structDefResult = parseStructDef(currentPosition);
            if (!structDefResult.isPresent()) break;
            ParseResult<StructDef> result = structDefResult.get();
            structDefs.add(result.result);
            currentPosition = result.nextPosition;
        }

        // Keep trying to parse statements until none are left
        while (true) {
            Optional<ParseResult<Statement>> statementResult = parseStatement(currentPosition);
            if (!statementResult.isPresent()) break; // Exit if no more statements
            ParseResult<Statement> result = statementResult.get();
            statements.add(result.result);
            currentPosition = result.nextPosition;
        }

        return new ParseResult<Program>(new Program(structDefs, statements), currentPosition);
    }

    // structdef ::= `struct` structname `{` (param `;`)* `}`
    public Optional<ParseResult<StructDef>> parseStructDef(final int position) throws ParserException {
        int currentPosition = position;

        // Check that there's a struct token here
        if (!checkTokenIs(currentPosition, new StructToken())) {
            return Optional.empty();
        } else {
            currentPosition += 1;
        }

        // Try to parse the struct name
        Optional<ParseResult<Type>> opStructName = parseType(currentPosition);
        if (!opStructName.isPresent() || !(opStructName.get().result instanceof StructName)) {
            return Optional.empty();
        }
        // Cast struct name to StructName object
        StructName structName = (StructName)opStructName.get().result;
        currentPosition += 1;

        // Check that there's a left brace here
        if (!checkTokenIs(currentPosition, new LeftBraceToken())) {
            return Optional.empty();
        } else {
            currentPosition += 1;
        }

        List<Param> params = new ArrayList<>();
        // Check for params
        while (true) {
            Optional<ParseResult<Param>> paramResult = parseParam(currentPosition);
            if (!paramResult.isPresent())
                break; // Exit if no more statements
            ParseResult<Param> param = paramResult.get();
            params.add(param.result);
            currentPosition = param.nextPosition;
        }

        // Check that there's a right brace here
        if (!checkTokenIs(currentPosition, new RightBraceToken())) {
            return Optional.empty();
        } else {
            currentPosition += 1;
        }

        // Return the variable declaration
        return Optional.of(
            new ParseResult<StructDef>(
                new StructDef(structName, params),
                currentPosition
            )
        );
    }

    // param :: = type var
    public Optional<ParseResult<Param>> parseParam(final int position) throws ParserException {
        int currentPosition = position;

        // Try to parse the type
        Optional<ParseResult<Type>> opType = parseType(currentPosition);
        if (!opType.isPresent()) {
            return Optional.empty();
        }
        ParseResult<Type> type = opType.get();
        currentPosition = type.nextPosition;

        // Try to parse the variable
        Optional<ParseResult<Variable>> opVar = parseVar(currentPosition);
        if (!opVar.isPresent()) {
            return Optional.empty();
        }
        ParseResult<Variable> var = opVar.get();
        currentPosition = var.nextPosition;

        // Check that there's a semicolon here
        if (!checkTokenIs(currentPosition, new SemicolonToken())) {
            return Optional.empty();
        } else {
            currentPosition += 1;
        }

        // Return the variable declaration
        return Optional.of(
            new ParseResult<Param>(
                new Param(type.result, var.result),
                currentPosition
            )
        );
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
    // But for now, it's just stmt ::= var '=' exp ';'
    public Optional<ParseResult<Statement>> parseStatement(final int position) throws ParserException {
        // Try to parse an assignment statement, return immediately if successful
        Optional<ParseResult<VardecStmt>> result = parseVardec(position);
        if (result.isPresent()) {
            // Map Vardec result to Statement result
            ParseResult<Statement> statementResult = new ParseResult<>(result.get().result,
                    result.get().nextPosition);
            return Optional.of(statementResult);
        } else {
            return Optional.empty();
        }
    }

    // vardec ::= type var '=' exp ';'
    public Optional<ParseResult<VardecStmt>> parseVardec(final int position) throws ParserException {
        int currentPosition = position;
        // This is a weird idea, but can we make a list of the items in a production
        // And loop through them?

        // Try to parse the type
        Optional<ParseResult<Type>> opType = parseType(currentPosition);
        if (!opType.isPresent()) {
            return Optional.empty();
        }
        ParseResult<Type> type = opType.get();
        currentPosition = type.nextPosition;

        // Try to parse the variable
        Optional<ParseResult<Variable>> opVar = parseVar(currentPosition);
        if (!opVar.isPresent()) {
            return Optional.empty();
        }
        ParseResult<Variable> var = opVar.get();
        currentPosition = var.nextPosition;

        // Check that there's an assignment operator here
        if (!checkTokenIs(currentPosition, new AssignmentToken())) {
            return Optional.empty();
        } else {
            currentPosition += 1;
        }

        // Check that there's an expression
        Optional<ParseResult<Expression>> opExp = parseExp(currentPosition);
        if (!opExp.isPresent()) {
            return Optional.empty();
        }
        ParseResult<Expression> exp = opExp.get();
        currentPosition = exp.nextPosition;

        // Check that there's a semicolon
        if (!checkTokenIs(currentPosition, new SemicolonToken())) {
            return Optional.empty();
        } else {
            currentPosition += 1;
        }

        // Return the variable declaration
        return Optional.of(
            new ParseResult<VardecStmt>(
                new VardecStmt(type.result, var.result, exp.result),
                currentPosition
            )
        );
    }

    // exp ::= or_exp
    // But for now, exp ::= int literal
    public Optional<ParseResult<Expression>> parseExp(final int position) throws ParserException {
        final Optional<Token> maybeToken = getToken(position);
        if (!maybeToken.isPresent()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        // Try to parse int literal
        if (token instanceof IntLiteralToken) {
            // This is just a placeholder because I want to start testing it already
            // Create a new parse result for Int Literal
            Optional<ParseResult<IntLiteralExp>> result = Optional.of(new ParseResult<IntLiteralExp>(
                new IntLiteralExp(Integer.parseInt(((IntLiteralToken)token).getTokenizedValue())), 
                position + 1
            ));
            // Wow, that's ugly ^^
            // Map int literal exp result to exp result
            ParseResult<Expression> statementResult = new ParseResult<>(result.get().result,
                    result.get().nextPosition);
            return Optional.of(statementResult);
        } else {
            return Optional.empty();
        }
    }

    // Tries to parse variable
    public Optional<ParseResult<Variable>> parseVar(final int position) throws ParserException {
        final Optional<Token> maybeToken = getToken(position);
        if (!maybeToken.isPresent()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        // If this is an identifier
        if (token instanceof IdentifierToken) {
            // Create a new Optional ParseResult for a Variable
            // Construct the new Variable with the Identifier token's name
            // I know this looks insane
            return Optional.of(
                new ParseResult<Variable>(
                    new Variable(
                        ((IdentifierToken)token).getTokenizedValue()), 
                    position + 1
                )
            );
        } else {
            return Optional.empty();
        }
    }

    // (IdentifierToken)

    // type ::= 'int' | 'bool' | 'void' | structname
    // But right now it's only type ::= 'int'
    public Optional<ParseResult<Type>> parseType(final int position) throws ParserException {
        final Optional<Token> maybeToken = getToken(position);
        if (!maybeToken.isPresent()) {
            return Optional.empty();
        }

        Token token = maybeToken.get();
        if (token instanceof IntToken) {
            return Optional.of(new ParseResult<Type>(new IntType(), position + 1));
        } else if (token instanceof IdentifierToken) {
            // Make a variable from the identifier
            Variable var = new Variable(((IdentifierToken)token).getTokenizedValue());
            return Optional.of(new ParseResult<>(new StructName(var), position + 1));
        } else {
            return Optional.empty();
        }
        // else if (token instanceof BoolToken) {
        //     return new ParseResult<Type>(new BoolType(), position + 1);
        // }
        
    }
}
