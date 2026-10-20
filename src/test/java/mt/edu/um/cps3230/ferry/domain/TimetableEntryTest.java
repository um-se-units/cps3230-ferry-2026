package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TimetableEntryTest {

    private static final LocalTime DEPARTURE = LocalTime.of(5, 45);

    @Test
    void shouldRefuseWhenBothEndsOfTheCrossingAreTheSame() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TimetableEntry(Terminal.MGARR, Terminal.MGARR, DEPARTURE,
                        900, 140))
                .withMessageContaining("crosses between the two terminals");
    }

    @Test
    void shouldAllowAnEntryWithNoPlacesLeftToSellWhenItIsBuilt() {
        TimetableEntry soldOut =
                new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, 0, 0);

        assertThat(soldOut.passengerCapacity()).isZero();
    }

    @Test
    void shouldRefuseWhenACapacityIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, -1, 140));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, 900, -1));
    }

    @Test
    void shouldRefuseWhenSomethingItNeedsIsMissing() {
        assertThatNullPointerException().isThrownBy(
                () -> new TimetableEntry(null, Terminal.MGARR, DEPARTURE, 900, 140));
        assertThatNullPointerException().isThrownBy(
                () -> new TimetableEntry(Terminal.CIRKEWWA, null, DEPARTURE, 900, 140));
        assertThatNullPointerException().isThrownBy(
                () -> new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, null, 900, 140));
    }
}
