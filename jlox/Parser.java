package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.jlox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}
    
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // This parses a series of statements,
    // as many as it can find until it hits the end of the input.
    List<Stmt> parse() {
	List<Stmt> statements = new ArrayList<>();
	while (!isAtEnd()) {
	    statements.add(declaration());
	}
	return statements;
    }

    private Expr expression() {
	// return equalit();
	return assignment();
    }

    private Stmt declaration(){
	try {
	    if (match(VAR)) return varDeclaration();

	    return statement();
	} catch (ParseError error) {
	    synchronize();
	    return null;
	}
    }

    private Stmt statement() {
	if (match(PRINT)) return printStatement();
	if (match(WHILE)) return whileStatement();
       	if (match(LEFT_BRACE)) return new Stmt.Block(block());
	if (match(FOR)) return forStatement();
	if (match(IF)) return ifStatement();
	return expressionStatement();
    }

    private Stmt forStatement() {
	/*
	    If the token following the ( is a semicolon then the initializer has been omitted. 
	    Otherwise, we check for a var keyword to see if it’s a variable declaration.
	    If neither of those matched, it must be an expression. 
	 */
	consume(LEFT_PAREN, "Expect '(' after 'for' .");
	Stmt initializer;
	if (match(SEMICOLON)) {
	    initializer = null;
	} else if (match(VAR)) {
	    initializer = varDeclaration();
	} else {
	    initializer = expressionStatement();
	}

	/*
	  We look for a semicolon to see if the clause has been omitted.
	 */
	Expr condition = null;
	if (!check(SEMICOLON)) {
	    condition = expression();
	}
	consume(SEMICOLON, "Expect ';' after loop condition.");

	// increment expression
	Expr increment = null;
	if (!check(RIGHT_PAREN)) {
	    increment = expression();
	}
	consume(RIGHT_PAREN, "Expect ')' after for clauses.");

	// loop body
	Stmt body = statement();
	if (increment != null) {
	    body = new Stmt.Block(
		 Arrays.asList(body,
			       new Stmt.Expression(increment)));
	}

	// If the condition is omitted, we jam in true to make an infinite loop.
	if (condition == null) condition = new Expr.Literal(true);
	body = new Stmt.While(condition, body);

	// If there is an initializer, it runs once before the entire loop.
	if (initializer != null) {
	    body = new Stmt.Block(Arrays.asList(initializer, body));
	}
	return body;
    }
    
    /*
        No matter what hack they use to get themselves out of the trouble, they always choose the same interpretation—the else is bound to the nearest if that precedes it.

        Our parser conveniently does that already. Since ifStatement() eagerly looks for an else before returning, the innermost call to a nested series will claim the else clause for itself before returning to the outer if statements.
    */
    private Stmt ifStatement() {
	consume(LEFT_PAREN, "Expect '(' after 'if'.");
	Expr condition = expression();
	consume(RIGHT_PAREN, "Expect ')' after if condition.");

	Stmt thenBranch = statement();
	Stmt elseBranch = null;
	if (match(ELSE)) {
	    elseBranch = statement();
	}

	return new Stmt.If(condition, thenBranch, elseBranch);
    }
    
    private Stmt printStatement() {
	Expr value = expression();
	consume(SEMICOLON, "Expect ';' after value.");
	return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
	Token name = consume(IDENTIFIER, "Expect variable name.");

	Expr initializer = null;
	if (match(EQUAL)) {
	    initializer = expression();
	}

	consume(SEMICOLON, "Expect ';' after variable declaration.");
	return new Stmt.Var(name, initializer);
    }

    // Actually translate straight tot Java.
    private Stmt whileStatement() {
	consume(LEFT_PAREN, "Expect '(' after 'while'.");
	Expr condition = expression();
	consume(RIGHT_PAREN, "Expect ')' after condition.");
	Stmt body = statement();

	return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
	Expr expr = expression();
	consume(SEMICOLON, "Expect ';' after expression.");
	return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
	List<Stmt> statements = new ArrayList<>();

	while (!check(RIGHT_BRACE) && !isAtEnd()) {
	    statements.add(declaration());
	}

	consume(RIGHT_BRACE, "Expect '}' after block.");
	return statements;
    }
    
    private Expr assignment() {
	Expr expr = or();

	if (match(EQUAL)) {
	    Token equals = previous();
	    Expr value = assignment();

	    if (expr instanceof Expr.Variable) {
		Token name = ((Expr.Variable)expr).name;
		return new Expr.Assign(name, value);
	    }
	    error(equals, "Invalid assignment target.");
	}

	return expr;
    }

    private Expr or() {
	Expr expr = and();

	while (match(OR)) {
	    Token operator = previous();
	    Expr right = and();
	    expr = new Expr.Logical(expr, operator, right);
	}

	return expr;
    }

    private Expr and() {
	Expr expr = equality();

	while (match(AND)) {
	    Token operator = previous();
	    Expr right = equality();
	    expr = new Expr.Logical(expr, operator, right);
	}

	return expr;
    }
    
    private Expr equality() {
        Expr expr = comparsion();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparsion();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {	 
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private Expr comparsion() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE))  return new Expr.Literal(true);
        if (match(NIL))   return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

	if (match(IDENTIFIER)) {
	    return new Expr.Variable(previous());
	}
	
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
	
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current ++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
