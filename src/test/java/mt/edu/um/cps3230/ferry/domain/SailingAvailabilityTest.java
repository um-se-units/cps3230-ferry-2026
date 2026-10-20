package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class SailingAvailabilityTest {

    private static final Sailing SAILING = Fixtures.sailing(10, 2);

    @Test
    void shouldTakeWhatIsBookedOffTheCapacityWhenThePlacesLeftAreAskedFor() {
        SailingAvailability availability = new SailingAvailability(SAILING, 4, 1);

        assertThat(availability.passengerPlacesLeft()).isEqualTo(6);
        assertThat(availability.vehiclePlacesLeft()).isEqualTo(1);
        assertThat(availability.isFull()).isFalse();
    }

    @Test
    void shouldBeFullWhenEveryPassengerPlaceIsTaken() {
        assertThat(new SailingAvailability(SAILING, 10, 0).isFull()).isTrue();
    }

    @Test
    void shouldNeverReportFewerThanNoPlacesLeftWhenMoreAreBookedThanTheSailingHolds() {
        SailingAvailability oversold = new SailingAvailability(SAILING, 12, 3);

        assertThat(oversold.passengerPlacesLeft()).isZero();
        assertThat(oversold.vehiclePlacesLeft()).isZero();
        assertThat(oversold.isFull()).isTrue();
    }

    @Test
    void shouldRefuseWhenABookedCountIsNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SailingAvailability(SAILING, -1, 0))
                .withMessageContaining("cannot be negative");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SailingAvailability(SAILING, 0, -1))
                .withMessageContaining("cannot be negative");
    }

    @Test
    void shouldRefuseWhenThereIsNoSailing() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SailingAvailability(null, 0, 0));
    }
}
