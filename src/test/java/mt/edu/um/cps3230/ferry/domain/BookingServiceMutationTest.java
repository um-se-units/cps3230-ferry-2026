package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_ID;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.clockAt;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemorySailingRepository;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import org.junit.jupiter.api.Test;

/**
 * The mutants of {@link BookingService} that the earlier tests left alive, or
 * never reached. Most sit in the capacity check: no test had a vehicle on
 * board, or a reservation that had run out.
 *
 * <pre>
 * Mutant                                           Killed by
 * holdsPlacesAt filter returns true                an expired reservation gives its places back
 * vehicle().isPresent() negated                    foot passengers need no vehicle place
 * vehicle filter returns true or false             a foot passenger does not take a vehicle place;
 *                                                  a vehicle does
 * vehiclesTaken &gt;= capacity: boundary, negated     the last vehicle place is taken, then refused
 * sailingFor, bookingFor: supplier returns null    an unknown sailing or reference says so
 * BookingRequest.withVehicle returns null          the vehicle tests build their request with it
 * </pre>
 */
class BookingServiceMutationTest {

    private static final LocalDateTime DAY_BEFORE = DEPARTURE.minusDays(1);

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final InMemorySailingRepository sailings = new InMemorySailingRepository(bookings);
    private final AtomicInteger referencesIssued = new AtomicInteger();

    @Test
    void shouldGiveThePlacesBackWhenAReservationIsHeldTooLong() {
        sailings.save(sailing(1, 0));
        serviceAt(DAY_BEFORE).reserve(footPassenger());
        LocalDateTime afterTheWindow = DAY_BEFORE.plus(Booking.RESERVATION_WINDOW).plusMinutes(1);

        Booking second = serviceAt(afterTheWindow).reserve(footPassenger());

        assertThat(second.status()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldTakeAFootPassengerWhenTheSailingCarriesNoVehicles() {
        sailings.save(sailing(10, 0));

        Booking booking = serviceAt(DAY_BEFORE).reserve(footPassenger());

        assertThat(booking.status()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void shouldTakeTheLastVehiclePlaceWhenFootPassengersHoldTheRest() {
        sailings.save(sailing(10, 1));
        BookingService service = serviceAt(DAY_BEFORE);
        service.reserve(footPassenger());

        Booking withVehicle = service.reserve(withVehicle("ABC 123"));

        assertThat(withVehicle.vehicle()).isPresent();
    }

    @Test
    void shouldRefuseAVehicleWhenTheLastVehiclePlaceIsTaken() {
        sailings.save(sailing(10, 2));
        BookingService service = serviceAt(DAY_BEFORE);
        service.reserve(withVehicle("ABC 123"));
        service.reserve(withVehicle("DEF 456"));

        assertThatExceptionOfType(SailingFullException.class)
                .isThrownBy(() -> service.reserve(withVehicle("GHI 789")))
                .withMessageContaining("no vehicle places left");
    }

    @Test
    void shouldSayWhichSailingWhenItDoesNotExist() {
        BookingService service = serviceAt(DAY_BEFORE);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> service.reserve(footPassenger()))
                .withMessageContaining(SAILING_ID);
    }

    @Test
    void shouldSayWhichReferenceWhenNoBookingHasIt() {
        BookingService service = serviceAt(DAY_BEFORE);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> service.find("FF-404"))
                .withMessageContaining("FF-404");
    }

    private BookingService serviceAt(LocalDateTime now) {
        return new BookingService(clockAt(now), sailings, bookings, new FakePaymentGateway(),
                new InMemoryNotificationGateway(), new PriceCalculator(),
                () -> "FF-" + referencesIssued.incrementAndGet());
    }

    private static BookingRequest footPassenger() {
        return BookingRequest.forPassengers(SAILING_ID, List.of(adult()), CONTACT);
    }

    private static BookingRequest withVehicle(String registration) {
        return BookingRequest.withVehicle(SAILING_ID, List.of(adult()), new Vehicle(registration), CONTACT);
    }
}
