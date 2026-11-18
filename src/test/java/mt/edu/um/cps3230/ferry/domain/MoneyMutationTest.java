package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The mutants of {@link Money} that the earlier tests left alive. Most are a
 * {@code <} turned into {@code <=}: the cure is a test on each edge of every
 * range, the lowest value accepted and the first value refused.
 *
 * <pre>
 * Mutant                                    Killed by
 * cents &lt; 0 became cents &lt;= 0               an amount of zero is allowed
 * cents &lt; 0 || cents &gt; 99 boundaries         0 and 99 cents allowed, -1 and 100 refused
 * percent &lt; 0 || percent &gt; 100 boundaries   0% and 100% allowed, -1% and 101% refused
 * compareTo returns 0                       a smaller amount sorts first
 * ofCents returns null                      ofCents gives the amount asked for
 * toString: / and % became *                an amount prints as euro and cents
 * </pre>
 */
class MoneyMutationTest {

    @Test
    void shouldAllowAnAmountWhenItIsZero() {
        assertThat(new Money(0).cents()).isZero();
    }

    @Test
    void shouldRefuseAnAmountWhenItIsOneCentBelowZero() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Money(-1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 99})
    void shouldAllowTheCentsWhenTheyAreAtEitherEndOfTheRange(int cents) {
        assertThat(Money.ofEuros(4, cents).cents()).isEqualTo(400 + cents);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 100})
    void shouldRefuseTheCentsWhenTheyAreJustOutsideTheRange(int cents) {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.ofEuros(4, cents));
    }

    @Test
    void shouldLeaveTheAmountAloneWhenNothingIsTakenOff() {
        assertThat(Money.ofCents(465).lessPercent(0)).isEqualTo(Money.ofCents(465));
    }

    @Test
    void shouldLeaveNothingWhenEverythingIsTakenOff() {
        assertThat(Money.ofCents(465).lessPercent(100)).isEqualTo(Money.ZERO);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void shouldRefuseThePercentageWhenItIsJustOutsideTheRange(int percent) {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.ofCents(465).lessPercent(percent));
    }

    @Test
    void shouldSortTheSmallerAmountFirstWhenTwoAreCompared() {
        assertThat(Money.ofCents(115).compareTo(Money.ofCents(465))).isNegative();
        assertThat(Money.ofCents(465).compareTo(Money.ofCents(115))).isPositive();
    }

    @Test
    void shouldPrintEuroAndCentsWhenTheAmountIsShown() {
        assertThat(Money.ofCents(1570)).hasToString("€15.70");
        assertThat(Money.ofCents(5)).hasToString("€0.05");
    }
}
