package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CAR;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DECLINED_CARD;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.GOOD_CARD;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_ID;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adults;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.clockAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemorySailingRepository;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

    private static final LocalDateTime WELL_BEFORE_DEPARTURE = DEPARTURE.minusHours(3);

    private final InMemorySailingRepository sailings = new InMemorySailingRepository();
    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final InMemoryNotificationGateway notifications = new InMemoryNotificationGateway();
    private final AtomicInteger nextReference = new AtomicInteger();

    @Test
    void shouldHoldPlacesWhenTheSailingHasRoom() {
        seed(Fixtures.sailing());

        Booking booking = serviceAt(WELL_BEFORE_DEPARTURE).reserve(requestFor(adult()));

        assertThat(booking.status()).isEqualTo(BookingStatus.RESERVED);
        assertThat(booking.reference()).isEqualTo("FF-0001");
        assertThat(bookings.findByReference("FF-0001")).contains(booking);
    }

    @Test
    void shouldRefuseWhenTheSailingIsUnknown() {
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> serviceAt(WELL_BEFORE_DEPARTURE)
                        .reserve(BookingRequest.forPassengers("no-such-sailing", List.of(adult()), CONTACT)))
                .withMessageContaining("no sailing with id");
    }

    @Test
    void shouldRefuseWhenTheSailingHasAlreadyLeft() {
        seed(Fixtures.sailing());

        assertThatIllegalStateException()
                .isThrownBy(() -> serviceAt(DEPARTURE).reserve(requestFor(adult())))
                .withMessageContaining("has already left");
    }

    @Test
    void shouldRefuseWhenTheSailingHasNoPassengerPlacesLeft() {
        seed(Fixtures.sailing(2, 140));
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult(), adult()));

        assertThatExceptionOfType(SailingFullException.class)
                .isThrownBy(() -> service.reserve(requestFor(adult())))
                .withMessageContaining("0 passenger places left");
    }

    @Test
    void shouldRefuseWhenTheSailingHasNoVehiclePlacesLeft() {
        seed(Fixtures.sailing(900, 1));
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(BookingRequest.withVehicle(SAILING_ID, List.of(adult()), CAR, CONTACT));

        assertThatExceptionOfType(SailingFullException.class)
                .isThrownBy(() -> service
                        .reserve(BookingRequest.withVehicle(SAILING_ID, List.of(adult()), CAR, CONTACT)))
                .withMessageContaining("no vehicle places left");
    }

    @Test
    void shouldGivePlacesBackWhenAReservationWasHeldTooLong() {
        seed(Fixtures.sailing(2, 140));
        serviceAt(WELL_BEFORE_DEPARTURE).reserve(requestFor(adult(), adult()));

        LocalDateTime afterTheWindow =
                WELL_BEFORE_DEPARTURE.plus(Booking.RESERVATION_WINDOW).plusMinutes(1);
        Booking later = serviceAt(afterTheWindow).reserve(requestFor(adult()));

        assertThat(later.status()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldStillHoldPlacesWhenAReservationIsConfirmed() {
        seed(Fixtures.sailing(2, 140));
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult(), adult()));
        service.confirm("FF-0001", GOOD_CARD);

        LocalDateTime afterTheWindow =
                WELL_BEFORE_DEPARTURE.plus(Booking.RESERVATION_WINDOW).plusMinutes(1);

        assertThatExceptionOfType(SailingFullException.class)
                .isThrownBy(() -> serviceAt(afterTheWindow).reserve(requestFor(adult())));
    }

    @Test
    void shouldConfirmWhenTheCardIsGood() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));

        Booking confirmed = service.confirm("FF-0001", GOOD_CARD);

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.paymentReference()).isPresent();
    }

    @Test
    void shouldTellTheContactWhenTheBookingIsConfirmed() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));

        service.confirm("FF-0001", GOOD_CARD);

        assertThat(notifications.messagesTo(CONTACT)).singleElement()
                .asString().contains("FF-0001", "confirmed");
    }

    @Test
    void shouldLeaveTheBookingReservedWhenTheCardIsTurnedDown() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));

        assertThatExceptionOfType(PaymentDeclinedException.class)
                .isThrownBy(() -> service.confirm("FF-0001", DECLINED_CARD));

        assertThat(bookings.findByReference("FF-0001").orElseThrow().status())
                .isEqualTo(BookingStatus.RESERVED);
        assertThat(notifications.sentCount()).isZero();
    }

    @Test
    void shouldRefuseToConfirmWhenTheReservationWasHeldTooLong() {
        seed(Fixtures.sailing());
        serviceAt(WELL_BEFORE_DEPARTURE).reserve(requestFor(adult()));

        LocalDateTime afterTheWindow =
                WELL_BEFORE_DEPARTURE.plus(Booking.RESERVATION_WINDOW).plusMinutes(1);

        assertThatIllegalStateException()
                .isThrownBy(() -> serviceAt(afterTheWindow).confirm("FF-0001", GOOD_CARD))
                .withMessageContaining("held too long");
    }

    @Test
    void shouldRefuseToConfirmWhenTheBookingIsAlreadyConfirmed() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));
        service.confirm("FF-0001", GOOD_CARD);

        assertThatIllegalStateException()
                .isThrownBy(() -> service.confirm("FF-0001", GOOD_CARD))
                .withMessageContaining("cannot move from CONFIRMED to CONFIRMED");
    }

    @Test
    void shouldNotTakeAnyMoneyWhenTheBookingIsAlreadyCancelled() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));
        service.cancel("FF-0001");

        assertThatIllegalStateException()
                .isThrownBy(() -> service.confirm("FF-0001", GOOD_CARD))
                .withMessageContaining("cannot move from CANCELLED to CONFIRMED");
    }

    @Test
    void shouldCancelWhenTheSailingIsMoreThanTwoHoursAway() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));

        assertThat(service.cancel("FF-0001").status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldRefuseToCancelWhenTheSailingIsLessThanTwoHoursAway() {
        seed(Fixtures.sailing());
        serviceAt(WELL_BEFORE_DEPARTURE).reserve(requestFor(adult()));

        assertThatIllegalStateException()
                .isThrownBy(() -> serviceAt(DEPARTURE.minusMinutes(90)).cancel("FF-0001"))
                .withMessageContaining("can no longer be cancelled");
    }

    @Test
    void shouldRefuseToCancelWhenTheWindowHasJustClosed() {
        seed(Fixtures.sailing());
        serviceAt(WELL_BEFORE_DEPARTURE).reserve(requestFor(adult()));

        LocalDateTime onTheDeadline = DEPARTURE.minus(BookingService.CANCELLATION_WINDOW);

        assertThatIllegalStateException()
                .isThrownBy(() -> serviceAt(onTheDeadline).cancel("FF-0001"));
    }

    @Test
    void shouldCheckInWhenTheBookingIsConfirmed() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));
        service.confirm("FF-0001", GOOD_CARD);

        assertThat(service.checkIn("FF-0001").status()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(bookings.findByReference("FF-0001").orElseThrow().status())
                .isEqualTo(BookingStatus.CHECKED_IN);
    }

    @Test
    void shouldCountOnlyTheBookingsCarryingAVehicleWhenVehiclePlacesAreCounted() {
        seed(Fixtures.sailing(900, 1));
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        service.reserve(requestFor(adult()));
        service.reserve(requestFor(adult()));

        Booking withCar = service
                .reserve(BookingRequest.withVehicle(SAILING_ID, List.of(adult()), CAR, CONTACT));

        assertThat(withCar.vehicle()).contains(CAR);
    }

    @Test
    void shouldReturnTheBookingWhenTheReferenceIsKnown() {
        seed(Fixtures.sailing());
        BookingService service = serviceAt(WELL_BEFORE_DEPARTURE);
        Booking reserved = service.reserve(requestFor(adult()));

        assertThat(service.find("FF-0001")).isEqualTo(reserved);
    }

    @Test
    void shouldRefuseWhenTheBookingReferenceIsUnknown() {
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> serviceAt(WELL_BEFORE_DEPARTURE).find("FF-9999"))
                .withMessageContaining("no booking with reference");
    }

    @Test
    void shouldPriceTheTripWithoutHoldingPlacesWhenAQuoteIsAskedFor() {
        seed(Fixtures.sailing());

        Money quoted = serviceAt(WELL_BEFORE_DEPARTURE).quote(requestFor(adults(10)));

        assertThat(quoted).isEqualTo(Money.ofCents(3953));
        assertThat(bookings.all()).isEmpty();
    }

    private void seed(Sailing sailing) {
        sailings.save(sailing);
    }

    private BookingRequest requestFor(Passenger... passengers) {
        return requestFor(List.of(passengers));
    }

    private BookingRequest requestFor(List<Passenger> passengers) {
        return BookingRequest.forPassengers(SAILING_ID, passengers, CONTACT);
    }

    private BookingService serviceAt(LocalDateTime now) {
        return new BookingService(clockAt(now), sailings, bookings, payments, notifications,
                new PriceCalculator(),
                () -> String.format("FF-%04d", nextReference.incrementAndGet()));
    }
}
