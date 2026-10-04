package mt.edu.um.cps3230.ferry.adapters.inmemory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingAvailability;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.ports.BookingRepository;
import mt.edu.um.cps3230.ferry.ports.SailingRepository;

/**
 * Keeps sailings in a map, for tests and for running without a database. It
 * reads the bookings it is given to work out what is left on a sailing; the
 * database adapter asks one database for both.
 */
public final class InMemorySailingRepository implements SailingRepository {

    private final Map<String, Sailing> sailings = new ConcurrentHashMap<>();
    private final BookingRepository bookings;

    /** A repository with nothing booked on any sailing. */
    public InMemorySailingRepository() {
        this(new InMemoryBookingRepository());
    }

    public InMemorySailingRepository(BookingRepository bookings) {
        this.bookings = bookings;
    }

    public InMemorySailingRepository(BookingRepository bookings, List<Sailing> sailings) {
        this(bookings);
        saveAll(sailings);
    }

    @Override
    public Optional<Sailing> findById(String id) {
        return Optional.ofNullable(sailings.get(id));
    }

    @Override
    public List<SailingAvailability> search(LocalDate date, Terminal from,
                                            LocalDateTime reservationsHeldSince) {
        return sailings.values().stream()
                .filter(sailing -> sailing.date().equals(date) && sailing.from() == from)
                .map(sailing -> availabilityOf(sailing, reservationsHeldSince))
                .toList();
    }

    @Override
    public void save(Sailing sailing) {
        sailings.put(sailing.id(), sailing);
    }

    /** Every sailing held, in no particular order. */
    public List<Sailing> all() {
        return List.copyOf(sailings.values());
    }

    private SailingAvailability availabilityOf(Sailing sailing, LocalDateTime reservationsHeldSince) {
        List<Booking> holding = bookings.findBySailingId(sailing.id()).stream()
                .filter(booking -> booking.holdsPlacesAt(
                        reservationsHeldSince.plus(Booking.RESERVATION_WINDOW)))
                .toList();
        int passengers = holding.stream().mapToInt(booking -> booking.passengers().size()).sum();
        int vehicles = (int) holding.stream().filter(booking -> booking.vehicle().isPresent()).count();
        return new SailingAvailability(sailing, passengers, vehicles);
    }
}
