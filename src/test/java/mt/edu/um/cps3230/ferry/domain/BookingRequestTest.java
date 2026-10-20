package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CAR;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_ID;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BookingRequestTest {

    @Test
    void shouldCarryNoVehicleWhenOnlyPassengersTravel() {
        assertThat(BookingRequest.forPassengers(SAILING_ID, List.of(adult()), CONTACT).vehicle())
                .isEmpty();
    }

    @Test
    void shouldCarryTheVehicleWhenOneIsBrought() {
        assertThat(BookingRequest.withVehicle(SAILING_ID, List.of(adult()), CAR, CONTACT).vehicle())
                .contains(CAR);
    }

    @Test
    void shouldRefuseWhenNobodyIsTravelling() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BookingRequest.forPassengers(SAILING_ID, List.of(), CONTACT))
                .withMessageContaining("at least one passenger");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "not an address"})
    void shouldRefuseWhenTheContactIsNotAnEmailAddress(String contact) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BookingRequest.forPassengers(SAILING_ID, List.of(adult()), contact))
                .withMessageContaining("contact e-mail address");
    }

    @Test
    void shouldRefuseWhenSomethingItNeedsIsMissing() {
        assertThatNullPointerException().isThrownBy(
                () -> new BookingRequest(null, List.of(adult()), Optional.empty(), CONTACT));
        assertThatNullPointerException().isThrownBy(
                () -> new BookingRequest(SAILING_ID, List.of(adult()), null, CONTACT));
    }

    @Test
    void shouldKeepItsOwnCopyWhenTheListOfPassengersIsChangedAfterwards() {
        List<Passenger> travelling = new java.util.ArrayList<>(List.of(adult()));
        BookingRequest request = BookingRequest.forPassengers(SAILING_ID, travelling, CONTACT);

        travelling.add(adult());

        assertThat(request.passengers()).hasSize(1);
    }
}
