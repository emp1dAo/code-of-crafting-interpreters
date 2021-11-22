package com.craftinginterpreters.jlox;

/* 
Lexemes and Tokens

Here's a line of Lox code:

var language = "lox";

scan throuh the list of characters and group them together into the smallest sequences that still represent somethings. Each of these blobs of characters is called a lexme.

For the code above, lexemes includes: {var, language, =, "lox", ;}

When we take the lexeme and bundle it togethrt with that other data, the result is a token.
 */

class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
	this.type = type;
	this.lexeme = lexeme;
	this.literal = literal;
	this.line = line;
    }

    public String toString() {
	return type + " " + lexeme + " " + literal;
    }
}
