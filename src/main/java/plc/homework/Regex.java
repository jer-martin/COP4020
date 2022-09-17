package plc.homework;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("^[A-Za-z0-9._]{2,}@[A-Za-z0-9~]+\\.([A-Za-z0-9-]+\\.)*[a-z]{3}$"),
            ODD_STRINGS = Pattern.compile("^(.|\n)(..|\n.|.\n|\n\n){5,9}$"),
            CHARACTER_LIST = Pattern.compile("\\[(('{1}.{0,1}'{1}){1}(\\,\\s*'{1}.{0,1}'{1})*)*\\]$"),
            DECIMAL = Pattern.compile("^^(\\-*\\+*)(([1-9]+\\d*)|0)\\.{1}\\d+"),
            STRING = Pattern.compile("^\"(?:\\\\[brnt\"'\\\\]|[^\"\\\\]|\\w)*\"$");
}
