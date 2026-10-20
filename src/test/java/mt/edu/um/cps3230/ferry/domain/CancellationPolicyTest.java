package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationOn;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;

import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.adapters.weather.FixedWeatherFeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CancellationPolicyTest {

    private static final String OTHER_CONTACT = "second@example.com";

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final InMemoryNotificationGateway notifications = new InMemoryNotificationGateway();

    @ParameterizedTest(name = "waves {0} m and gusts {1} kph mean {2}")
    @CsvSource({
        "0.0,  0.0, SAILS",
        "1.79, 44.9, SAILS",
        "1.8,   0.0, AT_RISK",
        "0.0,  45.0, AT_RISK",
        "2.49, 59.9, AT_RISK",
        "2.5,   0.0, CANCELLED",
        "0.0,  60.0, CANCELLED",
        "4.0,  90.0, CANCELLED"
    })
    void shouldReadTheForecastAgainstTheThresholdsWhenTheSailingIsJudged(
            double waveHeightMetres, double windGustKph, SailingStatus expected) {
        assertThat(policyFacing(waveHeightMetres, windGustKph).decide(sailing())).isEqualTo(expected);
    }

    @Test
    void shouldUseTheThresholdsItWasGivenWhenTheyAreNotTheUsualOnes() {
        CancellationPolicy strict = new CancellationPolicy(new FixedWeatherFeed(1.0, 10),
                bookings, payments, notifications, new PriceCalculator(), 0.9, 60, 0.5, 45);

        assertThat(strict.decide(sailing())).isEqualTo(SailingStatus.CANCELLED);
    }

    @Test
    void shouldCallOffEveryBookingWhenTheSailingIsCancelled() {
        givenAReservationAndAConfirmedBooking();

        policyFacing(3.0, 20).apply(sailing());

        assertThat(bookings.all()).extracting(Booking::status)
                .containsOnly(BookingStatus.CANCELLED);
    }

    @Test
    void shouldReturnTheMoneyWhenACancelledBookingWasAlreadyPaidFor() {
        givenAReservationAndAConfirmedBooking();

        policyFacing(3.0, 20).apply(sailing());

        assertThat(payments.refunds()).containsExactly(
                java.util.Map.entry("PAY-1", Money.ofEuros(4, 65)));
    }

    @Test
    void shouldTellEveryContactWhenTheSailingIsCancelled() {
        givenAReservationAndAConfirmedBooking();

        policyFacing(3.0, 20).apply(sailing());

        assertThat(notifications.recipients()).containsExactlyInAnyOrder(CONTACT, OTHER_CONTACT);
        assertThat(notifications.messagesTo(CONTACT)).singleElement()
                .asString().contains("cancelled");
    }

    @Test
    void shouldLeaveTheBookingsStandingWhenTheSailingIsOnlyAtRisk() {
        givenAReservationAndAConfirmedBooking();

        policyFacing(0.5, 50).apply(sailing());

        assertThat(bookings.all()).extracting(Booking::status)
                .containsExactlyInAnyOrder(BookingStatus.RESERVED, BookingStatus.CONFIRMED);
        assertThat(payments.refunds()).isEmpty();
    }

    @Test
    void shouldWarnEveryContactWhenTheSailingIsAtRisk() {
        givenAReservationAndAConfirmedBooking();

        policyFacing(0.5, 50).apply(sailing());

        assertThat(notifications.recipients()).containsExactlyInAnyOrder(CONTACT, OTHER_CONTACT);
        assertThat(notifications.messagesTo(CONTACT)).singleElement()
                .asString().contains("may be called off");
    }

    @Test
    void shouldTellNobodyWhenTheSeaIsCalm() {
        givenAReservationAndAConfirmedBooking();

        SailingStatus status = policyFacing(0.4, 12).apply(sailing());

        assertThat(status).isEqualTo(SailingStatus.SAILS);
        assertThat(notifications.sentCount()).isZero();
        assertThat(bookings.all()).extracting(Booking::status)
                .containsExactlyInAnyOrder(BookingStatus.RESERVED, BookingStatus.CONFIRMED);
    }

    @Test
    void shouldLeaveAlreadyCancelledBookingsAloneWhenTheSailingIsCancelled() {
        bookings.save(reservationOn(sailing(), "FF-0001", CONTACT, DEPARTURE.minusDays(1), adult())
                .cancelled());

        policyFacing(3.0, 20).apply(sailing());

        assertThat(notifications.sentCount()).isZero();
        assertThat(payments.refunds()).isEmpty();
    }

    @Test
    void shouldLeaveAlreadyCancelledBookingsAloneWhenTheSailingIsAtRisk() {
        bookings.save(reservationOn(sailing(), "FF-0001", CONTACT, DEPARTURE.minusDays(1), adult())
                .cancelled());

        policyFacing(0.5, 50).apply(sailing());

        assertThat(notifications.sentCount()).isZero();
    }

    private void givenAReservationAndAConfirmedBooking() {
        bookings.save(reservationOn(sailing(), "FF-0001", CONTACT, DEPARTURE.minusDays(1), adult())
                .confirmedWith("PAY-1"));
        bookings.save(
                reservationOn(sailing(), "FF-0002", OTHER_CONTACT, DEPARTURE.minusDays(1), adult()));
    }

    private CancellationPolicy policyFacing(double waveHeightMetres, double windGustKph) {
        return new CancellationPolicy(new FixedWeatherFeed(waveHeightMetres, windGustKph), bookings,
                payments, notifications, new PriceCalculator());
    }
}
