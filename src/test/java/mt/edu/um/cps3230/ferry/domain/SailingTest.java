package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class SailingTest {

    @Test
    void shouldTakeItsDateFromItsDepartureWhenTheDateIsAskedFor() {
        assertThat(Fixtures.sailing().date()).isEqualTo(SAILING_DATE);
    }

    @Test
    void shouldRefuseWhenBothEndsOfTheCrossingAreTheSame() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Sailing(SAILING_ID, Terminal.MGARR, Terminal.MGARR,
                        DEPARTURE, 900, 140))
                .withMessageContaining("crosses between the two terminals");
    }

    @Test
    void shouldAllowASailingWithNoPlacesLeftToSellWhenItIsBuilt() {
        Sailing soldOut = new Sailing(SAILING_ID, Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, 0, 0);

        assertThat(soldOut.passengerCapacity()).isZero();
        assertThat(soldOut.vehicleCapacity()).isZero();
    }

    @Test
    void shouldRefuseWhenAPassengerCapacityIsNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Sailing(SAILING_ID, Terminal.CIRKEWWA, Terminal.MGARR,
                        DEPARTURE, -1, 140))
                .withMessageContaining("capacity cannot be negative");
    }

    @Test
    void shouldRefuseWhenAVehicleCapacityIsNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Sailing(SAILING_ID, Terminal.CIRKEWWA, Terminal.MGARR,
                        DEPARTURE, 900, -1))
                .withMessageContaining("capacity cannot be negative");
    }

    @Test
    void shouldRefuseWhenSomethingItNeedsIsMissing() {
        assertThatNullPointerException().isThrownBy(
                () -> new Sailing(null, Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, 900, 140));
        assertThatNullPointerException().isThrownBy(
                () -> new Sailing(SAILING_ID, null, Terminal.MGARR, DEPARTURE, 900, 140));
        assertThatNullPointerException().isThrownBy(
                () -> new Sailing(SAILING_ID, Terminal.CIRKEWWA, null, DEPARTURE, 900, 140));
        assertThatNullPointerException().isThrownBy(
                () -> new Sailing(SAILING_ID, Terminal.CIRKEWWA, Terminal.MGARR, null, 900, 140));
    }

    @Test
    void shouldPointAtTheOtherHarbourWhenATerminalIsAskedForItsOpposite() {
        assertThat(Terminal.CIRKEWWA.opposite()).isEqualTo(Terminal.MGARR);
        assertThat(Terminal.MGARR.opposite()).isEqualTo(Terminal.CIRKEWWA);
    }

    @Test
    void shouldSpellTheHarbourNamesInMalteseWhenTheyAreDisplayed() {
        assertThat(Terminal.CIRKEWWA.displayName()).isEqualTo("Ċirkewwa");
        assertThat(Terminal.MGARR.displayName()).isEqualTo("Mġarr");
    }
}
