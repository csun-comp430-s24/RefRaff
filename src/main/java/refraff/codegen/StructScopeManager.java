package refraff.codegen;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Stack;

// Keeps track of the struct variables that were declared in each scope
public class StructScopeManager {
    private Stack<ArrayList<String>> scopeStack = new Stack<>();

    public void enterScope() {
        scopeStack.push(new ArrayList<>());
    }

    public void declareStructVariable(String newStructVariable) throws CodegenException {
        try {
            ArrayList<String> currentScope = scopeStack.peek();
            currentScope.add(newStructVariable);
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to add a variable to the current scope, but scope stack was empty");
        }
    }

    public ArrayList<String> getCurrentScopeStructs() throws CodegenException {
        try {
            ArrayList<String> currentScope = scopeStack.peek();
            return currentScope;
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to get current scope, but scope stack was empty");
        }
    }

    public ArrayList<String> exitScope() throws CodegenException {
        try {
            ArrayList<String> currentScope = scopeStack.pop();
            return currentScope;
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to exit scope, but scope stack was empty");
        }
    }
}
