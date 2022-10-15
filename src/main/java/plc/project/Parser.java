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

    private ParseException errorHandler(String message) {
        if (tokens.has(0)) return new ParseException(message + " at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
        else return new ParseException(message+ " at index " +(tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
    }

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();
        boolean function = false;

        while (tokens.has(0)) {
            if (peek("FUN")) {
                functions.add(parseFunction());
                function = true;
            }
            // TODO: add VAL, LIST, VAR (goes to GLOBAL)
            else if (peek("VAL") || peek("LIST") || peek("VAR")) {
                globals.add(parseGlobal());
                //System.out.println("added global");
                if (function) throw errorHandler("invalid global variable located");
            }
            else throw errorHandler("expected expression");
        }

        return new Ast.Source(globals, functions);

    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if ( peek("LIST") ) {
            return parseList();
        }
        else if (peek("VAL") ) {
            return parseImmutable();
        }
        else if (peek("VAR")) {
            return parseMutable();
        }
        else throw errorHandler("invalid global variable located");
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
        //return new Ast.Global()
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("VAR");
        Ast.Statement.Declaration declaration = parseDeclarationStatement();
        //TODO: add try catch here

        return new Ast.Global(declaration.getName(), true, declaration.getValue());
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
       // throw new UnsupportedOperationException(); //TODO
        match("VAL");
        Ast.Statement.Declaration declaration = parseDeclarationStatement();

        return new Ast.Global(declaration.getName(), false, declaration.getValue());
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("FUN");

        if (match(Token.Type.IDENTIFIER)) { // first match matches name
            //System.out.println("id found after function call");

            String name = tokens.get(-1).getLiteral();

            if (match("(")) { // match first parenthesis
                List<String> parameters = new ArrayList<>();
                List<Ast.Statement> statements = new ArrayList<>();

                while (peek(Token.Type.IDENTIFIER)) { // this gets parameters
                    match(Token.Type.IDENTIFIER);

                    if (peek(",")) match(",");
                    if (!match(",")) {
                        if (!peek(")")) throw errorHandler("expected comma between identifiers");
                    }
                }

                if (!match(")")) throw errorHandler("expected closing parenthesis");

                if (peek("DO")) match("DO");
                else throw errorHandler("expected DO statement");

                while(!peek("END") && tokens.has(0)) { // while not at end and not empty
                    statements.add(parseStatement());
                    //System.out.println("statement added");
                    //System.out.println(statements.get(0));
                }
                //System.out.println(statements.get(0));
                if (peek("END")) {
                    //System.out.println("matching end");
                    match("END");
                }
                if (!tokens.get(-1).getLiteral().equals("END")) throw errorHandler("expected END statement");

                return new Ast.Function(name, parameters, statements);
            }
            else throw errorHandler("expected parenthesis after function identifier");



        }
        else throw errorHandler("expected identifier after function call");


    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
        // this is a group of statements, will be parsed within function
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if (peek("LET")) {
            match("LET");
            return parseDeclarationStatement();
        }

        if (peek("IF")) {
            return parseIfStatement();
        }

        if (peek("WHILE")) {
            return parseWhileStatement();
        }

        if (peek("RETURN")) {
            return parseReturnStatement();
        }

    // TODO: ADD BLOCK SUPPORT TO IF & SWITCH
        Ast.Expression reciever = parseExpression();
        if (peek("=")) { // this is an assignment statement
            match("=");
            Ast.Expression value = parseExpression();
            if (peek(";")) {
                match(";");
                return new Ast.Statement.Assignment(reciever, value);
            }
            else throw errorHandler("expected ; at end of statement");
        }
        else {
            //System.out.println("inside id in parseStatement");
            if (peek(";")) {
                //System.out.println("matched ;");
                match(";");
                return new Ast.Statement.Expression(reciever);
            }
            else throw errorHandler("expected ; at end of statement");
        }
        //else throw new UnsupportedOperationException(); // throws this because im currently only coding assg
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
       // throw new UnsupportedOperationException(); //TODO
        //match("LET");
        if (!match(Token.Type.IDENTIFIER)) {
            throw errorHandler("expected identifier");
            //TODO: handle actual index
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> value = Optional.empty();
        Optional<Object> temp = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
 //           temp = Optional.of(parseExpression());\
        }
        if (!match(";")) {
            throw errorHandler("expected semicolon");
        }
        return new Ast.Statement.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("IF");
        Ast.Expression condition = parseExpression(); // this gets the next expr in line (i tried to use a try catch but that didnt work)
        if (match("DO")) {
            boolean isElse = false;
            List<Ast.Statement> thenStatements = new ArrayList<>();
            List<Ast.Statement> elseStatements = new ArrayList<>();

            while (!match("END") && tokens.has(0)) { // while not end and tokens at current index populated
                if (match("ELSE")) {
                    if (!isElse) isElse = true;
                    else throw errorHandler("too many else statements");
                }
                if (isElse) elseStatements.add(parseStatement()); // if else, add to else list
                else thenStatements.add(parseStatement()); // if not else, add to then list
            }

            if (!tokens.get(-1).getLiteral().equals("END")) throw errorHandler("expected END statement");

            return new Ast.Statement.If(condition, thenStatements, elseStatements);
        }
        throw errorHandler("expected DO statement");
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
        //throw new UnsupportedOperationException(); //TODO
        match("WHILE");
        Ast.Expression condition = parseExpression();
        List<Ast.Statement> statements = new ArrayList<>();
        if (!match("DO")) throw errorHandler("expected DO statement");

        while (!match("END") && tokens.has(0)) { // while not end and tokens at current index populated
            statements.add(parseStatement());
        }

        if (!tokens.get(-1).getLiteral().equals("END")) throw errorHandler("expected END statement");

        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("RETURN");
        Ast.Expression value = parseExpression();
        if (!match(";")) {
            throw errorHandler("expected semicolon");
        }
        return new Ast.Statement.Return(value);
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
            throw errorHandler("invalid expression");
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
        if (match("||") || match("&&")) { // matches for  logical and
            //System.out.println("matching operator for binary");
            String op = tokens.get(-1).getLiteral();
            //System.out.println(op);

            Ast.Expression right = parseExpression(); //just throws it down the line
            output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
        }
        return output;
        }
        catch (ParseException parseException) {
            throw errorHandler("invalid token");
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
            if (match("==") || match("!=") || match("<") || match(">")) { // matches for equality or logical and
                //System.out.println("matching operator for binary");
                String op = tokens.get(-1).getLiteral();
                //System.out.println(op);

                Ast.Expression right = parseExpression(); //just throws it down the line
                output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
            }
            return output;
        }
        catch (ParseException parseException) {
            throw errorHandler("invalid token");
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
            if (match("+") || match("-")) { // matches for + or -
                //System.out.println("matching operator for binary");
                String op = tokens.get(-1).getLiteral();
                //System.out.println(op);

                Ast.Expression right = parseExpression(); //just throws it down the line
                output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
            }
            return output;
        }
        catch (ParseException parseException) {
            throw errorHandler("invalid token");
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
            if (match("*") || match("/") || match("^")) { // matches for mult or div
                //System.out.println("matching operator for binary");
                String op = tokens.get(-1).getLiteral();
                //System.out.println(op);

                Ast.Expression right = parseExpression(); //just throws it down the line
                output = new Ast.Expression.Binary(op, output, right); // this sets output to the binary
            }
            return output;
        }
        catch (ParseException parseException) {
            throw errorHandler("invalid token");
        }
    }


    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        //System.out.println("inside primary");
        if (peek("(")) {
            match("(");
            Ast.Expression.Group group = new Ast.Expression.Group(parseExpression());

            if (peek(")")) {
                match(")");
                return group;
            }
            else throw errorHandler("expected closing parenthesis");
        }



        if (match("TRUE")) {
            return new Ast.Expression.Literal(true);
        }

        if (match("FALSE")) {
            return new Ast.Expression.Literal(false);
        }

        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        }


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
            //System.out.println("id located");

            if (peek("(")) {
                match("(");
                //System.out.println("matched (");
                List<Ast.Expression> args = new ArrayList<Ast.Expression>();

                while (!peek(")")){
                    args.add(parseExpression());
                    if (peek(",")) {
                        match(",");
                        if (peek(")"))
                            throw errorHandler("trailing comma");
                    }
                }

                match(")");
                return new Ast.Expression.Function(out, args);
            }

            if (peek("[")) { // this is access
                match("[");

                List<Ast.Expression> args = new ArrayList<Ast.Expression>(); // i just copied the code from the function lol

                while (!peek("]")){
                    args.add(parseExpression());
                    if (peek(",")) {
                        match(",");
                        if (peek("]"))
                            throw errorHandler("trailing comma");
                    }
                }
                return new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral())), tokens.get(-3).getLiteral());
            }
            return new Ast.Expression.Access(Optional.empty(), out);
        }




            //throw errorHandler()
        throw errorHandler("Invalid primary expression");

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
