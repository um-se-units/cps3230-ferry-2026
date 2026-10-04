package mt.edu.um.cps3230.ferry.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * The daily timetable. The same crossings run every day, so a sailing is the
 * timetable entry put on a date.
 */
public final class Timetable {

    private static final DateTimeFormatter DEPARTURE_IN_ID = DateTimeFormatter.ofPattern("HHmm");

    private final List<TimetableEntry> entries;

    public Timetable(List<TimetableEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<TimetableEntry> entries() {
        return entries;
    }

    /**
     * The sailings that run on one date, earliest first. An id is built from
     * the date, the departure and the terminal, so the same crossing keeps the
     * same id however often the timetable is read.
     *
     * @param date the date to put the timetable on
     * @return the sailings for that date
     */
    public List<Sailing> sailingsOn(LocalDate date) {
        return entries.stream()
                .map(entry -> new Sailing(idFor(date, entry), entry.from(), entry.to(),
                        date.atTime(entry.departure()), entry.passengerCapacity(),
                        entry.vehicleCapacity()))
                .sorted(Comparator.comparing(Sailing::departsAt))
                .toList();
    }

    private static String idFor(LocalDate date, TimetableEntry entry) {
        return date + "-" + entry.departure().format(DEPARTURE_IN_ID) + "-" + entry.from();
    }
}
