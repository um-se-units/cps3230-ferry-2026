package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.DEPARTURE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.clockAt;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.resident;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemorySailingRepository;
import org.junit.jupiter.api.Test;

/**
 * The mutants left in the smaller rules, none of which the earlier tests
 * reached.
 *
 * <pre>
 * Mutant                                     Killed by
 * fareFor returns null for a resident        a resident pays the resident fare
 * Terminal.displayName returns ""            each terminal has its own name
 * Terminal.opposite: negated, null           each terminal's opposite is the other
 * Timetable.entries returns an empty list    the timetable keeps its entries
 * SailingSearch.on: empty list, null key     the sailings come back, earliest first
 * </pre>
 */
class SmallRulesMutationTest {

    @Test
    void shouldChargeTheResidentFareWhenThePassengerLivesOnGozo() {
        assertThat(new PriceCalculator().priceFor(reservationFor(resident())))
                .isEqualTo(PriceCalculator.RESIDENT_FARE);
    }

    @Test
    void shouldNameEachTerminalWhenItIsShown() {
        assertThat(Terminal.CIRKEWWA.displayName()).isEqualTo("Ċirkewwa");
        assertThat(Terminal.MGARR.displayName()).isEqualTo("Mġarr");
    }

    @Test
    void shouldFindTheOtherTerminalWhenAskedForTheOpposite() {
        assertThat(Terminal.CIRKEWWA.opposite()).isEqualTo(Terminal.MGARR);
        assertThat(Terminal.MGARR.opposite()).isEqualTo(Terminal.CIRKEWWA);
    }

    @Test
    void shouldKeepItsEntriesWhenATimetableIsMade() {
        TimetableEntry entry = new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, LocalTime.NOON, 900, 140);

        assertThat(new Timetable(List.of(entry)).entries()).containsExactly(entry);
    }

    @Test
    void shouldListTheSailingsEarliestFirstWhenTheyAreSearched() {
        Timetable timetable = new Timetable(List.of(
                new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, LocalTime.of(12, 0), 900, 140),
                new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, LocalTime.of(7, 0), 900, 140)));
        InMemorySailingRepository sailings = new InMemorySailingRepository();
        sailings.saveAll(timetable.sailingsOn(SAILING_DATE));

        List<SailingAvailability> found = new SailingSearch(clockAt(DEPARTURE.minusDays(1)), sailings)
                .on(SAILING_DATE, Terminal.CIRKEWWA);

        assertThat(found).extracting(availability -> availability.sailing().departsAt().toLocalTime())
                .containsExactly(LocalTime.of(7, 0), LocalTime.of(12, 0));
    }
}
