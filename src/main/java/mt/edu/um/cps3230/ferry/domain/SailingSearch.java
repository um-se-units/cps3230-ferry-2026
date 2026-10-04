package mt.edu.um.cps3230.ferry.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import mt.edu.um.cps3230.ferry.ports.SailingRepository;

/**
 * Finds the sailings leaving one terminal on one date, earliest first, with
 * what is already booked on each of them.
 */
public final class SailingSearch {

    private final Clock clock;
    private final SailingRepository sailings;

    public SailingSearch(Clock clock, SailingRepository sailings) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sailings = Objects.requireNonNull(sailings, "sailings");
    }

    /**
     * @param date the date to travel on
     * @param from the terminal to leave from
     * @return the sailings and their occupancy, earliest first
     */
    public List<SailingAvailability> on(LocalDate date, Terminal from) {
        Objects.requireNonNull(date, "a search needs a date");
        Objects.requireNonNull(from, "a search needs a terminal to leave from");
        LocalDateTime reservationsHeldSince =
                LocalDateTime.now(clock).minus(Booking.RESERVATION_WINDOW);
        return sailings.search(date, from, reservationsHeldSince).stream()
                .sorted(Comparator.comparing(availability -> availability.sailing().departsAt()))
                .toList();
    }
}
