package mt.edu.um.cps3230.ferry.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One crossing on the timetable, with the places it has for passengers and for
 * vehicles.
 *
 * @param id the identifier, stable for a given date and departure
 * @param from the terminal the sailing leaves from
 * @param to the terminal the sailing arrives at
 * @param departsAt the local departure time in Malta
 * @param passengerCapacity how many passengers the sailing takes
 * @param vehicleCapacity how many vehicles the sailing takes
 */
public record Sailing(String id, Terminal from, Terminal to, LocalDateTime departsAt,
                      int passengerCapacity, int vehicleCapacity) {

    public Sailing {
        Objects.requireNonNull(id, "a sailing needs an id");
        Objects.requireNonNull(from, "a sailing needs a terminal to leave from");
        Objects.requireNonNull(to, "a sailing needs a terminal to arrive at");
        Objects.requireNonNull(departsAt, "a sailing needs a departure time");
        if (from == to) {
            throw new IllegalArgumentException("a sailing crosses between the two terminals: " + from);
        }
        if (passengerCapacity < 0 || vehicleCapacity < 0) {
            throw new IllegalArgumentException("a capacity cannot be negative");
        }
    }

    /** The date the sailing leaves on, which is the date every fare is worked out against. */
    public LocalDate date() {
        return departsAt.toLocalDate();
    }
}
