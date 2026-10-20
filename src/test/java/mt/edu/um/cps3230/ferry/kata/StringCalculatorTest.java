package mt.edu.um.cps3230.ferry.kata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The kata, one rule at a time, in the order the tests were written. Early
 * tests that became rows of a parameterised test were folded in during a
 * refactoring step.
 */
class StringCalculatorTest {

    private final StringCalculator calculator = new StringCalculator();

    @Test
    void shouldReturnZeroWhenTheStringIsEmpty() {
        assertThat(calculator.add("")).isZero();
    }

    @ParameterizedTest(name = "\"{0}\" adds up to {1}")
    @CsvSource(delimiter = '|', value = {
        "1         | 1",
        "1,2       | 3",
        "1,2,3,4,5 | 15"
    })
    void shouldAddEveryNumberWhenTheyAreSeparatedByCommas(String numbers, int sum) {
        assertThat(calculator.add(numbers)).isEqualTo(sum);
    }

    @Test
    void shouldAcceptANewLineAsASeparatorWhenMixedWithCommas() {
        assertThat(calculator.add("1\n2,3")).isEqualTo(6);
    }

    @Test
    void shouldSplitOnTheNamedSeparatorWhenTheFirstLineNamesOne() {
        assertThat(calculator.add("//;\n1;2")).isEqualTo(3);
    }

    @Test
    void shouldTreatTheSeparatorLiterallyWhenItMeansSomethingToARegularExpression() {
        assertThat(calculator.add("//.\n1.2")).isEqualTo(3);
    }

    @Test
    void shouldRefuseTheStringWhenItHoldsANegativeNumber() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.add("1,-2"))
                .withMessage("negatives not allowed: -2");
    }

    @Test
    void shouldListEveryNegativeWhenThereIsMoreThanOne() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.add("-1,2,-3"))
                .withMessage("negatives not allowed: -1, -3");
    }

    @ParameterizedTest(name = "\"{0}\" adds up to {1}")
    @CsvSource(delimiter = '|', value = {
        "2,1001 | 2",
        "2,1000 | 1002"
    })
    void shouldLeaveOutNumbersAboveAThousandWhenAdding(String numbers, int sum) {
        assertThat(calculator.add(numbers)).isEqualTo(sum);
    }

    @Test
    void shouldSplitOnALongSeparatorWhenItIsInBrackets() {
        assertThat(calculator.add("//[***]\n1***2***3")).isEqualTo(6);
    }

    @Test
    void shouldSplitOnEverySeparatorWhenSeveralAreNamed() {
        assertThat(calculator.add("//[*][%]\n1*2%3")).isEqualTo(6);
    }

    @Test
    void shouldSplitOnEverySeparatorWhenSeveralLongOnesAreNamed() {
        assertThat(calculator.add("//[**][%%]\n1**2%%3")).isEqualTo(6);
    }
}
