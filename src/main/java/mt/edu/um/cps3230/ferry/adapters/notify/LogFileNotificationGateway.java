package mt.edu.um.cps3230.ferry.adapters.notify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;

/**
 * Writes each message to a log file, one line per message, instead of sending
 * real mail. Nothing in the course needs a mail server to be up.
 */
public final class LogFileNotificationGateway implements NotificationGateway {

    private final Path logFile;
    private final Clock clock;

    public LogFileNotificationGateway(Path logFile, Clock clock) {
        this.logFile = Objects.requireNonNull(logFile, "logFile");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void send(String emailAddress, String message) {
        String line = LocalDateTime.now(clock) + "\t" + emailAddress + "\t"
                + message.replace('\n', ' ') + System.lineSeparator();
        try {
            Files.writeString(logFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException failure) {
            throw new UncheckedIOException("could not write to " + logFile, failure);
        }
    }

    public Path logFile() {
        return logFile;
    }
}
