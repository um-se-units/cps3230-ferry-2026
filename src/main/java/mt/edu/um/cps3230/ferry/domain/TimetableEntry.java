package mt.edu.um.cps3230.ferry.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * One line of the daily timetable, which becomes one sailing on every date.
 *
 * @param from the terminal the sailing leaves from
 * @param to the terminal the sailing arrives at
 * @param departure the local departure time
 * @param passengerCapacity how many passengers the sailing takes
 * @param vehicleCapacity how many vehicles the sailing takes
 */
public record TimetableEntry(Terminal from, Terminal to, LocalTime departure,
                             int passengerCapacity, int vehicleCapacity) {

    public TimetableEntry {
        Objects.requireNonNull(from, "a timetable entry needs a terminal to leave from");
        Objects.requireNonNull(to, "a timetable entry needs a terminal to arrive at");
        Objects.requireNonNull(departure, "a timetable entry needs a departure time");
        if (from == to) {
            throw new IllegalArgumentException("a sailing crosses between the two terminals: " + from);
        }
        if (passengerCapacity < 0 || vehicleCapacity < 0) {
            throw new IllegalArgumentException("a capacity cannot be negative");
        }
    }
}
