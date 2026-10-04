package mt.edu.um.cps3230.ferry.adapters.inmemory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.ports.BookingRepository;

/** Keeps bookings in a map, for tests and for running without a database. */
public final class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Optional<Booking> findByReference(String reference) {
        return Optional.ofNullable(bookings.get(reference));
    }

    @Override
    public List<Booking> findBySailingId(String sailingId) {
        return bookings.values().stream()
                .filter(booking -> booking.sailing().id().equals(sailingId))
                .toList();
    }

    @Override
    public void save(Booking booking) {
        bookings.put(booking.reference(), booking);
    }

    /** Every booking held, in no particular order. */
    public List<Booking> all() {
        return List.copyOf(bookings.values());
    }
}
