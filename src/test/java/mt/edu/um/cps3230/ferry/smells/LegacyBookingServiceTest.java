package mt.edu.um.cps3230.ferry.smells;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemorySailingRepository;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.domain.BookingRequest;
import mt.edu.um.cps3230.ferry.domain.BookingService;
import mt.edu.um.cps3230.ferry.domain.BookingStatus;
import mt.edu.um.cps3230.ferry.domain.Passenger;
import mt.edu.um.cps3230.ferry.domain.PriceCalculator;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingFullException;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The same checks as before, refactored. Each test builds the sailing it needs
 * where the reader can see it, the clock and the references are fixed, and
 * each test checks one behaviour.
 *
 * <pre>
 * Smell                Was                                         Now
 * eager test           one test reserved, confirmed and checked    one test per behaviour
 *                      in, and asserted on all of it
 * assertion roulette   eleven assertions, none with a message      one or two assertions per test,
 *                                                                  each naming what it reads
 * mystery guest        sailings read from sailings.csv, picked     a sailing built in the test,
 *                      by position, 900 places taken on trust      capacity in plain sight
 * conditional logic    a loop over ages, with the fare rules      a parameterised test, one row
 *                      copied into if and else                     per boundary, the fare stated
 * Thread.sleep         a second's wait so that the clock made     references handed out by a
 *                      a new reference                             counter, the clock fixed
 * </pre>
 *
 * The eager test checked what {@code confirm}, {@code checkIn} and
 * {@code cancel} return. A first refactoring checked only what was stored,
 * and PIT showed three mutants it no longer killed; the tests here check both.
 */
class LegacyBookingServiceTest {

    private static final ZoneId MALTA = ZoneId.of("Europe/Malta");
    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 10, 6, 7, 0);
    private static final LocalDate SAILING_DATE = DEPARTURE.toLocalDate();
    private static final String SAILING_ID = "2026-10-06-0700-CIRKEWWA";
    private static final String CARD = "4111111111111111";
    private static final String EMAIL = "someone@example.com";

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final InMemorySailingRepository sailings = new InMemorySailingRepository(bookings);
    private final InMemoryNotificationGateway notifications = new InMemoryNotificationGateway();
    private final AtomicInteger referencesIssued = new AtomicInteger();
    private final BookingService service = new BookingService(
            Clock.fixed(DEPARTURE.minusDays(1).atZone(MALTA).toInstant(), MALTA), sailings, bookings,
            new FakePaymentGateway(), notifications, new PriceCalculator(),
            () -> "FF-" + referencesIssued.incrementAndGet());

    @Test
    void shouldQuoteTwoAdultFaresAndAChildFareWhenAFamilyTravels() {
        sailingWithPlaces(900);
        BookingRequest family = request(aged(40), aged(38), aged(7));

        assertThat(service.quote(family).cents()).isEqualTo(465 + 465 + 115);
    }

    @Test
    void shouldHoldThePlacesWhenASailingIsReserved() {
        sailingWithPlaces(900);

        Booking reserved = service.reserve(request(aged(40), aged(38)));

        assertThat(bookings.findByReference(reserved.reference()))
                .map(Booking::status).contains(BookingStatus.RESERVED);
    }

    @Test
    void shouldKeepThePaymentWhenAReservationIsConfirmed() {
        sailingWithPlaces(900);
        Booking reserved = service.reserve(request(aged(40)));

        Booking confirmed = service.confirm(reserved.reference(), CARD);

        assertThat(confirmed.paymentReference()).isPresent();
        assertThat(bookings.findByReference(reserved.reference())).contains(confirmed);
    }

    @Test
    void shouldTellTheContactWhenABookingIsConfirmed() {
        sailingWithPlaces(900);
        Booking reserved = service.reserve(request(aged(40)));

        service.confirm(reserved.reference(), CARD);

        assertThat(notifications.messagesTo(EMAIL)).singleElement(STRING).contains(reserved.reference());
    }

    @Test
    void shouldCheckInWhenTheBookingIsConfirmed() {
        sailingWithPlaces(900);
        Booking reserved = service.reserve(request(aged(40)));
        service.confirm(reserved.reference(), CARD);

        Booking checkedIn = service.checkIn(reserved.reference());

        assertThat(checkedIn.status()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(service.find(reserved.reference())).isEqualTo(checkedIn);
    }

    @Test
    void shouldRefuseAPassengerWhenEveryPlaceIsTaken() {
        sailingWithPlaces(3);
        service.reserve(request(aged(40), aged(38), aged(7)));

        assertThatExceptionOfType(SailingFullException.class)
                .isThrownBy(() -> service.reserve(request(aged(25))));
    }

    @ParameterizedTest(name = "aged {0} pays {1} cents")
    @CsvSource({
        " 0,   0",
        " 2,   0",
        " 3, 115",
        "11, 115",
        "12, 465",
        "70, 465",
    })
    void shouldChargeTheFareForTheirAgeWhenOnePassengerTravels(int age, long cents) {
        sailingWithPlaces(900);

        assertThat(service.quote(request(aged(age))).cents()).isEqualTo(cents);
    }

    @Test
    void shouldGiveEachBookingItsOwnReferenceWhenTwoAreMade() {
        sailingWithPlaces(900);

        Booking first = service.reserve(request(aged(40)));
        Booking second = service.reserve(request(aged(40)));

        assertThat(List.of(first.reference(), second.reference())).containsExactly("FF-1", "FF-2");
    }

    @Test
    void shouldCancelWhenAskedTheDayBeforeTheSailing() {
        sailingWithPlaces(900);
        Booking reserved = service.reserve(request(aged(40)));

        Booking cancelled = service.cancel(reserved.reference());

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookings.findByReference(reserved.reference())).contains(cancelled);
    }

    private void sailingWithPlaces(int passengers) {
        sailings.save(new Sailing(SAILING_ID, Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE, passengers, 0));
    }

    private static Passenger aged(int years) {
        return new Passenger("Passenger aged " + years, SAILING_DATE.minusYears(years), false);
    }

    private static BookingRequest request(Passenger... passengers) {
        return BookingRequest.forPassengers(SAILING_ID, List.of(passengers), EMAIL);
    }
}
