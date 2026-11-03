package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The two compound conditions in {@link Booking}, each with an MC/DC set of
 * three rows, and the refusals in its constructor.
 *
 * <pre>
 * hasExpired:     status is RESERVED (A)  &&  past the 15 minute window (B)
 * holdsPlacesAt:  status is active (A)    &&  not expired (B)
 *
 * For A && B the pairs are (T, T) against (F, T) for A, and against (T, F) for B.
 * </pre>
 *
 * <p>The two conditions in {@code holdsPlacesAt} are coupled: a booking that is
 * not RESERVED never expires, so a cancelled booking always has B true, which
 * is what A's pair needs.
 */
class BookingBranchTest {

    private static final Booking RESERVED = reservationFor(adult());
    private static final LocalDateTime RESERVED_AT = RESERVED.reservedAt();

    @ParameterizedTest(name = "{0}, {1} minutes after reserving, expired: {2}")
    @CsvSource({
        "RESERVED,  16, true",
        "CONFIRMED, 16, false",
        "RESERVED,  15, false"
    })
    void shouldExpireOnlyWhenTheBookingIsStillReservedAndThePaymentWindowHasPassed(
            BookingStatus status, int minutes, boolean expired) {
        Booking booking = in(status);

        assertThat(booking.hasExpired(RESERVED_AT.plusMinutes(minutes))).isEqualTo(expired);
    }

    @ParameterizedTest(name = "{0}, {1} minutes after reserving, holds places: {2}")
    @CsvSource({
        "RESERVED,  15, true",
        "CANCELLED, 15, false",
        "RESERVED,  16, false"
    })
    void shouldHoldPlacesOnlyWhenTheBookingIsActiveAndHasNotExpired(
            BookingStatus status, int minutes, boolean holds) {
        Booking booking = in(status);

        assertThat(booking.holdsPlacesAt(RESERVED_AT.plusMinutes(minutes))).isEqualTo(holds);
    }

    @Test
    void shouldRefuseABookingWhenItHasNoPassengers() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> bookingFor(List.of(), CONTACT))
                .withMessageContaining("at least one passenger");
    }

    @ParameterizedTest(name = "contact \"{0}\"")
    @NullSource
    @ValueSource(strings = {"traveller.example.com"})
    void shouldRefuseABookingWhenTheContactIsNotAnEmailAddress(String contact) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> bookingFor(List.of(adult()), contact))
                .withMessageContaining("contact e-mail address");
    }

    private static Booking in(BookingStatus status) {
        return switch (status) {
            case RESERVED -> RESERVED;
            case CONFIRMED -> RESERVED.confirmedWith("PAY-1");
            case CHECKED_IN -> RESERVED.confirmedWith("PAY-1").checkedIn();
            case CANCELLED -> RESERVED.cancelled();
        };
    }

    private static Booking bookingFor(List<Passenger> passengers, String contact) {
        return Booking.reservation("FF-0001", sailing(), passengers, Optional.empty(), contact,
                DEPARTURE.minusDays(1));
    }
}
