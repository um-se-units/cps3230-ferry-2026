package mt.edu.um.cps3230.ferry.adapters.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.domain.BookingStatus;
import mt.edu.um.cps3230.ferry.domain.Passenger;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingAvailability;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcRepositoriesTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 6);
    private static final LocalDateTime DEPARTURE = DATE.atTime(7, 45);
    private static final LocalDateTime RESERVED_AT = DATE.atTime(6, 0);
    private static final LocalDateTime NOTHING_EXPIRED = RESERVED_AT.minusHours(1);

    private static final Sailing SAILING = new Sailing("2026-10-06-0745-CIRKEWWA",
            Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, 900, 140);

    private JdbcSailingRepository sailings;
    private JdbcBookingRepository bookings;

    @BeforeEach
    void openAFreshDatabase() {
        DataSource database = Database.inMemory("test-" + UUID.randomUUID());
        Database.applySchema(database);
        sailings = new JdbcSailingRepository(database);
        bookings = new JdbcBookingRepository(database);
        sailings.save(SAILING);
    }

    @Test
    void shouldReadBackWhatWasStoredWhenASailingIsSaved() {
        assertThat(sailings.findById(SAILING.id())).contains(SAILING);
    }

    @Test
    void shouldFindNothingWhenTheSailingIsNotThere() {
        assertThat(sailings.findById("no-such-sailing")).isEmpty();
    }

    @Test
    void shouldReplaceTheSailingWhenItIsSavedAgain() {
        sailings.save(new Sailing(SAILING.id(), Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, 10, 2));

        assertThat(sailings.findById(SAILING.id()).orElseThrow().passengerCapacity()).isEqualTo(10);
    }

    @Test
    void shouldApplyTheSchemaAgainWithoutComplainingWhenItIsAlreadyThere() {
        DataSource database = Database.inMemory("twice-" + UUID.randomUUID());
        Database.applySchema(database);

        Database.applySchema(database);

        assertThat(new JdbcSailingRepository(database).findById("anything")).isEmpty();
    }

    @Test
    void shouldReadBackEveryPartOfABookingWhenItIsSaved() {
        Booking booking = bookingOn(SAILING, "FF-0001", BookingStatus.CONFIRMED,
                Optional.of(new Vehicle("ABC 123")), Optional.of("PAY-1"),
                new Passenger("Anna", LocalDate.of(1990, 4, 5), false),
                new Passenger("Ben", LocalDate.of(2018, 4, 5), true));
        bookings.save(booking);

        assertThat(bookings.findByReference("FF-0001")).contains(booking);
    }

    @Test
    void shouldKeepThePassengersInOrderWhenABookingIsReadBack() {
        bookings.save(bookingOn(SAILING, "FF-0001", BookingStatus.RESERVED, Optional.empty(),
                Optional.empty(), adult("First"), adult("Second"), adult("Third")));

        assertThat(bookings.findByReference("FF-0001").orElseThrow().passengers())
                .extracting(Passenger::name).containsExactly("First", "Second", "Third");
    }

    @Test
    void shouldReplaceTheBookingWhenItMovesThroughItsLifecycle() {
        Booking reserved = bookingOn(SAILING, "FF-0001", BookingStatus.RESERVED, Optional.empty(),
                Optional.empty(), adult("Anna"));
        bookings.save(reserved);

        bookings.save(reserved.confirmedWith("PAY-9"));

        Booking stored = bookings.findByReference("FF-0001").orElseThrow();
        assertThat(stored.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(stored.paymentReference()).contains("PAY-9");
        assertThat(bookings.findBySailingId(SAILING.id())).hasSize(1);
    }

    @Test
    void shouldFindNothingWhenTheReferenceIsNotThere() {
        assertThat(bookings.findByReference("FF-9999")).isEmpty();
    }

    @Test
    void shouldFindEveryBookingOnTheSailingWhenTheSailingIsAskedFor() {
        bookings.save(bookingOn(SAILING, "FF-0001", BookingStatus.RESERVED, Optional.empty(),
                Optional.empty(), adult("Anna")));
        bookings.save(bookingOn(SAILING, "FF-0002", BookingStatus.CONFIRMED, Optional.empty(),
                Optional.of("PAY-2"), adult("Ben")));

        assertThat(bookings.findBySailingId(SAILING.id()))
                .extracting(Booking::reference).containsExactlyInAnyOrder("FF-0001", "FF-0002");
    }

    @Test
    void shouldReportEveryPlaceFreeWhenNothingIsBookedYet() {
        SailingAvailability found = onlyResult();

        assertThat(found.passengersBooked()).isZero();
        assertThat(found.vehiclesBooked()).isZero();
        assertThat(found.passengerPlacesLeft()).isEqualTo(900);
    }

    @Test
    void shouldCountThePassengersAndTheVehicleWhenPlacesAreHeld() {
        bookings.save(bookingOn(SAILING, "FF-0001", BookingStatus.RESERVED,
                Optional.of(new Vehicle("ABC 123")), Optional.empty(),
                adult("Anna"), adult("Ben")));

        SailingAvailability found = onlyResult();

        assertThat(found.passengersBooked()).isEqualTo(2);
        assertThat(found.vehiclesBooked()).isEqualTo(1);
    }

    @Test
    void shouldLeaveCancelledBookingsOutWhenPlacesAreCounted() {
        bookings.save(bookingOn(SAILING, "FF-0001", BookingStatus.CANCELLED,
                Optional.of(new Vehicle("ABC 123")), Optional.empty(), adult("Anna")));

        assertThat(onlyResult().passengersBooked()).isZero();
        assertThat(onlyResult().vehiclesBooked()).isZero();
    }

    @Test
    void shouldLeaveReservationsHeldTooLongOutWhenPlacesAreCounted() {
        bookings.save(bookingOn(SAILING, "FF-0001", BookingStatus.RESERVED, Optional.empty(),
                Optional.empty(), adult("Anna")));

        List<SailingAvailability> later =
                sailings.search(DATE, Terminal.CIRKEWWA, RESERVED_AT.plusMinutes(1));

        assertThat(later).singleElement()
                .extracting(SailingAvailability::passengersBooked).isEqualTo(0);
    }

    @Test
    void shouldStillCountConfirmedBookingsHoweverOldTheyAreWhenPlacesAreCounted() {
        bookings.save(bookingOn(SAILING, "FF-0001", BookingStatus.CONFIRMED, Optional.empty(),
                Optional.of("PAY-1"), adult("Anna")));

        List<SailingAvailability> later =
                sailings.search(DATE, Terminal.CIRKEWWA, RESERVED_AT.plusDays(1));

        assertThat(later).singleElement()
                .extracting(SailingAvailability::passengersBooked).isEqualTo(1);
    }

    @Test
    void shouldReturnOnlyThatDayAndThatTerminalWhenTheSailingsAreSearched() {
        sailings.save(new Sailing("2026-10-06-0615-MGARR", Terminal.MGARR, Terminal.CIRKEWWA,
                DATE.atTime(6, 15), 900, 140));
        sailings.save(new Sailing("2026-10-07-0745-CIRKEWWA", Terminal.CIRKEWWA, Terminal.MGARR,
                DATE.plusDays(1).atTime(7, 45), 900, 140));

        assertThat(sailings.search(DATE, Terminal.CIRKEWWA, NOTHING_EXPIRED))
                .extracting(found -> found.sailing().id())
                .containsExactly(SAILING.id());
    }

    private SailingAvailability onlyResult() {
        List<SailingAvailability> found =
                sailings.search(DATE, Terminal.CIRKEWWA, NOTHING_EXPIRED);
        assertThat(found).hasSize(1);
        return found.get(0);
    }

    private static Passenger adult(String name) {
        return new Passenger(name, LocalDate.of(1990, 4, 5), false);
    }

    private static Booking bookingOn(Sailing sailing, String reference, BookingStatus status,
                                     Optional<Vehicle> vehicle, Optional<String> payment,
                                     Passenger... passengers) {
        return new Booking(reference, sailing, List.of(passengers), vehicle,
                "traveller@example.com", status, RESERVED_AT, payment);
    }
}
