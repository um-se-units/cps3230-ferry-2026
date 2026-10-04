package mt.edu.um.cps3230.ferry.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.domain.BookingRequest;
import mt.edu.um.cps3230.ferry.domain.BookingService;
import mt.edu.um.cps3230.ferry.domain.CancellationPolicy;
import mt.edu.um.cps3230.ferry.domain.Passenger;
import mt.edu.um.cps3230.ferry.domain.PriceCalculator;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingAvailability;
import mt.edu.um.cps3230.ferry.domain.SailingSearch;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.ports.SailingRepository;

/**
 * The JSON API, and the static pages that call it. A failed request always
 * answers with {@code {"errors": [...]}}: 404 when a reference is not known,
 * 400 for everything the application refuses to do.
 */
public final class Routes {

    private final BookingService bookings;
    private final SailingSearch search;
    private final SailingRepository sailings;
    private final CancellationPolicy weather;
    private final PriceCalculator prices;

    public Routes(BookingService bookings, SailingSearch search, SailingRepository sailings,
                  CancellationPolicy weather, PriceCalculator prices) {
        this.bookings = Objects.requireNonNull(bookings, "bookings");
        this.search = Objects.requireNonNull(search, "search");
        this.sailings = Objects.requireNonNull(sailings, "sailings");
        this.weather = Objects.requireNonNull(weather, "weather");
        this.prices = Objects.requireNonNull(prices, "prices");
    }

    /**
     * Builds the application, with the front end served from the same process.
     *
     * @return a Javalin instance that has not been started yet
     */
    public Javalin toJavalin() {
        return Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.routes
                    .get("/api/sailings", this::listSailings)
                    .get("/api/sailings/{id}", this::showSailing)
                    .get("/api/quote", this::quote)
                    .post("/api/bookings", this::reserve)
                    .get("/api/bookings/{reference}", this::showBooking)
                    .post("/api/bookings/{reference}/confirm", this::confirm)
                    .post("/api/bookings/{reference}/cancel", this::cancel)
                    .post("/api/admin/sailings/{id}/weather-check", this::weatherCheck)
                    .exception(NoSuchElementException.class,
                            (failure, context) -> answer(context, 404, failure.getMessage()))
                    .exception(IllegalArgumentException.class,
                            (failure, context) -> answer(context, 400, failure.getMessage()))
                    .exception(IllegalStateException.class,
                            (failure, context) -> answer(context, 400, failure.getMessage()));
        });
    }

    private void listSailings(Context context) {
        LocalDate date = dateParam(context, "date");
        Terminal from = terminalParam(context, "from");
        context.json(search.on(date, from).stream().map(SailingView::of).toList());
    }

    private void showSailing(Context context) {
        String id = context.pathParam("id");
        Sailing sailing = sailings.findById(id)
                .orElseThrow(() -> new NoSuchElementException("no sailing with id " + id));
        context.json(SailingView.of(availabilityOf(sailing)));
    }

    private void quote(Context context) {
        String sailingId = requiredParam(context, "sailingId");
        Sailing sailing = sailings.findById(sailingId)
                .orElseThrow(() -> new NoSuchElementException("no sailing with id " + sailingId));
        BookingRequest request = new BookingRequest(sailingId, passengersIn(context),
                Optional.empty(), "quote@example.com");
        boolean withVehicle = Boolean.parseBoolean(context.queryParam("vehicle"));
        context.json(QuoteView.of(
                prices.priceFor(sailing.date(), request.passengers(), withVehicle)));
    }

    private void reserve(Context context) {
        NewBookingRequest request = bodyOf(context, NewBookingRequest.class);
        Booking booking = bookings.reserve(request.toBookingRequest());
        context.status(201).json(BookingView.of(booking, prices.priceFor(booking)));
    }

    private void showBooking(Context context) {
        Booking booking = bookings.find(context.pathParam("reference"));
        context.json(BookingView.of(booking, prices.priceFor(booking)));
    }

    private void confirm(Context context) {
        ConfirmRequest request = bodyOf(context, ConfirmRequest.class);
        Booking booking = bookings.confirm(context.pathParam("reference"), request.cardNumber());
        context.json(BookingView.of(booking, prices.priceFor(booking)));
    }

    private void cancel(Context context) {
        Booking booking = bookings.cancel(context.pathParam("reference"));
        context.json(BookingView.of(booking, prices.priceFor(booking)));
    }

    private void weatherCheck(Context context) {
        String id = context.pathParam("id");
        Sailing sailing = sailings.findById(id)
                .orElseThrow(() -> new NoSuchElementException("no sailing with id " + id));
        context.json(WeatherCheckView.of(id, weather.apply(sailing)));
    }

    /** The occupancy of one sailing, read back from the search over its own day. */
    private SailingAvailability availabilityOf(Sailing sailing) {
        return search.on(sailing.date(), sailing.from()).stream()
                .filter(found -> found.sailing().id().equals(sailing.id()))
                .findFirst()
                .orElseGet(() -> new SailingAvailability(sailing, 0, 0));
    }

    /**
     * Reads the passengers of a quote, given once per passenger as
     * {@code passenger=YYYY-MM-DD} or {@code passenger=YYYY-MM-DD:resident}.
     */
    private static List<Passenger> passengersIn(Context context) {
        List<String> given = context.queryParams("passenger");
        if (given.isEmpty()) {
            throw new IllegalArgumentException(
                    "a quote needs at least one passenger, as passenger=YYYY-MM-DD");
        }
        List<Passenger> passengers = new ArrayList<>();
        for (String passenger : given) {
            String[] parts = passenger.split(":", 2);
            boolean resident = parts.length == 2 && "resident".equalsIgnoreCase(parts[1].trim());
            passengers.add(new PassengerRequest("Passenger", parts[0].trim(), resident).toPassenger());
        }
        return passengers;
    }

    private static <T> T bodyOf(Context context, Class<T> shape) {
        T body;
        try {
            body = context.bodyAsClass(shape);
        } catch (Exception unreadable) {
            // Jackson reports a bad body as an IOException, so nothing narrower will do.
            throw new IllegalArgumentException("the request body is not the JSON this expects");
        }
        if (body == null) {
            throw new IllegalArgumentException("the request needs a JSON body");
        }
        return body;
    }

    private static LocalDate dateParam(Context context, String name) {
        String given = requiredParam(context, name);
        try {
            return LocalDate.parse(given);
        } catch (DateTimeParseException badDate) {
            throw new IllegalArgumentException(name + " must be a date as YYYY-MM-DD: " + given);
        }
    }

    private static Terminal terminalParam(Context context, String name) {
        String given = requiredParam(context, name);
        try {
            return Terminal.valueOf(given.toUpperCase());
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(
                    name + " must be CIRKEWWA or MGARR: " + given);
        }
    }

    private static String requiredParam(Context context, String name) {
        String given = context.queryParam(name);
        if (given == null || given.isBlank()) {
            throw new IllegalArgumentException(name + " is needed");
        }
        return given;
    }

    private static void answer(Context context, int status, String message) {
        context.status(status).json(ErrorsView.of(message));
    }
}
