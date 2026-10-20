package mt.edu.um.cps3230.ferry.adapters.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import java.util.List;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.domain.Timetable;
import mt.edu.um.cps3230.ferry.domain.TimetableEntry;
import org.junit.jupiter.api.Test;

class CsvTimetableReaderTest {

    @Test
    void shouldReadTheTimetableShippedWithTheApplicationWhenNoFileIsNamed() {
        Timetable timetable = CsvTimetableReader.fromClasspath();

        assertThat(timetable.entries()).hasSize(20);
        assertThat(timetable.entries()).extracting(TimetableEntry::from)
                .containsOnly(Terminal.CIRKEWWA, Terminal.MGARR);
    }

    @Test
    void shouldRunTheSameNumberOfCrossingsEachWayWhenTheTimetableIsRead() {
        List<TimetableEntry> entries = CsvTimetableReader.fromClasspath().entries();

        long leavingCirkewwa = entries.stream()
                .filter(entry -> entry.from() == Terminal.CIRKEWWA).count();

        assertThat(leavingCirkewwa).isEqualTo(entries.size() - leavingCirkewwa);
    }

    @Test
    void shouldSkipCommentsAndBlankLinesWhenTheFileIsRead() {
        Timetable timetable = CsvTimetableReader.read(List.of(
                "# a comment",
                "",
                "   ",
                "CIRKEWWA,MGARR,05:45,900,140"));

        assertThat(timetable.entries()).containsExactly(
                new TimetableEntry(Terminal.CIRKEWWA, Terminal.MGARR, LocalTime.of(5, 45), 900, 140));
    }

    @Test
    void shouldRefuseWhenALineHasTheWrongNumberOfColumns() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvTimetableReader.read(List.of("CIRKEWWA,MGARR,05:45")))
                .withMessageContaining("needs 5 columns");
    }

    @Test
    void shouldRefuseWhenATerminalIsNotOneOfTheTwo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvTimetableReader.read(
                        List.of("VALLETTA,MGARR,05:45,900,140")))
                .withMessageContaining("could not read the timetable line");
    }

    @Test
    void shouldRefuseWhenATimeIsNotATime() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvTimetableReader.read(
                        List.of("CIRKEWWA,MGARR,quarter to six,900,140")))
                .withMessageContaining("could not read the timetable line");
    }

    @Test
    void shouldRefuseWhenTheFileHasNoSailingsInIt() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvTimetableReader.read(List.of("# nothing but a comment")))
                .withMessageContaining("no sailings");
    }

    @Test
    void shouldRefuseWhenTheResourceIsNotThere() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvTimetableReader.fromClasspath("/no-such-file.csv"))
                .withMessageContaining("could not find");
    }
}
