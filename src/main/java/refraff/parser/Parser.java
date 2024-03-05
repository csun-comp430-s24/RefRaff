package refraff.parser;

import java.util.*;

import refraff.parser.ParserException;
import refraff.parser.statement.*;
import refraff.parser.type.*;
import refraff.tokenizer.Token;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.reserved.*;

public class Parser {
    
    public final Token[] tokens;

    public Parser(final Token[] tokens) {
        this.tokens = tokens;
    }

    public Token getToken(final int position) throws ParserException {
        if (position >= 0 && position < tokens.length) {
            return tokens[position];
        } else {
            throw new ParserException("Out of tokens");
        }
    }

    public boolean checkTokenIs(final int position,
                                final Token expected) throws ParserException {
        try {
            final Token received = getToken(position);
            return expected.equals(received);
        } catch (ParserException e) {
            return false;
        }
    }

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
    private ParseResult<Program> parseProgram(final int position) throws ParserException {
        int currentPosition = position;
        List<Statement> statements = new ArrayList<>();

        // Keep trying to parse statements until none are left
        while(true) {
            Optional<ParseResult<Statement>> statementResult = parseStatement(currentPosition);
            if (!statementResult.isPresent()) break; // Exit if no more statements
            ParseResult<Statement> result = statementResult.get();
            statements.add(result.result);
            currentPosition = result.nextPosition;
        }

        return new ParseResult<Program>(new Program(statements), currentPosition);
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
    private ParseResult<Statement> parseStatement(final int position) throws ParserException {
        Optional<ParseResult<Statement>> result;

        // Try to parse an assignment statement, return immediately if successful
        result = parseVardec(position);
        if (result.isPresent()) {
            return result;
        }
    }

    // vardec ::= type var '=' exp ';'
    private Optional<ParseResult<Vardec>> parseVardec(final int position) {
        int currentPosition = position;
        Optional<ParseResult<Token>> type = parseType(position);
        if (!type.isPresent()) {
            return Optional.empty();
        }
        Optional<ParseResult<Variable>> var = ParseVar()


        Optional<ParseResult<Statement>> statementResult = parseStatement(currentPosition);
        if (!statementResult.isPresent()) break; // Exit if no more statements
        ParseResult<Statement> result = statementResult.get();
        statements.add(result.result);
        currentPosition = result.nextPosition;
    }

    // type ::= 'int' | 'bool' | 'void' | structname
    // But right now it's only type ::= 'int'
    private ParseResult<Type> parseType(final int position) throws ParserException {
        final Token token = getToken(position);
        Optional<ParseResult<Type>> type;
        return new ParseResult<Type>(new IntType(), position + 1);
        // if (token instanceof IntToken) {
        //     return new ParseResult<Type>(new IntType(), position + 1);
        // } else if (token instanceof BoolToken) {
        //     return new ParseResult<Type>(new BoolType(), position + 1);
        // }
        
    }
}
