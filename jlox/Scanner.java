package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.jlox.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    // start and current filds are offsets that index into the string
    // start points to the first character in the lexeme being scanned.
    private int start = 0;
    // current points at the character currently being considered.
    private int current = 0;
    // line field tracks what source line current is on
    // so we can produce tokens that know their location
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    static {
    keywords = new HashMap<>();
    keywords.put("and",    AND);
    keywords.put("class",  CLASS);
    keywords.put("else",   ELSE);
    keywords.put("false",  FALSE);
    keywords.put("for",    FOR);
    keywords.put("fun",    FUN);
    keywords.put("if",     IF);
    keywords.put("nil",    NIL);
    keywords.put("or",     OR);
    keywords.put("print",  PRINT);
    keywords.put("return", RETURN);
    keywords.put("super",  SUPER);
    keywords.put("this",   THIS);
    keywords.put("true",   TRUE);
    keywords.put("var",    VAR);
    keywords.put("while",  WHILE);
    }
    
    
    Scanner(String source) {
	this.source = source;
    }

    // We store the raw source cod as a simple string.
    // We have a list ready to fill with tokens we're going to generate.
    List<Token> scanTokens() {
	// Scanner works its way through the source code/
	// adding tokens until it runs out of characters.
	while (!isAtEnd()) {
	    // We are at the beginning of the next lexeme.
	    start = current;
	    scanToken();
	}
	
	// appends one final "end of file" token.
	tokens.add(new Token(EOF, "", null, line));
	return tokens;
    }

    // recognizing lexemes
    // each turn of the loop, we scan a single token.
    private void scanToken() {
	char c = advance();
	switch (c) {
	case '(': addToken(LEFT_PAREN);                     break;
	case ')': addToken(RIGHT_PAREN);                    break;
	case '{': addToken(LEFT_BRACE);                     break;
	case '}': addToken(RIGHT_BRACE);                    break;
	case ',': addToken(COMMA);                          break;
	case '.': addToken(DOT);                            break;
	case '-': addToken(MINUS);                          break;
	case '+': addToken(PLUS);                           break;
	case ';': addToken(SEMICOLON);                      break;
	case '*': addToken(STAR);                           break;
	// for operators such as: !=, >=, <=, ==
	// we need to look at the second character
	case '!':
	    addToken(match('=') ? BANG_EQUAL : BANG);       break;
	case '=':
	  addToken(match('=') ? EQUAL_EQUAL : EQUAL);       break;
	case '<':
	    addToken(match('=') ? LESS_EQUAL : LESS);       break;
	case '>':
	    addToken(match('=') ? GREATER_EQUAL : GREATER); break;
	// comment begin with a slash too.
	case '/':
	    if (match('/')) {
		// a comment goes until the end of the line.
		while (peek() != '\n' && !isAtEnd()) advance();
		// Comments aren't meaningful, when reach the end of the comment we don't call addToken().
	    } else if (match('*')) {
		blockComment();
	    } else {
		addToken(SLASH);
	    }                                               break;
	case ' ':
	case '\r':
	case '\t':
	    // Ignore whitespace
	                                                    break;
	case '\n':
	    line ++;                                        break;
	// Tackle literals
	case '"':
	    string();                                       break;
	default:
	    if (isDigit(c)) {
		number();
	    } else if (isAlpha(c)) {
		identifier();
	    } else {
		Lox.error(line, "Unexpected character.");
	    }                                               break;
	}
    }

    private void identifier() {
	while (isAlphaNumeric(peek())) advance();

	// After scan an identifier, check to see if it matches anything in map.
	String text = source.substring(start, current);
	TokenType type = keywords.get(text);
	// If so, we use that keyword’s token type.
	// Otherwise, it’s a regular user-defined identifier.
	if (type == null) type = IDENTIFIER;
	addToken(IDENTIFIER);
    }

    private void number() {
	while (isDigit(peek())) advance();

	// Look for a fractional part.
	if (peek() == '.' && isDigit(peekNext())) {
	    // consume the "."
	    advance();
	    while (isDigit(peek())) advance();
	}

	addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	
    }
    
    private void string() {
       while (peek() != '"' && !isAtEnd()) {
	   if (peek() == '\n') line ++;
	   advance();
       }

       if (isAtEnd()) {
	   Lox.error(line, "Unterminated string.");
	   return;
       }

       // The closing ".
       advance();

       //Trim the surrounding quote.
       String value = source.substring(start + 1, current - 1);
       addToken(STRING, value);
   }

    // It’s like a conditional advance().
    // We only consume the current character if it’s what we’re looking for.
    private boolean match(char expected) {
	if (isAtEnd()) return false;
	if (source.charAt(current) != expected) return false;
	current ++;
	return true;
    }

    // lookahead
    // It's sort of like advance(), but doesn't consume the character.
    // It only looks at the current unconsumed character.
    private char peek() {
	if (isAtEnd()) return '\0';
	return source.charAt(current);
    }

    private char peekNext() {
	if (current + 1 >= source.length()) return '\0';
	return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
	return ('a' <= c && c <= 'z') ||
	       ('A' <= c && c <= 'Z') ||
	       c == '_';
    }

    private boolean isAlphaNumeric(char c) {
	return isAlpha(c) || isDigit(c);
    }
    
    private boolean isDigit(char c) {
	return '0' <= c && c <= '9';
    }

    // isAtEnd() tells us if we're consumed all the characters.
    private boolean isAtEnd() {
	return current >= source.length();
    }

    // advance() is for "input"
    // advance() consumes the next character in the source file and returns it
    private char advance() {
	return source.charAt(current++);
    }

    // addToken() is for "output"
    // addToken() grabs the text of the current lexeme and creats a new token for it.
    private void addToken(TokenType type) {
	addToken(type, null);
    }
    
    private void addToken(TokenType type, Object literal) {
	String text = source.substring(start, current);
	tokens.add(new Token(type, text, literal, line));
    }

    // handle '/* ... */'
    private void blockComment() {
	while (peek() != '*' && !isAtEnd()) {
	    // when encounter '\n', need to add line
	    if (peek() == '\n') ++line;
	    // handle '*/'
	    if (peek() == '/') {
		if (peekNext() == '*') {
		    advance();
		    advance();
		    blockComment();
		}
	    }
	    advance();
	}
	
	if (isAtEnd()) {
	    Lox.error(line, "Unterminated block comment");
	}

	advance();

	if (!match('/')) {
	    advance();
	    blockComment();
	}
    }
}
