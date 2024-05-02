package refraff.codegen;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import refraff.parser.type.StructType;

// Keeps track of the struct variables that were declared in each scope
public class StructScopeManager {
    private Stack<Map<String, StructType>> scopeStack = new Stack<>();
    private Map<String, StructType> variableToStructType = new HashMap<>();

    public void enterScope() {
        scopeStack.push(new HashMap<String, StructType>());
    }

    public void addStructVariableToScope(String newStructVariable, StructType structType) throws CodegenException {
        try {
            Map<String, StructType> currentScope = scopeStack.peek();
            variableToStructType.put(newStructVariable, structType);
            currentScope.put(newStructVariable, structType);
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to add a variable to the current scope, but scope stack was empty");
        }
    }

    public boolean isStructVariable(String variableName) {
        return variableToStructType.containsKey(variableName);
    }

    public StructType getStructTypeFromVariable(String variableName) {
        return variableToStructType.get(variableName);
    }

    public boolean isInScope(String variableName) throws CodegenException {
        try {
            Map<String, StructType> currentScope = scopeStack.peek();
            return currentScope.containsKey(variableName);
        } catch (EmptyStackException e) {
            throw new CodegenException("Tried to check if variable was a struct, but scope stack was empty");
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
