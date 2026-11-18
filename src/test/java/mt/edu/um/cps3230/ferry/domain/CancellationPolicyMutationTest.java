package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.adapters.weather.FixedWeatherFeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The mutants of {@link CancellationPolicy} that the earlier tests left alive,
 * or never reached. The thin suite decides far from every threshold, so each
 * {@code >=} could become {@code >} unnoticed; and nothing called
 * {@code apply}, so every mutant in it went untested.
 *
 * <pre>
 * Mutant                                         Killed by
 * each of the four &gt;= became &gt;                  a forecast exactly on each threshold
 * apply: call to callOffEveryBooking removed     a cancelled sailing calls the reservation off
 * apply: call to warnEveryContact removed        a sailing at risk tells the contact
 * apply returns null                             apply says what it decided
 * isActive check negated, in either loop         a booking already called off is left alone
 * CONFIRMED check negated                        only a paid booking is refunded
 * refund, save or send removed                   each is checked on its own
 * </pre>
 */
class CancellationPolicyMutationTest {

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final InMemoryNotificationGateway notifications = new InMemoryNotificationGateway();

    @ParameterizedTest(name = "waves {0} m, gusts {1} km/h: {2}")
    @CsvSource({
        "2.5, 0,  CANCELLED",
        "0,   60, CANCELLED",
        "1.8, 0,  AT_RISK",
        "0,   45, AT_RISK",
    })
    void shouldTakeTheHarsherStatusWhenTheForecastIsExactlyOnAThreshold(
            double waveHeightMetres, double windGustKph, SailingStatus expected) {
        assertThat(policyFacing(waveHeightMetres, windGustKph).decide(sailing())).isEqualTo(expected);
    }

    @Test
    void shouldSayWhatItDecidedWhenItActs() {
        assertThat(policyFacing(3.0, 0).apply(sailing())).isEqualTo(SailingStatus.CANCELLED);
    }

    @Test
    void shouldCallTheReservationOffWhenTheSailingIsCancelled() {
        bookings.save(reservationFor(adult()));

        policyFacing(3.0, 0).apply(sailing());

        assertThat(statusOf("FF-0001")).isEqualTo(BookingStatus.CANCELLED);
        assertThat(payments.refunds()).isEmpty();
    }

    @Test
    void shouldRefundWhatWasPaidWhenTheSailingIsCancelled() {
        bookings.save(reservationFor(adult()).confirmedWith("PAY-1"));

        policyFacing(3.0, 0).apply(sailing());

        assertThat(payments.refunds()).containsEntry("PAY-1", PriceCalculator.ADULT_FARE);
    }

    @Test
    void shouldTellTheContactWhenTheSailingIsCancelled() {
        bookings.save(reservationFor(adult()));

        policyFacing(3.0, 0).apply(sailing());

        assertThat(notifications.messagesTo(CONTACT)).singleElement().asString().contains("is cancelled");
    }

    @Test
    void shouldLeaveACalledOffBookingAloneWhenTheSailingIsCancelled() {
        bookings.save(reservationFor(adult()).confirmedWith("PAY-1").cancelled());

        policyFacing(3.0, 0).apply(sailing());

        assertThat(payments.refunds()).isEmpty();
        assertThat(notifications.sentCount()).isZero();
    }

    @Test
    void shouldWarnTheContactAndKeepTheBookingWhenTheSailingIsAtRisk() {
        bookings.save(reservationFor(adult()));

        policyFacing(2.0, 0).apply(sailing());

        assertThat(notifications.messagesTo(CONTACT)).singleElement().asString().contains("may be called off");
        assertThat(statusOf("FF-0001")).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldNotWarnACalledOffBookingWhenTheSailingIsAtRisk() {
        bookings.save(reservationFor(adult()).cancelled());

        policyFacing(2.0, 0).apply(sailing());

        assertThat(notifications.sentCount()).isZero();
    }

    private CancellationPolicy policyFacing(double waveHeightMetres, double windGustKph) {
        return new CancellationPolicy(new FixedWeatherFeed(waveHeightMetres, windGustKph),
                bookings, payments, notifications, new PriceCalculator());
    }

    private BookingStatus statusOf(String reference) {
        Optional<Booking> booking = bookings.findByReference(reference);
        return booking.orElseThrow().status();
    }
}
