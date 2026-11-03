package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_ID;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adults;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.clockAt;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemorySailingRepository;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import org.junit.jupiter.api.Test;

/**
 * Every branch of {@link BookingService}, each as the rule it belongs to. Each
 * service gets a clock fixed at the moment the test is about, and they all
 * share one set of fakes, so a later service sees what an earlier one did.
 *
 * <pre>
 * Method   Branch                                Rule
 * reserve  the sailing has left, or not          no booking once it leaves
 * reserve  more passengers than places, or not   a full sailing takes nobody else
 * reserve  a vehicle asked for, or not           foot passengers need no vehicle place
 * reserve  every vehicle place taken, or not     a vehicle needs a place of its own
 * confirm  still reserved, or not                only a reservation is confirmed
 * confirm  held too long, or not                 15 minutes to pay
 * confirm  card turned down, or not              no payment, no confirmation
 * cancel   two hours before departure, or not    the customer's cut-off
 * </pre>
 */
class BookingServiceBranchTest {

    private static final LocalDateTime DAY_BEFORE = DEPARTURE.minusDays(1);
    private static final String CARD = "4111111111111111";
    private static final String DECLINED_CARD = "4111111111110000";

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final InMemorySailingRepository sailings = new InMemorySailingRepository(bookings);
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final InMemoryNotificationGateway notifications = new InMemoryNotificationGateway();
    private final AtomicInteger referencesIssued = new AtomicInteger();

    @Test
    void shouldHoldThePlacesWhenTheSailingHasNotLeft() {
        sailings.save(sailing());

        Booking booking = serviceAt(DEPARTURE.minusMinutes(1)).reserve(footPassengers(2));

        assertThat(booking.status()).isEqualTo(BookingStatus.RESERVED);
        assertThat(bookings.findByReference(booking.reference())).contains(booking);
    }

    @Test
    void shouldRefuseAReservationWhenTheSailingIsLeavingAtThatMoment() {
        sailings.save(sailing());

        assertThatIllegalStateException()
                .isThrownBy(() -> serviceAt(DEPARTURE).reserve(footPassengers(1)))
                .withMessageContaining("has already left");
        assertThat(bookings.all()).isEmpty();
    }

    @Test
    void shouldTakeTheLastPlacesWhenTheyAreExactlyEnough() {
        sailings.save(sailing(3, 0));
        BookingService service = serviceAt(DAY_BEFORE);
        service.reserve(footPassengers(1));

        Booking last = service.reserve(footPassengers(2));

        assertThat(last.status()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldRefuseAReservationWhenItAsksForMorePassengersThanArePlacesLeft() {
        sailings.save(sailing(3, 0));
        BookingService service = serviceAt(DAY_BEFORE);
        service.reserve(footPassengers(2));

        assertThatExceptionOfType(SailingFullException.class)
                .isThrownBy(() -> service.reserve(footPassengers(2)))
                .withMessageContaining("has 1 passenger places left");
    }

    @Test
    void shouldGiveThePlacesBackWhenAReservationIsHeldTooLong() {
        sailings.save(sailing(2, 0));
        serviceAt(DAY_BEFORE).reserve(footPassengers(2));

        Booking next = serviceAt(DAY_BEFORE.plus(Booking.RESERVATION_WINDOW).plusMinutes(1))
                .reserve(footPassengers(2));

        assertThat(next.status()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldCarryAVehicleWhenAVehiclePlaceIsLeft() {
        sailings.save(sailing(10, 1));

        Booking booking = serviceAt(DAY_BEFORE).reserve(withVehicle("ABC 123"));

        assertThat(booking.vehicle()).contains(new Vehicle("ABC 123"));
    }

    @Test
    void shouldRefuseAVehicleWhenEveryVehiclePlaceIsTaken() {
        sailings.save(sailing(10, 1));
        BookingService service = serviceAt(DAY_BEFORE);
        service.reserve(withVehicle("ABC 123"));

        assertThatExceptionOfType(SailingFullException.class)
                .isThrownBy(() -> service.reserve(withVehicle("XYZ 789")))
                .withMessageContaining("no vehicle places left");
    }

    @Test
    void shouldStillTakeFootPassengersWhenEveryVehiclePlaceIsTaken() {
        sailings.save(sailing(10, 1));
        BookingService service = serviceAt(DAY_BEFORE);
        service.reserve(withVehicle("ABC 123"));

        Booking onFoot = service.reserve(footPassengers(1));

        assertThat(onFoot.status()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldConfirmAndTellTheContactWhenTheCardIsAccepted() {
        Booking reserved = reservedAt(DAY_BEFORE);

        Booking confirmed = serviceAt(DAY_BEFORE.plusMinutes(5)).confirm(reserved.reference(), CARD);

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.paymentReference()).isPresent();
        assertThat(bookings.findByReference(reserved.reference())).contains(confirmed);
        assertThat(notifications.messagesTo(CONTACT)).singleElement().asString()
                .contains(reserved.reference(), "confirmed");
    }

    @Test
    void shouldConfirmWhenTheReservationIsExactlyAsOldAsTheWindow() {
        Booking reserved = reservedAt(DAY_BEFORE);

        Booking confirmed = serviceAt(DAY_BEFORE.plus(Booking.RESERVATION_WINDOW))
                .confirm(reserved.reference(), CARD);

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void shouldRefuseToConfirmWhenTheBookingIsAlreadyConfirmed() {
        Booking reserved = reservedAt(DAY_BEFORE);
        BookingService service = serviceAt(DAY_BEFORE.plusMinutes(5));
        service.confirm(reserved.reference(), CARD);

        assertThatIllegalStateException()
                .isThrownBy(() -> service.confirm(reserved.reference(), CARD))
                .withMessageContaining("cannot move from CONFIRMED to CONFIRMED");
        assertThat(notifications.sentCount()).isEqualTo(1);
    }

    @Test
    void shouldRefuseToConfirmWhenTheReservationWasHeldTooLong() {
        Booking reserved = reservedAt(DAY_BEFORE);
        BookingService late = serviceAt(DAY_BEFORE.plus(Booking.RESERVATION_WINDOW).plusSeconds(1));

        assertThatIllegalStateException()
                .isThrownBy(() -> late.confirm(reserved.reference(), CARD))
                .withMessageContaining("held too long");
        assertThat(bookings.findByReference(reserved.reference())).contains(reserved);
    }

    @Test
    void shouldKeepTheReservationAndTellNobodyWhenTheCardIsTurnedDown() {
        Booking reserved = reservedAt(DAY_BEFORE);
        BookingService service = serviceAt(DAY_BEFORE.plusMinutes(5));

        assertThatExceptionOfType(PaymentDeclinedException.class)
                .isThrownBy(() -> service.confirm(reserved.reference(), DECLINED_CARD));
        assertThat(bookings.findByReference(reserved.reference())).contains(reserved);
        assertThat(notifications.sentCount()).isZero();
    }

    @Test
    void shouldCancelWhenTheCustomerAsksBeforeTheCutOff() {
        Booking reserved = reservedAt(DAY_BEFORE);
        LocalDateTime justBeforeCutOff =
                DEPARTURE.minus(BookingService.CANCELLATION_WINDOW).minusMinutes(1);

        Booking cancelled = serviceAt(justBeforeCutOff).cancel(reserved.reference());

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookings.findByReference(reserved.reference())).contains(cancelled);
    }

    @Test
    void shouldRefuseToCancelWhenTheCustomerAsksAtTheCutOff() {
        Booking reserved = reservedAt(DAY_BEFORE);
        BookingService atCutOff = serviceAt(DEPARTURE.minus(BookingService.CANCELLATION_WINDOW));

        assertThatIllegalStateException()
                .isThrownBy(() -> atCutOff.cancel(reserved.reference()))
                .withMessageContaining("can no longer be cancelled");
        assertThat(bookings.findByReference(reserved.reference())).contains(reserved);
    }

    private BookingService serviceAt(LocalDateTime now) {
        return new BookingService(clockAt(now), sailings, bookings, payments, notifications,
                new PriceCalculator(), () -> "FF-%04d".formatted(referencesIssued.incrementAndGet()));
    }

    private Booking reservedAt(LocalDateTime now) {
        sailings.save(sailing());
        return serviceAt(now).reserve(footPassengers(1));
    }

    private static BookingRequest footPassengers(int howMany) {
        return BookingRequest.forPassengers(SAILING_ID, adults(howMany), CONTACT);
    }

    private static BookingRequest withVehicle(String registration) {
        return BookingRequest.withVehicle(SAILING_ID, adults(1), new Vehicle(registration), CONTACT);
    }
}
