package com.craftinginterpreters.jlox;

import java.util.List;

import static com.craftinginterpreters.jlox.TokenType.*;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
	this.tokens = tokens;
    }

    private Expr expression() {
	return equality();
    }

    private Expr equlity() {
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

    private boolean check(TokenType type) {
	if (isAtend()) return false;
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
}
