package mt.edu.um.cps3230.ferry.ports;

import java.util.List;
import java.util.Optional;
import mt.edu.um.cps3230.ferry.domain.Booking;

/** Where bookings are kept. */
public interface BookingRepository {

    Optional<Booking> findByReference(String reference);

    /**
     * Every booking made on one sailing, whatever its status.
     *
     * @param sailingId the sailing to look under
     * @return the bookings on that sailing
     */
    List<Booking> findBySailingId(String sailingId);

    /** Stores a booking, replacing any earlier version with the same reference. */
    void save(Booking booking);
}
