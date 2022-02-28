package com.craftinginterpreters.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    
    public static void main(String[] args) throws IOException {
	if (args.length > 1) {
	    System.out.println("Usage: jlox [script]");
	    System.exit(64);
	} else if (args.length == 1) {
	    runFile(args[0]);
	} else {
	    runPrompt();
	}
    }
    
    // This interpreter supports two ways of running code.
    // Start jlox from the command line and give it path to file.
    private static void runFile(String path) throws IOException {
	byte[] bytes = Files.readAllBytes(Paths.get(path));
	run(new String(bytes, Charset.defaultCharset()));
	// Indicate an error in the exit code.
	if (hadError) System.exit(65);
	if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
	InputStreamReader input = new InputStreamReader(System.in);
	BufferedReader reader = new BufferedReader(input);

	// interactive loop
	for (;;) {
	    System.out.print(">");
	    // readLine() : reads a line of input from the user on the command line and returns the result.
	    String line = reader.readLine();
	    // When readLine() returns null, exit the loop
	    if (line == null) break;
	    run(line);
	    // reset hadErroe, if users make a mistake, it should't kill entire session
	    hadError = false;
	}
    }

    // The runPrompt() and the runFile() are wrappered from this core function.
    private static void run(String source) {
    Scanner scanner = new Scanner(source);
	List<Token> tokens = scanner.scanTokens();
	Parser parser = new Parser(tokens);
	List<Stmt> statements = parser.parse();

	// Stop if ther was syntax error/
	if (hadError) return;
	Resolver resolver = new Resolver(interpreter);
	resolver.resolve(statements);
	// Stop if there was a resolution error.
	if (hadError) return;
	interpreter.interpret(statements);
    }
    // error() and report() helper tells the user some syntax error occurred on a given line.
    static void error(int line, String message)  {
	report(line, "", message);
    }

    private static void report(int line, String where,
			       String message) {
	System.err.println("[line " + line + "] error" + where + ": " + message);
	hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
	System.err.println(error.getMessage() +
			   "\n[line " + error.token.line + "]");
	hadRuntimeError = true;
    }

}
