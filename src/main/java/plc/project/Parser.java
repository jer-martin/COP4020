package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException { // should just fall all the way down?
        //System.out.println("up top");
        try { // i believe that this should work as a baseline for all of the expression parsing
            return parseLogicalExpression();
        }
        catch (ParseException parseException){
            throw new ParseException("broke up top", tokens.index);
        }
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        //System.out.println("in logical");
        try {
            //System.out.println("inside try");
        Ast.Expression output = parseComparisonExpression(); // this gets the value of left
            //System.out.println(output.toString());
        if (match("&&")) { // matches for equality or logical and
            //System.out.println("matching operator for binary");
            String op = tokens.get(-1).getLiteral();
            System.out.println(op);

            Ast.Expression right = parseComparisonExpression(); //just throws it down the line
            output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
        }
        return output;
        }
        catch (ParseException parseException) {
            throw new ParseException("no match", tokens.index);
        }
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //System.out.println("inside comparison");
        try {
            //System.out.println("inside try");
            Ast.Expression output = parseAdditiveExpression();
            if (match("==")) { // matches for equality or logical and
                //System.out.println("matching operator for binary");
                String op = tokens.get(-1).getLiteral();
                System.out.println(op);

                Ast.Expression right = parseComparisonExpression(); //just throws it down the line
                output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
            }
            return output;
        }
        catch (ParseException parseException) {
            throw new ParseException("failed at comparison", tokens.index);
        }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //System.out.println("inside additive");
        try {
            //System.out.println("inside try");
            Ast.Expression output = parseMultiplicativeExpression();
            if (match("+")) { // matches for equality or logical and
                //System.out.println("matching operator for binary");
                String op = tokens.get(-1).getLiteral();
                System.out.println(op);

                Ast.Expression right = parseComparisonExpression(); //just throws it down the line
                output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
            }
            return output;
        }
        catch (ParseException parseException) {
            throw new ParseException("failed at additive", tokens.index);
        }
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //System.out.println("inside multiplicative");
        try {
            //System.out.println("inside try");
            Ast.Expression output = parsePrimaryExpression();
            if (match("*")) { // matches for equality or logical and
                //System.out.println("matching operator for binary");
                String op = tokens.get(-1).getLiteral();
                System.out.println(op);

                Ast.Expression right = parseComparisonExpression(); //just throws it down the line
                output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
            }
            return output;
        }
        catch (ParseException parseException) {
            throw new ParseException("failed at multiplicative", tokens.index);
        }
    }


    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //System.out.println("inside primary");

        // TODO: add functionality for (logical, comparison, additive, multiplicative) expressions

        // i could make these a switch statement... but will I? who knows........

        if (match("TRUE")) {
            return new Ast.Expression.Literal(true);
        }

        if (match("FALSE")) {
            return new Ast.Expression.Literal(false);
        }

        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        }
        // this may also just be how you do true/false
        // TODO: TRUE/FALSE/NIL

        // THIS IS HARDCODED - MATCH CAN ALSO MATCH TYPES
//        if (match("1")) { // if match digit no decimal >> parse int (immutable?)
//            return new Ast.Expression.Literal(new BigInteger("1"));
//        }
//        if (match("2.0")) { // if match digit w decimal >> parse dec (immutable)
//            return new Ast.Expression.Literal(new BigDecimal("2.0"));
//        }

        if (match(Token.Type.INTEGER)) { // integer located
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }

        if (match(Token.Type.DECIMAL)) { // decimal located
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }

        if (match(Token.Type.CHARACTER)) { // char located
            String out = (tokens.get(-1).getLiteral()); //
            return new Ast.Expression.Literal(out.charAt(1));
        }

        if (match(Token.Type.STRING)) { // string located
            String out = (tokens.get(-1).getLiteral());

            if (out.contains("\\")) { // this means there is an escape
                out = out.replace("\\n", "\n"); // will simply replace the escape with its literal, must add all supported escs
                out = out.replace("\\b", "\b");
                out = out.replace("\\r", "\r");
                out = out.replace("\\t", "\t");
                out = out.replace("\\'", "\'");
                out = out.replace("\\\"", "\"");
                out = out.replace("\\\\", "\\");
            }

            return new Ast.Expression.Literal(out.substring(1, out.length() - 1));
        }

        if (match(Token.Type.IDENTIFIER)) { // id located
            String out = (tokens.get(-1).getLiteral());
            System.out.println("id located");

            if (peek("(")) {
                match("(");

                List<Ast.Expression> args = new ArrayList<Ast.Expression>();

                while (!peek(")")){
                    args.add(parseExpression());
                    if (peek(",")) {
                        match(",");
                        if (peek(")"))
                            throw new ParseException("trailing comma", tokens.get(0).getIndex());
                    }
                }

                match(")");
                return new Ast.Expression.Function(out, args);
            }

            if (peek("[")) {
                match("[");

                List<Ast.Expression> args = new ArrayList<Ast.Expression>(); // i just copied the code from the function lol

                while (!peek("]")){
                    args.add(parseExpression());
                    if (peek(",")) {
                        match(",");
                        if (peek(")"))
                            throw new ParseException("trailing comma", tokens.get(0).getIndex());
                    }
                }
                return new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral())), tokens.get(-3).getLiteral());
            }
            return new Ast.Expression.Access(Optional.empty(), out);
        }




            //throw new ParseException()
        throw new ParseException("Invalid primary expression", tokens.index);

    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) return false;
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) return false;
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
