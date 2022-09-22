package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }


    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        ArrayList<Token> tokens = new ArrayList<Token>();
        while(chars.has(0)) {
            if(!(match(" ") || match("\b") || match("\n") || match("\r") || match("\t"))) { // checks whitespace
                tokens.add(lexToken());
            }
            else {
                chars.skip();
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */

    public Token lexToken() {
        // TODO: convert to switches -- will be much faster
        if(peek("[A-Za-z_]")) { // checks for alpha only, because identifiers cant begin with a digit
                return lexIdentifier();
        }
        if(peek("[0-9]+") || peek("[\\\\+-]?", "[0-9]+")) { // checks for numbers with and without sign
            return lexNumber();
        }
        if(peek("'")) { // just checks for a single quote
            return lexCharacter();
        }
        if(peek("\"")) { // just checks for a double quote
            return lexString();
        }
        if(peek("([<>!=] '='?|(.))")) { // checks for symbols
            return lexOperator();
        }
        throw new UnsupportedOperationException(); // removing this prevents it from running... no return token
                                                   // if you jump into next lex method (at least immediately)
    }

    public Token lexIdentifier() {
        System.out.println("Identifier located");
        match("[A-Za-z_0-9-]"); // matches for identifier, adding numbers to allow for alphanumeric
        while(match("[A-Za-z_0-9-]")); // steps through all chars, making sure they match
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        if (peek("[\\\\+-]?")) match("[\\\\+-]?"); // takes in sign

        System.out.println("Number located");
        if (peek("0")) { // leading zero check and logic branches
            System.out.println("leading 0 found");
            if (peek("(\\.){1}")) {
                System.out.println("Decimal point found");
                while (match("[0-9]+")) ; // takes in the rest of the digits
                return chars.emit(Token.Type.DECIMAL);
            }
            else {
                System.out.println("No decimal point after leading zero found");
                throw new ParseException("expected decimal point after leading zero", chars.index);
            }
        }

        while (match("[0-9]+")); // no leading zero logic branch
        if (peek("(\\.){1}")) {
            match("(\\.){1}");
            System.out.println("Decimal point found");
            if (peek("[0-9]+")) {
                while (match("[0-9]+")) ; // takes in the rest of the digits
                return chars.emit(Token.Type.DECIMAL);
            }
            else {
                System.out.println("trailing decimal found");
                throw new ParseException("Trailing decimal", chars.index);
            }
        }

        System.out.println("No decimal point found");
        while (match("[0-9]+")) ; // takes in the rest of the digits
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        System.out.println("Char located");
        match("'"); // takes in first quote
        System.out.println("first quote");
        if (peek("\\\\")) { // checks for slash
            match("\\\\");
            lexEscape();
        }
        else if (peek(".{1}")) { //checks everything else
            match(".{1}"); // this causes empty chars to fail, because it is reading in the second quote
                                    // as the char between the two quotes
            System.out.println("non newline");
        }
        System.out.println("about to take in last quote");

        if (peek("'")) match("'"); // takes in closing quote
        else {
            System.out.println("no closing quote found");
            throw new ParseException("expected closing char quote", chars.index);
        }

        System.out.println("last quote");
        System.out.println("Type emitted");
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        match("\"{1}");
        while(match("[A-Za-z_,0-9!\\.]*(\\\\)*\\s*")) { // walks through and brings in all letters & digits
            if (peek("(\\\\)*")) {
                lexEscape();
            }
            if (peek("\\s*")) match("\\s*");
        }
        if (peek("\"{1}")) {
            match("\"{1}");
            return chars.emit(Token.Type.STRING);
        }
        else throw new ParseException("expected closing quote", chars.index);
    }

    public void lexEscape() {
        System.out.println("Escape located");
        if(peek("\\\\")) {
            System.out.println("slash matched");
            match("\\\\");
        }

        if(peek("[brnt\"'\\\\]")) {
            System.out.println("supported escape found");
            match("[brnt\"'\\\\]");
        }
        else {
            System.out.println("unsupported escape");
            throw new ParseException("unsupported escape character", chars.index);
        }
    }

    public Token lexOperator() {
        System.out.println("Operator located");


        if (peek("<","=")) {
            match("<","=");
            System.out.println("match double");
            return chars.emit(Token.Type.OPERATOR);
        }
        else if (peek(">","=")){
            match(">","=");
            System.out.println("match double");
            return chars.emit(Token.Type.OPERATOR);
        }
        else if(peek("!","=")) {
            match("!","=");
            System.out.println("match double");
            return chars.emit(Token.Type.OPERATOR);
        }
        else if(peek("=","=")){
            match("=","=");
            System.out.println("match double");
            return chars.emit(Token.Type.OPERATOR);
        }


        match("([<>!=] '='?|(.))"); // matches for single operator
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {

        for (int i = 0; i < patterns.length; i++ ) {

            if (!chars.has(i) ||
                !String.valueOf(chars.get(i)).matches(patterns[i]) ) {
                return false;
            }
         }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
