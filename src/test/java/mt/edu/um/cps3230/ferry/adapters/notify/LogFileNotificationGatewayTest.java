package mt.edu.um.cps3230.ferry.adapters.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogFileNotificationGatewayTest {

    private static final ZoneId MALTA = ZoneId.of("Europe/Malta");
    private static final LocalDateTime MOMENT = LocalDateTime.of(2026, 10, 6, 6, 0);
    private static final Clock CLOCK = Clock.fixed(MOMENT.atZone(MALTA).toInstant(), MALTA);

    @TempDir
    Path directory;

    @Test
    void shouldWriteOneLineWhenAMessageIsSent() throws IOException {
        Path log = directory.resolve("notifications.log");

        new LogFileNotificationGateway(log, CLOCK).send("traveller@example.com", "Booking confirmed");

        assertThat(Files.readAllLines(log)).singleElement().asString()
                .contains(MOMENT.toString(), "traveller@example.com", "Booking confirmed");
    }

    @Test
    void shouldAddToTheFileWhenItIsAlreadyThere() throws IOException {
        Path log = directory.resolve("notifications.log");
        LogFileNotificationGateway gateway = new LogFileNotificationGateway(log, CLOCK);

        gateway.send("first@example.com", "One");
        gateway.send("second@example.com", "Two");

        assertThat(Files.readAllLines(log)).hasSize(2);
    }

    @Test
    void shouldKeepOneMessageToOneLineWhenTheMessageRunsOver() throws IOException {
        Path log = directory.resolve("notifications.log");

        new LogFileNotificationGateway(log, CLOCK)
                .send("traveller@example.com", "Line one\nline two");

        List<String> lines = Files.readAllLines(log);
        assertThat(lines).singleElement().asString().contains("Line one line two");
    }

    @Test
    void shouldSayWhereItWritesWhenItIsAsked() {
        Path log = directory.resolve("notifications.log");

        assertThat(new LogFileNotificationGateway(log, CLOCK).logFile()).isEqualTo(log);
    }
}
