package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.aged;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.resident;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The one line rules whose branches no other test takes.
 *
 * <pre>
 * Class                Rule
 * Terminal             each terminal's opposite is the other one
 * SailingAvailability  a sailing is full when no passenger place is left
 * Passenger            an infant is not a child (the age >= 3 half of isChildOn)
 * PriceCalculator      a Gozo resident pays the resident fare
 * </pre>
 */
class SmallRulesBranchTest {

    @ParameterizedTest(name = "the opposite of {0} is {1}")
    @CsvSource({"CIRKEWWA, MGARR", "MGARR, CIRKEWWA"})
    void shouldGiveTheOtherTerminalWhenAskedForTheOpposite(Terminal terminal, Terminal opposite) {
        assertThat(terminal.opposite()).isEqualTo(opposite);
    }

    @ParameterizedTest(name = "{0} of 900 passenger places booked, full: {1}")
    @CsvSource({"899, false", "900, true"})
    void shouldBeFullOnlyWhenNoPassengerPlaceIsLeft(int passengersBooked, boolean full) {
        SailingAvailability availability = new SailingAvailability(sailing(900, 140), passengersBooked, 0);

        assertThat(availability.isFull()).isEqualTo(full);
    }

    @Test
    void shouldNotCountAnInfantAsAChildWhenAskedDirectly() {
        assertThat(aged(2).isChildOn(SAILING_DATE)).isFalse();
    }

    @Test
    void shouldChargeTheResidentFareWhenThePassengerIsAGozoResident() {
        Money price = new PriceCalculator().priceFor(reservationFor(resident()));

        assertThat(price).isEqualTo(Money.ofEuros(1, 15));
    }
}
