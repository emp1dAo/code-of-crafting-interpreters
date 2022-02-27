package com.craftinginterpreters.jlox;
import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    

    LoxFunction(Stmt.Function declaration, Environment closure) {
	this.closure = closure;
	this.declaration = declaration;
    }

    @Override
    public String toString() {
	return "<fn " + declaration.name.lexeme + ">";
    }
    
    @Override
    public int arity() {
	return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter,
		       List<Object> arguments) {
	// use close to holds on to the surrouding variables.
	Environment environment = new Environment(closure);
	
	// A function encapsulate its parameters;
	// Each function gets its own environment where it stores those variables;
	for (int i = 0; i < declaration.params.size(); i ++) {
	    environment.define(declaration.params.get(i).lexeme,
			       arguments.get(i));
	}

	// interpreter.executeBlock(declaration.body, environment);
	try {
	    interpreter.executeBlock(declaration.body, environment);
	} catch (Return returnValue) {
	    return returnValue.value;
	}
	return null;
    }
}

