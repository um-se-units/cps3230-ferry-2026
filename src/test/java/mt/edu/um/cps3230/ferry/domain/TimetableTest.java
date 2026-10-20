package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimetableTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 6);

    private final Timetable timetable = new Timetable(List.of(
            new TimetableEntry(Terminal.MGARR, Terminal.CIRKEWWA, LocalTime.of(6, 15), 900, 140),
            new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, LocalTime.of(5, 45), 900, 140)));

    @Test
    void shouldBuildOneSailingPerEntryWhenTheTimetableIsPutOnADate() {
        assertThat(timetable.sailingsOn(DATE)).hasSize(2);
    }

    @Test
    void shouldOrderByDepartureWhenTheTimetableIsPutOnADate() {
        assertThat(timetable.sailingsOn(DATE))
                .extracting(Sailing::departsAt)
                .containsExactly(DATE.atTime(5, 45), DATE.atTime(6, 15));
    }

    @Test
    void shouldNameTheSailingAfterItsDateDepartureAndTerminalWhenItIsBuilt() {
        assertThat(timetable.sailingsOn(DATE))
                .extracting(Sailing::id)
                .containsExactly("2026-10-06-0545-CIRKEWWA", "2026-10-06-0615-MGARR");
    }

    @Test
    void shouldGiveTheSameSailingTheSameIdWhenTheTimetableIsReadAgain() {
        assertThat(timetable.sailingsOn(DATE)).isEqualTo(timetable.sailingsOn(DATE));
    }

    @Test
    void shouldKeepItsOwnCopyOfTheEntriesWhenTheListIsChangedAfterwards() {
        List<TimetableEntry> lines = new ArrayList<>(List.of(
                new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, LocalTime.of(5, 45), 900, 140)));
        Timetable built = new Timetable(lines);

        lines.add(new TimetableEntry(Terminal.MGARR, Terminal.CIRKEWWA, LocalTime.of(6, 15), 900, 140));

        assertThat(built.entries()).hasSize(1);
    }

    @Test
    void shouldCarryTheCapacitiesThroughWhenASailingIsBuilt() {
        Sailing sailing = timetable.sailingsOn(DATE).get(0);

        assertThat(sailing.passengerCapacity()).isEqualTo(900);
        assertThat(sailing.vehicleCapacity()).isEqualTo(140);
    }
}
