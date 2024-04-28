package refraff.codegen;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import refraff.parser.type.StructType;

// Keeps track of the struct variables that were declared in each scope
public class StructScopeManager {
    private Stack<Map<String, StructType>> scopeStack = new Stack<>();

    public void enterScope() {
        scopeStack.push(new HashMap<String, StructType>());
    }

    public void declareStructVariable(String newStructVariable, StructType structType) throws CodegenException {
        try {
            Map<String, StructType> currentScope = scopeStack.peek();
            currentScope.put(newStructVariable, structType);
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to add a variable to the current scope, but scope stack was empty");
        }
    }

    public Map<String, StructType> getCurrentScopeStructs() throws CodegenException {
        try {
            Map<String, StructType> currentScope = scopeStack.peek();
            return currentScope;
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to get current scope, but scope stack was empty");
        }
    }

    public Map<String, StructType> exitScope() throws CodegenException {
        try {
            Map<String, StructType> currentScope = scopeStack.pop();
            return currentScope;
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to exit scope, but scope stack was empty");
        }
    }
}
