package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @Test
    void shouldHoldWholeEuroAndCentsWhenBuiltFromEuros() {
        assertThat(Money.ofEuros(4, 65).cents()).isEqualTo(465);
    }

    @Test
    void shouldAllowBothEndsOfTheCentsRangeWhenBuiltFromEuros() {
        assertThat(Money.ofEuros(4, 0)).isEqualTo(Money.ofCents(400));
        assertThat(Money.ofEuros(4, 99)).isEqualTo(Money.ofCents(499));
    }

    @Test
    void shouldAddUpWhenTwoAmountsAreCombined() {
        assertThat(Money.ofEuros(4, 65).plus(Money.ofEuros(15, 70))).isEqualTo(Money.ofEuros(20, 35));
    }

    @ParameterizedTest(name = "{0} cents less {1} percent is {2} cents")
    @CsvSource({
        "4650, 15, 3953",
        "100, 15, 85",
        "115, 15, 98",
        "1, 50, 1",
        "1000, 0, 1000",
        "1000, 100, 0"
    })
    void shouldRoundHalfUpWhenAPercentageComesOff(long cents, int percent, long expected) {
        assertThat(Money.ofCents(cents).lessPercent(percent)).isEqualTo(Money.ofCents(expected));
    }

    @Test
    void shouldRefuseWhenTheAmountIsNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.ofCents(-1))
                .withMessageContaining("cannot be negative");
    }

    @Test
    void shouldRefuseWhenTheCentsAreNotPartOfAEuro() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.ofEuros(4, 100))
                .withMessageContaining("between 0 and 99");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.ofEuros(4, -1))
                .withMessageContaining("between 0 and 99");
    }

    @Test
    void shouldRefuseWhenThePercentageIsOutOfRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.ofCents(100).lessPercent(101))
                .withMessageContaining("between 0 and 100");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.ofCents(100).lessPercent(-1))
                .withMessageContaining("between 0 and 100");
    }

    @Test
    void shouldOrderByAmountWhenTwoAmountsAreCompared() {
        assertThat(Money.ofEuros(1, 15)).isLessThan(Money.ofEuros(4, 65));
    }

    @Test
    void shouldReadAsEuroAndCentsWhenPrinted() {
        assertThat(Money.ofEuros(4, 65)).hasToString("€4.65");
        assertThat(Money.ofCents(5)).hasToString("€0.05");
    }
}
