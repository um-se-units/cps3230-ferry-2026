package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_ID;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.aged;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The mutants of the value records that the earlier tests left alive, or
 * never reached. Every one is an edge: an age on a birthday, a capacity of
 * exactly zero, a sailing with exactly no places left.
 *
 * <pre>
 * Mutant                                        Killed by
 * Passenger: age &lt; 3, &gt;= 3, &lt; 12 boundaries     ages 2, 3, 11 and 12
 * Sailing, TimetableEntry: capacity &lt; 0         a capacity of zero allowed, of -1 refused
 * SailingAvailability: never reached            places left, never below zero; full or not;
 *                                               a count of zero allowed, of -1 refused
 * </pre>
 */
class ValueMutationTest {

    @ParameterizedTest(name = "aged {0}: infant {1}, child {2}")
    @CsvSource({
        " 2, true,  false",
        " 3, false, true",
        "11, false, true",
        "12, false, false",
    })
    void shouldPlaceThePassengerInTheRightBandWhenTheyAreOnABirthday(
            int years, boolean infant, boolean child) {
        Passenger passenger = aged(years);

        assertThat(passenger.isInfantOn(SAILING_DATE)).isEqualTo(infant);
        assertThat(passenger.isChildOn(SAILING_DATE)).isEqualTo(child);
    }

    @Test
    void shouldAllowASailingWhenItTakesNoPassengersAndNoVehicles() {
        Sailing empty = sailing(0, 0);

        assertThat(empty.passengerCapacity()).isZero();
        assertThat(empty.vehicleCapacity()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1"})
    void shouldRefuseASailingWhenACapacityIsNegative(int passengers, int vehicles) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Sailing(SAILING_ID,
                Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, passengers, vehicles));
    }

    @Test
    void shouldAllowATimetableEntryWhenItTakesNoPassengersAndNoVehicles() {
        TimetableEntry entry = new TimetableEntry(Terminal.MGARR, Terminal.CIRKEWWA, LocalTime.NOON, 0, 0);

        assertThat(entry.passengerCapacity()).isZero();
        assertThat(entry.vehicleCapacity()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1"})
    void shouldRefuseATimetableEntryWhenACapacityIsNegative(int passengers, int vehicles) {
        assertThatIllegalArgumentException().isThrownBy(() -> new TimetableEntry(
                Terminal.MGARR, Terminal.CIRKEWWA, LocalTime.NOON, passengers, vehicles));
    }

    @Test
    void shouldCountThePlacesLeftWhenSomeAreBooked() {
        SailingAvailability availability = new SailingAvailability(sailing(10, 4), 7, 1);

        assertThat(availability.passengerPlacesLeft()).isEqualTo(3);
        assertThat(availability.vehiclePlacesLeft()).isEqualTo(3);
        assertThat(availability.isFull()).isFalse();
    }

    @Test
    void shouldBeFullWhenEveryPassengerPlaceIsBooked() {
        SailingAvailability availability = new SailingAvailability(sailing(10, 4), 10, 0);

        assertThat(availability.isFull()).isTrue();
    }

    @Test
    void shouldAllowAnAvailabilityWhenNothingIsBooked() {
        SailingAvailability availability = new SailingAvailability(sailing(10, 4), 0, 0);

        assertThat(availability.passengerPlacesLeft()).isEqualTo(10);
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1"})
    void shouldRefuseAnAvailabilityWhenABookedCountIsNegative(int passengers, int vehicles) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SailingAvailability(sailing(10, 4), passengers, vehicles));
    }
}
