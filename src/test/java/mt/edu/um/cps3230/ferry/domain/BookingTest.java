package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void shouldStartReservedWhenPlacesAreFirstHeld() {
        assertThat(reservationFor(adult()).status()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldCarryThePaymentWhenTheReservationIsConfirmed() {
        Booking confirmed = reservationFor(adult()).confirmedWith("PAY-1");

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.paymentReference()).contains("PAY-1");
    }

    @Test
    void shouldCheckInWhenTheBookingIsConfirmed() {
        Booking checkedIn = reservationFor(adult()).confirmedWith("PAY-1").checkedIn();

        assertThat(checkedIn.status()).isEqualTo(BookingStatus.CHECKED_IN);
    }

    @Test
    void shouldCancelWhenTheBookingIsStillReserved() {
        assertThat(reservationFor(adult()).cancelled().status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldCancelWhenTheBookingIsAlreadyConfirmed() {
        Booking cancelled = reservationFor(adult()).confirmedWith("PAY-1").cancelled();

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.paymentReference()).contains("PAY-1");
    }

    @Test
    void shouldRefuseToCheckInWhenTheBookingIsStillReserved() {
        Booking reserved = reservationFor(adult());

        assertThatIllegalStateException()
                .isThrownBy(reserved::checkedIn)
                .withMessageContaining("cannot move from RESERVED to CHECKED_IN");
    }

    @Test
    void shouldRefuseToConfirmWhenTheBookingIsAlreadyConfirmed() {
        Booking confirmed = reservationFor(adult()).confirmedWith("PAY-1");

        assertThatIllegalStateException().isThrownBy(() -> confirmed.confirmedWith("PAY-2"));
    }

    @Test
    void shouldRefuseEveryMoveWhenTheBookingIsCancelled() {
        Booking cancelled = reservationFor(adult()).cancelled();

        assertThatIllegalStateException().isThrownBy(() -> cancelled.confirmedWith("PAY-1"));
        assertThatIllegalStateException().isThrownBy(cancelled::checkedIn);
        assertThatIllegalStateException().isThrownBy(cancelled::cancelled);
    }

    @Test
    void shouldRefuseToCancelWhenThePassengersHaveCheckedIn() {
        Booking checkedIn = reservationFor(adult()).confirmedWith("PAY-1").checkedIn();

        assertThatIllegalStateException().isThrownBy(checkedIn::cancelled);
    }

    @Test
    void shouldLeaveTheEarlierBookingAloneWhenItMovesOn() {
        Booking reserved = reservationFor(adult());

        reserved.confirmedWith("PAY-1");

        assertThat(reserved.status()).isEqualTo(BookingStatus.RESERVED);
        assertThat(reserved.paymentReference()).isEmpty();
    }

    @Test
    void shouldStillHoldItsPlacesWhenTheWindowHasJustNotPassed() {
        Booking reserved = reservedAt(DEPARTURE.minusHours(3));

        assertThat(reserved.hasExpired(DEPARTURE.minusHours(3).plus(Booking.RESERVATION_WINDOW)))
                .isFalse();
    }

    @Test
    void shouldGiveItsPlacesBackWhenTheWindowHasPassed() {
        Booking reserved = reservedAt(DEPARTURE.minusHours(3));

        assertThat(reserved.hasExpired(
                DEPARTURE.minusHours(3).plus(Booking.RESERVATION_WINDOW).plusSeconds(1))).isTrue();
    }

    @Test
    void shouldNeverExpireWhenTheBookingIsAlreadyConfirmed() {
        Booking confirmed = reservedAt(DEPARTURE.minusHours(3)).confirmedWith("PAY-1");

        assertThat(confirmed.hasExpired(DEPARTURE)).isFalse();
        assertThat(confirmed.holdsPlacesAt(DEPARTURE)).isTrue();
    }

    @Test
    void shouldHoldNoPlacesWhenTheBookingIsCancelled() {
        assertThat(reservationFor(adult()).cancelled().holdsPlacesAt(DEPARTURE)).isFalse();
    }

    @Test
    void shouldRefuseWhenNobodyIsTravelling() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Booking.reservation("FF-0001", sailing(), List.of(),
                        Optional.empty(), CONTACT, DEPARTURE.minusDays(1)))
                .withMessageContaining("at least one passenger");
    }

    @Test
    void shouldRefuseWhenTheContactIsNotAnEmailAddress() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Booking.reservation("FF-0001", sailing(), List.of(adult()),
                        Optional.empty(), "not an address", DEPARTURE.minusDays(1)))
                .withMessageContaining("contact e-mail address");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Booking.reservation("FF-0001", sailing(), List.of(adult()),
                        Optional.empty(), null, DEPARTURE.minusDays(1)))
                .withMessageContaining("contact e-mail address");
    }

    @Test
    void shouldRefuseWhenSomethingItNeedsIsMissing() {
        assertThatNullPointerException().isThrownBy(() -> Booking.reservation(null, sailing(),
                List.of(adult()), Optional.empty(), CONTACT, DEPARTURE.minusDays(1)));
        assertThatNullPointerException().isThrownBy(() -> Booking.reservation("FF-0001", null,
                List.of(adult()), Optional.empty(), CONTACT, DEPARTURE.minusDays(1)));
        assertThatNullPointerException().isThrownBy(() -> Booking.reservation("FF-0001", sailing(),
                List.of(adult()), null, CONTACT, DEPARTURE.minusDays(1)));
        assertThatNullPointerException().isThrownBy(() -> Booking.reservation("FF-0001", sailing(),
                List.of(adult()), Optional.empty(), CONTACT, null));
        assertThatNullPointerException().isThrownBy(() -> new Booking("FF-0001", sailing(),
                List.of(adult()), Optional.empty(), CONTACT, null, DEPARTURE.minusDays(1),
                Optional.empty()));
        assertThatNullPointerException().isThrownBy(() -> new Booking("FF-0001", sailing(),
                List.of(adult()), Optional.empty(), CONTACT, BookingStatus.RESERVED,
                DEPARTURE.minusDays(1), null));
    }

    private static Booking reservedAt(java.time.LocalDateTime moment) {
        return Booking.reservation("FF-0001", sailing(), List.of(adult()), Optional.empty(),
                CONTACT, moment);
    }
}
