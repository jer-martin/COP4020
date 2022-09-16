package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Symbols in Username","j&son@gmail.com",false),
                Arguments.of("Symbols in Server Name","jason@gm&il.com",false),
                Arguments.of("Symbols before .","jason@gmail&$%.com",false),
                Arguments.of("Symbols in Domain","jason@gmail.c&%$m",false),
                Arguments.of("Leading Digit","13xenn@steam.com",true),
                Arguments.of("Server Name Leading Digit","13xenn@5team.com",true),
                Arguments.of("Digit before .","13xenn@gmail5.com",true),
                Arguments.of("Domain Leading Digit","13xenn@gmail5.5com",false),
                Arguments.of("Legal Character Test","ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.@gmail.com",true),
                Arguments.of("Illegal Character Test","john[];'/\\-=+()*#$%&^*!@gmail.com",false),
                Arguments.of("Illegal Char in Domain","john@gm[]il.com",false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "gasolineeee", true),
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("11 characters with escape", "\n\n\n\n\n\n\n\n\n\n\n", true),
                Arguments.of("Symbols","[]&^%$#@!()_+-='\"",true),
                Arguments.of("13 Digits with Single Digit Escape ","\1\2\3\4\5\6\7\1\2\3\4\5\6",true),
                Arguments.of("21 Characters","Polyentolyekmongtainh",false),
                Arguments.of("19 Characters","Polyentolyekmongtai",true),
                Arguments.of("15 Characters","Michaelchristop",true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Misaligned Whitespace","['a','b', 'c']",true),
                Arguments.of("Open and Close","[]",true),
                Arguments.of("Double Open and Close","[[]]",false),
                Arguments.of("Just Comma","[,]",false),
                Arguments.of("John","john",false),
                Arguments.of("Newlines (escape test)","['\n','\n']", false),
                Arguments.of("Unmatched Bracket","['a', 'b'",false),
                Arguments.of("Parenthesis in place of bracket","('a', 'b', 'c')",false),
                Arguments.of("Leading Digit","3['a', 'b', 'c']",false),
                Arguments.of("Curly braces in place of bracket","{'a', 'b', 'c'}",false),
                Arguments.of("open empty close","[' ']",true),
                Arguments.of("No whitespace","['a','b','c']",true),
                Arguments.of("Trailing comma","['a', 'b', 'c',]",false),
                Arguments.of("open empty close no whitespace","['']",true),
                Arguments.of("double empty no whitespace","['','']",true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }


    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Negative Integer", "-1.0", true),
                Arguments.of("Three Digits After Decimal", "10100.001", true),
                Arguments.of("Zero","0.0", true),
                Arguments.of("long before and after","999999999999999.99999999999999999999999999", true),
                Arguments.of("Integer Without Decimal","1", false),
                Arguments.of("No Leading Zero",".5", false),
                Arguments.of("Period Before Leading Zero",".0.13", false),
                Arguments.of("Leading Plus","+3.30", true),
                Arguments.of("Period Asterisk",".*.*",false),
                Arguments.of("Double Period","0..0", false),
                Arguments.of("Lone Period",".", false),
                Arguments.of("Symbols","()*&^%$#@![]{};':.01",false),
                Arguments.of("Leading zero, int following","0.3",true),
                Arguments.of("trailing zero rest ints","3.345000000000000000",true),
                Arguments.of("leading zero with int before decimal pt","01.03",false),
                Arguments.of("0.0","0.0",true),
                Arguments.of("double lone zero","00.0",false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
            Arguments.of( "Valid Escape" , "\"1\\t2\"" , true ),
            Arguments.of("Empty String","\"\"",true),
            Arguments.of("hello world","\"Hello, World!\"",true),
            Arguments.of("valid escape test","\"\b\n\r\t\'\"",true),
            Arguments.of("mixed symbols and letters","\"/brantly?\"",true),
            Arguments.of("unclosed double quote","\"\"gameplay\"",false),
            Arguments.of("text before quotes","beginning\"\"",false),
            Arguments.of("invalid escape","\"invalid\\escape\"",false),
            Arguments.of("vertical seperator","\"brrantly|\"",true),
            Arguments.of("unterminated","\"unterminated",false),
            Arguments.of("quote then quote escape","\"quote\\\"",false),
            Arguments.of("only terminating quote","beginning\"",false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
