package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * The mutants of {@link Booking} and {@link BookingStatus} that the earlier
 * tests left alive. Each survived because no test tried the move or the
 * status that the mutant lets through.
 *
 * <pre>
 * Mutant                                       Killed by
 * confirmedWith: status check removed          a booking is not confirmed twice
 * cancelled: status check removed              a checked in booking is not cancelled
 * hasExpired: status == RESERVED negated       a paid booking never expires
 * holdsPlacesAt returns true                   a cancelled booking holds no places
 * isActive returns true                        CANCELLED is not active
 * </pre>
 */
class BookingMutationTest {

    private static final LocalDateTime LONG_AFTER_RESERVING = DEPARTURE.minusDays(1).plusHours(1);

    @Test
    void shouldRefuseToConfirmWhenTheBookingIsAlreadyConfirmed() {
        Booking confirmed = reservationFor(adult()).confirmedWith("PAY-1");

        assertThatIllegalStateException()
                .isThrownBy(() -> confirmed.confirmedWith("PAY-2"))
                .withMessageContaining("cannot move from CONFIRMED to CONFIRMED");
    }

    @Test
    void shouldRefuseToCancelWhenTheBookingIsCheckedIn() {
        Booking checkedIn = reservationFor(adult()).confirmedWith("PAY-1").checkedIn();

        assertThatIllegalStateException()
                .isThrownBy(checkedIn::cancelled)
                .withMessageContaining("cannot move from CHECKED_IN to CANCELLED");
    }

    @Test
    void shouldNotExpireWhenTheBookingIsPaidFor() {
        Booking confirmed = reservationFor(adult()).confirmedWith("PAY-1");

        assertThat(confirmed.hasExpired(LONG_AFTER_RESERVING)).isFalse();
    }

    @Test
    void shouldHoldNoPlacesWhenTheBookingIsCancelled() {
        Booking cancelled = reservationFor(adult()).cancelled();

        assertThat(cancelled.holdsPlacesAt(DEPARTURE.minusDays(1))).isFalse();
    }

    @Test
    void shouldNotBeActiveWhenTheStatusIsCancelled() {
        assertThat(BookingStatus.CANCELLED.isActive()).isFalse();
    }
}
