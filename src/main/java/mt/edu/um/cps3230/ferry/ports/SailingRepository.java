package mt.edu.um.cps3230.ferry.ports;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingAvailability;
import mt.edu.um.cps3230.ferry.domain.Terminal;

/** Where sailings are kept. */
public interface SailingRepository {

    Optional<Sailing> findById(String id);

    /**
     * The sailings leaving one terminal on one date, with what is already
     * booked on each of them.
     *
     * @param date the date of the sailing
     * @param from the terminal the sailing leaves from
     * @param reservationsHeldSince reservations made before this are treated as
     *     given up, so they no longer hold places
     * @return the matching sailings and their occupancy, in no particular order
     */
    List<SailingAvailability> search(LocalDate date, Terminal from, LocalDateTime reservationsHeldSince);

    void save(Sailing sailing);

    default void saveAll(List<Sailing> sailings) {
        sailings.forEach(this::save);
    }
}
