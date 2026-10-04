package mt.edu.um.cps3230.ferry.adapters.inmemory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.domain.Timetable;
import mt.edu.um.cps3230.ferry.domain.TimetableEntry;

/**
 * Reads the daily timetable from a comma separated resource. Blank lines and
 * lines starting with a hash are skipped, so the file can explain itself.
 */
public final class CsvTimetableReader {

    private static final String DEFAULT_RESOURCE = "/sailings.csv";
    private static final int COLUMNS = 5;

    private CsvTimetableReader() {
    }

    /** The timetable shipped with the application. */
    public static Timetable fromClasspath() {
        return fromClasspath(DEFAULT_RESOURCE);
    }

    /**
     * @param resource the path of the resource to read, starting with a slash
     * @return the timetable it holds
     */
    public static Timetable fromClasspath(String resource) {
        try (InputStream source = CsvTimetableReader.class.getResourceAsStream(resource)) {
            if (source == null) {
                throw new IllegalArgumentException("could not find " + resource + " on the classpath");
            }
            return read(IOUtils.readLines(source, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException("could not read " + resource, failure);
        }
    }

    /**
     * @param lines the lines of the file
     * @return the timetable they hold
     */
    public static Timetable read(List<String> lines) {
        List<TimetableEntry> entries = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            entries.add(entryIn(trimmed));
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("the timetable has no sailings in it");
        }
        return new Timetable(entries);
    }

    private static TimetableEntry entryIn(String line) {
        String[] columns = line.split(",");
        if (columns.length != COLUMNS) {
            throw new IllegalArgumentException(
                    "a timetable line needs " + COLUMNS + " columns: " + line);
        }
        try {
            return new TimetableEntry(
                    Terminal.valueOf(columns[0].trim()),
                    Terminal.valueOf(columns[1].trim()),
                    LocalTime.parse(columns[2].trim()),
                    Integer.parseInt(columns[3].trim()),
                    Integer.parseInt(columns[4].trim()));
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("could not read the timetable line: " + line, failure);
        }
    }
}
