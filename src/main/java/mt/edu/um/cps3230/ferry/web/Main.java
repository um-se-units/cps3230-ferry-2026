package mt.edu.um.cps3230.ferry.web;

import io.javalin.Javalin;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import mt.edu.um.cps3230.ferry.adapters.inmemory.CsvTimetableReader;
import mt.edu.um.cps3230.ferry.adapters.jdbc.Database;
import mt.edu.um.cps3230.ferry.adapters.jdbc.JdbcBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.jdbc.JdbcSailingRepository;
import mt.edu.um.cps3230.ferry.adapters.notify.LogFileNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.adapters.weather.OpenMeteoWeatherFeed;
import mt.edu.um.cps3230.ferry.domain.BookingReferences;
import mt.edu.um.cps3230.ferry.domain.BookingService;
import mt.edu.um.cps3230.ferry.domain.CancellationPolicy;
import mt.edu.um.cps3230.ferry.domain.PriceCalculator;
import mt.edu.um.cps3230.ferry.domain.SailingSearch;
import mt.edu.um.cps3230.ferry.domain.Timetable;
import mt.edu.um.cps3230.ferry.ports.BookingRepository;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;
import mt.edu.um.cps3230.ferry.ports.PaymentGateway;
import mt.edu.um.cps3230.ferry.ports.SailingRepository;
import mt.edu.um.cps3230.ferry.ports.WeatherFeed;

/**
 * Starts the application. This is the only place that decides which adapter
 * stands behind which port: everything else is handed what it needs.
 *
 * <p>Settings, all optional, given as system properties:
 * <ul>
 *   <li>{@code ferry.port}, the port to listen on, 8080 by default</li>
 *   <li>{@code ferry.db.file}, a path to keep the database in; without it the
 *       database lives in memory and starts empty every time</li>
 *   <li>{@code ferry.notifications}, the log file messages are written to,
 *       {@code notifications.log} by default</li>
 *   <li>{@code ferry.timetable.days}, how many days of sailings to seed, 14 by
 *       default</li>
 * </ul>
 */
public final class Main {

    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_TIMETABLE_DAYS = 14;

    private Main() {
    }

    public static void main(String[] args) {
        Clock clock = Clock.system(MaltaTime.ZONE);

        DataSource database = databaseFor(System.getProperty("ferry.db.file"));
        Database.applySchema(database);

        SailingRepository sailings = new JdbcSailingRepository(database);
        BookingRepository bookings = new JdbcBookingRepository(database);
        PaymentGateway payments = new FakePaymentGateway();
        NotificationGateway notifications = new LogFileNotificationGateway(
                Path.of(System.getProperty("ferry.notifications", "notifications.log")), clock);
        WeatherFeed weather = new OpenMeteoWeatherFeed(HttpClient.newHttpClient());

        seedTimetable(sailings, clock);

        PriceCalculator prices = new PriceCalculator();
        BookingReferences references = () -> "FF-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        BookingService bookingService = new BookingService(clock, sailings, bookings, payments,
                notifications, prices, references);
        SailingSearch search = new SailingSearch(clock, sailings);
        CancellationPolicy cancellations =
                new CancellationPolicy(weather, bookings, payments, notifications, prices);

        Javalin app = new Routes(bookingService, search, sailings, cancellations, prices).toJavalin();
        app.start(portFrom(System.getProperty("ferry.port")));
    }

    private static DataSource databaseFor(String file) {
        return file == null ? Database.inMemory("ferry") : Database.file(Path.of(file));
    }

    private static int portFrom(String given) {
        return given == null ? DEFAULT_PORT : Integer.parseInt(given);
    }

    /** Puts the daily timetable on the next fortnight, so that there is something to book. */
    private static void seedTimetable(SailingRepository sailings, Clock clock) {
        Timetable timetable = CsvTimetableReader.fromClasspath();
        int days = Integer.getInteger("ferry.timetable.days", DEFAULT_TIMETABLE_DAYS);
        LocalDate today = LocalDate.now(clock);
        for (int day = 0; day < days; day++) {
            sailings.saveAll(timetable.sailingsOn(today.plusDays(day)));
        }
    }
}
