package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void shouldCheckInWhenTheBookingIsConfirmed() {
        Booking confirmed = reservationFor(adult()).confirmedWith("PAY-1");

        Booking checkedIn = confirmed.checkedIn();

        assertThat(checkedIn.status()).isEqualTo(BookingStatus.CHECKED_IN);
    }

    @Test
    void shouldRefuseToCheckInWhenTheBookingIsStillReserved() {
        Booking reserved = reservationFor(adult());

        assertThatIllegalStateException()
                .isThrownBy(reserved::checkedIn)
                .withMessageContaining("cannot move from RESERVED to CHECKED_IN");
    }
}
