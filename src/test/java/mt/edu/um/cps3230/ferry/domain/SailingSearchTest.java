package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.CONTACT;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.clockAt;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationOn;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailingFrom;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemorySailingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SailingSearchTest {

    private static final LocalDateTime MORNING = SAILING_DATE.atTime(4, 0);
    private static final Sailing EARLY = sailingFrom(Terminal.CIRKEWWA, SAILING_DATE.atTime(5, 45));
    private static final Sailing LATER = sailingFrom(Terminal.CIRKEWWA, SAILING_DATE.atTime(9, 30));

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final InMemorySailingRepository sailings = new InMemorySailingRepository(bookings);
    private final SailingSearch search = new SailingSearch(clockAt(MORNING), sailings);

    @BeforeEach
    void seedTheTimetable() {
        sailings.save(LATER);
        sailings.save(EARLY);
        sailings.save(sailingFrom(Terminal.MGARR, SAILING_DATE.atTime(6, 15)));
        sailings.save(sailingFrom(Terminal.CIRKEWWA, SAILING_DATE.plusDays(1).atTime(5, 45)));
    }

    @Test
    void shouldReturnOnlyTheSailingsLeavingThatTerminalWhenTheDayIsSearched() {
        assertThat(search.on(SAILING_DATE, Terminal.CIRKEWWA))
                .extracting(availability -> availability.sailing().from())
                .containsOnly(Terminal.CIRKEWWA);
    }

    @Test
    void shouldReturnOnlyTheSailingsOnThatDateWhenTheDayIsSearched() {
        assertThat(search.on(SAILING_DATE, Terminal.CIRKEWWA))
                .extracting(availability -> availability.sailing().date())
                .containsOnly(SAILING_DATE);
    }

    @Test
    void shouldPutTheEarliestFirstWhenTheDayIsSearched() {
        assertThat(search.on(SAILING_DATE, Terminal.CIRKEWWA))
                .extracting(availability -> availability.sailing().departsAt())
                .containsExactly(SAILING_DATE.atTime(5, 45), SAILING_DATE.atTime(9, 30));
    }

    @Test
    void shouldReturnNothingWhenNoSailingLeavesThatTerminalThatDay() {
        assertThat(search.on(SAILING_DATE.plusDays(1), Terminal.MGARR)).isEmpty();
    }

    @Test
    void shouldReportEverySeatFreeWhenNothingIsBookedYet() {
        SailingAvailability early = search.on(SAILING_DATE, Terminal.CIRKEWWA).get(0);

        assertThat(early.passengersBooked()).isZero();
        assertThat(early.passengerPlacesLeft()).isEqualTo(early.sailing().passengerCapacity());
        assertThat(early.isFull()).isFalse();
    }

    @Test
    void shouldCountTheBookedPlacesOutWhenSomeoneHoldsThem() {
        bookings.save(new Booking("FF-0001", EARLY, java.util.List.of(adult(), adult()),
                Optional.of(Fixtures.CAR), CONTACT, BookingStatus.CONFIRMED, MORNING,
                Optional.of("PAY-1")));

        SailingAvailability early = search.on(SAILING_DATE, Terminal.CIRKEWWA).get(0);

        assertThat(early.passengersBooked()).isEqualTo(2);
        assertThat(early.vehiclesBooked()).isEqualTo(1);
        assertThat(early.passengerPlacesLeft()).isEqualTo(EARLY.passengerCapacity() - 2);
    }

    @Test
    void shouldGivePlacesBackWhenAReservationWasHeldTooLong() {
        bookings.save(reservationOn(EARLY, "FF-0002", CONTACT,
                MORNING.minus(Booking.RESERVATION_WINDOW).minusMinutes(1), adult()));

        SailingAvailability early = search.on(SAILING_DATE, Terminal.CIRKEWWA).get(0);

        assertThat(early.passengersBooked()).isZero();
    }
}
