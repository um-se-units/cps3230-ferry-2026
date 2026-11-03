package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_ID;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Every refusal in the value records is a branch: one test per refusal, and
 * one accepting test for each record that no other test builds.
 *
 * <pre>
 * Record               Refuses
 * Money                a negative amount, cents outside 0 to 99, a percentage outside 0 to 100
 * Sailing              the same terminal at both ends, a negative capacity
 * TimetableEntry       the same terminal at both ends, a negative capacity
 * SailingAvailability  a negative count booked
 * Passenger            a missing or blank name
 * Vehicle              a missing or blank registration
 * BookingRequest       no passengers, a contact that is missing or has no @
 * </pre>
 */
class RefusalBranchTest {

    private static final LocalTime DEPARTURE_TIME = LocalTime.of(7, 45);

    @Test
    void shouldRefuseAnAmountWhenItIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.ofCents(-1));
    }

    @ParameterizedTest(name = "{0} cents")
    @ValueSource(ints = {-1, 100})
    void shouldRefuseEurosAndCentsWhenTheCentsAreOutsideNoughtToNinetyNine(int cents) {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.ofEuros(4, cents));
    }

    @ParameterizedTest(name = "{0}%")
    @ValueSource(ints = {-1, 101})
    void shouldRefuseADiscountWhenThePercentageIsOutsideNoughtToAHundred(int percent) {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.ofEuros(4, 65).lessPercent(percent));
    }

    @Test
    void shouldRefuseASailingWhenItLeavesFromAndArrivesAtTheSameTerminal() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new Sailing(SAILING_ID, Terminal.MGARR, Terminal.MGARR, DEPARTURE, 900, 140));
    }

    @ParameterizedTest(name = "{0} passengers, {1} vehicles")
    @CsvSource({"-1, 140", "900, -1"})
    void shouldRefuseASailingWhenACapacityIsNegative(int passengers, int vehicles) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new Sailing(SAILING_ID, Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, passengers, vehicles));
    }

    @Test
    void shouldAcceptATimetableEntryWhenItCrossesAndNoCapacityIsNegative() {
        TimetableEntry entry = new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE_TIME, 0, 0);

        assertThat(entry.departure()).isEqualTo(DEPARTURE_TIME);
    }

    @Test
    void shouldRefuseATimetableEntryWhenItLeavesFromAndArrivesAtTheSameTerminal() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new TimetableEntry(Terminal.CIRKEWWA, Terminal.CIRKEWWA, DEPARTURE_TIME, 900, 140));
    }

    @ParameterizedTest(name = "{0} passengers, {1} vehicles")
    @CsvSource({"-1, 140", "900, -1"})
    void shouldRefuseATimetableEntryWhenACapacityIsNegative(int passengers, int vehicles) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE_TIME, passengers, vehicles));
    }

    @Test
    void shouldAcceptAvailabilityWhenNothingIsBookedYet() {
        SailingAvailability availability = new SailingAvailability(sailing(), 0, 0);

        assertThat(availability.passengerPlacesLeft()).isEqualTo(900);
        assertThat(availability.vehiclePlacesLeft()).isEqualTo(140);
    }

    @ParameterizedTest(name = "{0} passengers, {1} vehicles booked")
    @CsvSource({"-1, 0", "0, -1"})
    void shouldRefuseAvailabilityWhenACountBookedIsNegative(int passengers, int vehicles) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new SailingAvailability(sailing(), passengers, vehicles));
    }

    @ParameterizedTest(name = "name \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRefuseAPassengerWhenTheNameIsMissingOrBlank(String name) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new Passenger(name, SAILING_DATE.minusYears(30), false));
    }

    @ParameterizedTest(name = "registration \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRefuseAVehicleWhenTheRegistrationIsMissingOrBlank(String registration) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Vehicle(registration));
    }

    @Test
    void shouldAcceptARequestWhenItHasAPassengerAndAnEmailAddress() {
        BookingRequest request = BookingRequest.forPassengers(SAILING_ID, List.of(adult()), CONTACT);

        assertThat(request.contactEmail()).isEqualTo(CONTACT);
    }

    @Test
    void shouldRefuseARequestWhenItHasNoPassengers() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                BookingRequest.forPassengers(SAILING_ID, List.of(), CONTACT));
    }

    @ParameterizedTest(name = "contact \"{0}\"")
    @NullSource
    @ValueSource(strings = {"traveller.example.com"})
    void shouldRefuseARequestWhenTheContactIsNotAnEmailAddress(String contact) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new BookingRequest(SAILING_ID, List.of(adult()), Optional.empty(), contact));
    }
}
