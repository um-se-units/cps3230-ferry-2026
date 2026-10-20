package mt.edu.um.cps3230.ferry.kata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** The kata worked through in week 2. The practical sheet gives the rules. */
public class StringCalculator {

    /** A comma or a new line, when the string names no separator of its own. */
    private static final String DEFAULT_SEPARATORS = ",|\n";

    /** A number above this is left out of the sum. */
    private static final int LARGEST_COUNTED = 1000;

    private static final String HEADER = "//";
    private static final Pattern BRACKETED = Pattern.compile("\\[(.+?)]");

    /**
     * Adds the numbers in a string.
     *
     * @param numbers the numbers to add
     * @return their sum
     */
    public int add(String numbers) {
        if (numbers.isEmpty()) {
            return 0;
        }
        String separators = DEFAULT_SEPARATORS;
        String body = numbers;
        if (numbers.startsWith(HEADER)) {
            int endOfHeader = numbers.indexOf('\n');
            separators = separatorsNamedIn(numbers.substring(HEADER.length(), endOfHeader));
            body = numbers.substring(endOfHeader + 1);
        }
        List<Integer> values = Arrays.stream(body.split(separators)).map(Integer::parseInt).toList();
        refuseNegatives(values);
        return values.stream().filter(value -> value <= LARGEST_COUNTED).mapToInt(Integer::intValue).sum();
    }

    /** "{@code ;}" names one separator; "{@code [**][%]}" names two, of any length. */
    private String separatorsNamedIn(String header) {
        if (!header.startsWith("[")) {
            return Pattern.quote(header);
        }
        List<String> separators = new ArrayList<>();
        Matcher bracketed = BRACKETED.matcher(header);
        while (bracketed.find()) {
            separators.add(Pattern.quote(bracketed.group(1)));
        }
        return String.join("|", separators);
    }

    private void refuseNegatives(List<Integer> values) {
        List<Integer> negatives = values.stream().filter(value -> value < 0).toList();
        if (!negatives.isEmpty()) {
            throw new IllegalArgumentException("negatives not allowed: "
                    + negatives.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
    }
}
