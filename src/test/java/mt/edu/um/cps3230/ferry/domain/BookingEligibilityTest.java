package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Who can book: one test for each partition of the inputs that a passenger,
 * a vehicle and a booking request accept or refuse.
 *
 * <pre>
 * Input                 Accepted                Refused
 * passenger name        any text                missing, empty, only spaces
 * date of birth         any date                missing
 * registration          any text                missing, empty, only spaces
 * passengers            one or more             none
 * contact address       contains @              missing, no @
 * </pre>
 */
class BookingEligibilityTest {

    private static final LocalDate BORN = LocalDate.of(1990, 4, 5);
    private static final Passenger ANNA = new Passenger("Anna", BORN, false);
    private static final String SAILING_ID = "2026-10-13-0745-CIRKEWWA";

    @Test
    void shouldAcceptAPassengerWhenTheyHaveANameAndADateOfBirth() {
        assertThat(new Passenger("Anna", BORN, false).name()).isEqualTo("Anna");
    }

    @ParameterizedTest(name = "name \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRefuseAPassengerWhenTheNameIsBlank(String name) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Passenger(name, BORN, false));
    }

    @Test
    void shouldRefuseAPassengerWhenTheDateOfBirthIsMissing() {
        assertThatNullPointerException().isThrownBy(() -> new Passenger("Anna", null, false));
    }

    @Test
    void shouldAcceptAVehicleWhenItHasARegistration() {
        assertThat(new Vehicle("ABC 123").registration()).isEqualTo("ABC 123");
    }

    @ParameterizedTest(name = "registration \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRefuseAVehicleWhenTheRegistrationIsBlank(String registration) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Vehicle(registration));
    }

    @Test
    void shouldAcceptARequestWhenOnePassengerAndAnAddressAreGiven() {
        BookingRequest request = BookingRequest.forPassengers(SAILING_ID, List.of(ANNA), "anna@example.com");

        assertThat(request.passengers()).containsExactly(ANNA);
    }

    @Test
    void shouldRefuseARequestWhenNobodyIsTravelling() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BookingRequest.forPassengers(SAILING_ID, List.of(), "anna@example.com"));
    }

    @ParameterizedTest(name = "contact \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"anna.example.com"})
    void shouldRefuseARequestWhenTheContactIsNotAnAddress(String contact) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BookingRequest.forPassengers(SAILING_ID, List.of(ANNA), contact));
    }
}
