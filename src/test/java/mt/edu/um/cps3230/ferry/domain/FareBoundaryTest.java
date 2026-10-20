package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Partitions and boundaries of the fare rules. Every expected price is worked
 * out from the rules on the practical sheet.
 *
 * <pre>
 * Input                          Partitions                       Boundaries
 * age on the sailing date        infant 0-2, child 3-11, 12+      2|3, 11|12
 * birthday and sailing date      had it, not yet                  the day before
 * residency                      resident, not resident           (none)
 * resident's age                 infant, 3 or over                2|3
 * fare paying passengers         1-9, 10 or more                  9|10
 * infants in a group             count towards the ten or not     9 fare paying + 1 infant
 * vehicle                        none, one                        (none)
 * </pre>
 */
class FareBoundaryTest {

    private static final LocalDate SAILING = LocalDate.of(2026, 10, 13);

    private final PriceCalculator calculator = new PriceCalculator();

    @ParameterizedTest(name = "a passenger who has just turned {0} pays {1} cents")
    @CsvSource({
        "0, 0",
        "2, 0",
        "3, 115",
        "11, 115",
        "12, 465",
        "64, 465"
    })
    void shouldChargeByAgeBandWhenThePassengerHasJustHadABirthday(int age, long cents) {
        Passenger passenger = bornOn(SAILING.minusYears(age), false);

        assertThat(priceOf(List.of(passenger))).isEqualTo(Money.ofCents(cents));
    }

    @ParameterizedTest(name = "a passenger who turns {0} the day after sailing pays {1} cents")
    @CsvSource({
        "3, 0",
        "12, 115"
    })
    void shouldChargeTheYoungerBandWhenTheBirthdayFallsTheDayAfterTheSailing(int turning, long cents) {
        Passenger passenger = bornOn(SAILING.minusYears(turning).plusDays(1), false);

        assertThat(priceOf(List.of(passenger))).isEqualTo(Money.ofCents(cents));
    }

    @ParameterizedTest(name = "a Gozo resident aged {0} pays {1} cents")
    @CsvSource({
        "3, 115",
        "7, 115",
        "40, 115"
    })
    void shouldChargeTheResidentFareWhenTheResidentIsThreeOrOver(int age, long cents) {
        Passenger resident = bornOn(SAILING.minusYears(age), true);

        assertThat(priceOf(List.of(resident))).isEqualTo(Money.ofCents(cents));
    }

    @Test
    void shouldLetAResidentTravelFreeWhenTheResidentIsAnInfant() {
        Passenger residentInfant = bornOn(SAILING.minusYears(2), true);

        assertThat(priceOf(List.of(residentInfant))).isEqualTo(Money.ZERO);
    }

    @ParameterizedTest(name = "{0} adults and {1} infants pay {2} cents")
    @CsvSource({
        "9, 0, 4185",
        "10, 0, 3953",
        "11, 0, 4348",
        "9, 1, 4185",
        "9, 3, 4185",
        "10, 1, 3953"
    })
    void shouldCountOnlyFarePayingPassengersWhenDecidingOnTheGroupRate(int adults, int infants, long cents) {
        List<Passenger> party = new ArrayList<>(adults(adults));
        IntStream.range(0, infants).forEach(index -> party.add(bornOn(SAILING.minusYears(1), false)));

        assertThat(priceOf(party)).isEqualTo(Money.ofCents(cents));
    }

    @Test
    void shouldChargeTheVehicleInFullWhenTheGroupRateApplies() {
        Money price = calculator.priceFor(SAILING, adults(10), true);

        assertThat(price).isEqualTo(Money.ofCents(3953 + 1570));
    }

    @Test
    void shouldAddTheVehicleFareWhenTheBookingCarriesAVehicle() {
        Money price = calculator.priceFor(SAILING, adults(1), true);

        assertThat(price).isEqualTo(Money.ofCents(465 + 1570));
    }

    private Money priceOf(List<Passenger> passengers) {
        return calculator.priceFor(SAILING, passengers, false);
    }

    private static List<Passenger> adults(int howMany) {
        return IntStream.range(0, howMany).mapToObj(index -> bornOn(SAILING.minusYears(40), false)).toList();
    }

    private static Passenger bornOn(LocalDate dateOfBirth, boolean gozoResident) {
        return new Passenger("Passenger born " + dateOfBirth, dateOfBirth, gozoResident);
    }
}
