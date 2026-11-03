package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.adapters.weather.FixedWeatherFeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What {@link CancellationPolicy#apply} does with each status, over a sailing
 * with one booking of each kind: held, paid for, and already called off.
 *
 * <pre>
 * Branch                            Taken by
 * status CANCELLED, AT_RISK, SAILS  one test each, at least
 * a booking already called off      skipped, on CANCELLED and on AT_RISK
 * a booking paid for                refunded; a held one is not
 * </pre>
 */
class CancellationPolicyApplyTest {

    private static final String HELD = "held@example.com";
    private static final String PAID = "paid@example.com";
    private static final String CALLED_OFF = "called.off@example.com";

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final InMemoryNotificationGateway notifications = new InMemoryNotificationGateway();

    @BeforeEach
    void bookOneOfEachKind() {
        bookings.save(bookingFor("FF-HELD", HELD));
        bookings.save(bookingFor("FF-PAID", PAID).confirmedWith("PAY-PAID"));
        bookings.save(bookingFor("FF-CALLED-OFF", CALLED_OFF).confirmedWith("PAY-CALLED-OFF").cancelled());
    }

    @Test
    void shouldCallOffEveryBookingWhenTheSailingIsCancelled() {
        SailingStatus status = policyFacing(3.0, 20).apply(sailing());

        assertThat(status).isEqualTo(SailingStatus.CANCELLED);
        assertThat(bookings.all()).extracting(Booking::status).containsOnly(BookingStatus.CANCELLED);
    }

    @Test
    void shouldRefundOnlyTheBookingStillPaidForWhenTheSailingIsCancelled() {
        policyFacing(3.0, 20).apply(sailing());

        assertThat(payments.refunds()).containsExactly(Map.entry("PAY-PAID", Money.ofEuros(4, 65)));
    }

    @Test
    void shouldTellOnlyTheContactsStillBookedWhenTheSailingIsCancelled() {
        policyFacing(3.0, 20).apply(sailing());

        assertThat(notifications.recipients()).containsExactlyInAnyOrder(HELD, PAID);
        assertThat(notifications.messagesTo(PAID)).singleElement().asString()
                .contains("is cancelled", "FF-PAID");
    }

    @Test
    void shouldWarnOnlyTheContactsStillBookedWhenTheSailingIsAtRisk() {
        SailingStatus status = policyFacing(2.0, 20).apply(sailing());

        assertThat(status).isEqualTo(SailingStatus.AT_RISK);
        assertThat(notifications.recipients()).containsExactlyInAnyOrder(HELD, PAID);
        assertThat(notifications.messagesTo(HELD)).singleElement().asString()
                .contains("may be called off", "FF-HELD");
    }

    @Test
    void shouldLeaveEveryBookingAndPaymentAloneWhenTheSailingIsAtRisk() {
        List<Booking> before = bookings.all();

        policyFacing(2.0, 20).apply(sailing());

        assertThat(bookings.all()).containsExactlyInAnyOrderElementsOf(before);
        assertThat(payments.refunds()).isEmpty();
    }

    @Test
    void shouldChangeNothingAndTellNobodyWhenTheSailingSails() {
        List<Booking> before = bookings.all();

        SailingStatus status = policyFacing(1.0, 20).apply(sailing());

        assertThat(status).isEqualTo(SailingStatus.SAILS);
        assertThat(bookings.all()).containsExactlyInAnyOrderElementsOf(before);
        assertThat(payments.refunds()).isEmpty();
        assertThat(notifications.sentCount()).isZero();
    }

    private CancellationPolicy policyFacing(double waveHeightMetres, double windGustKph) {
        return new CancellationPolicy(new FixedWeatherFeed(waveHeightMetres, windGustKph),
                bookings, payments, notifications, new PriceCalculator());
    }

    private static Booking bookingFor(String reference, String contact) {
        return Booking.reservation(reference, sailing(), List.of(adult()), Optional.empty(), contact,
                DEPARTURE.minusDays(1));
    }
}
