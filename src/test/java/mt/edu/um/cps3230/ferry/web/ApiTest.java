package mt.edu.um.cps3230.ferry.web;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemorySailingRepository;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.adapters.weather.FixedWeatherFeed;
import mt.edu.um.cps3230.ferry.domain.BookingReferences;
import mt.edu.um.cps3230.ferry.domain.BookingService;
import mt.edu.um.cps3230.ferry.domain.CancellationPolicy;
import mt.edu.um.cps3230.ferry.domain.PriceCalculator;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingSearch;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {

    private static final ZoneId MALTA = ZoneId.of("Europe/Malta");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 6);
    private static final LocalDateTime DEPARTURE = DATE.atTime(7, 45);
    /** Well before the departure, so a booking can still be held and called off. */
    private static final LocalDateTime NOW = DATE.atTime(5, 0);
    private static final String SAILING_ID = "2026-10-06-0745-CIRKEWWA";
    private static final Sailing SAILING = new Sailing(SAILING_ID, Terminal.CIRKEWWA,
            Terminal.MGARR, DEPARTURE, 900, 140);

    private static final String ONE_ADULT = """
            {"sailingId": "%s", "contactEmail": "traveller@example.com",
             "passengers": [{"name": "Anna", "dateOfBirth": "1990-04-05", "gozoResident": false}]}
            """.formatted(SAILING_ID);

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final InMemorySailingRepository sailings = new InMemorySailingRepository(bookings);
    private final InMemoryNotificationGateway notifications = new InMemoryNotificationGateway();
    private final AtomicInteger nextReference = new AtomicInteger();

    private Javalin app;

    @BeforeEach
    void startTheApplication() {
        sailings.save(SAILING);
        sailings.save(new Sailing("2026-10-06-0615-MGARR", Terminal.MGARR, Terminal.CIRKEWWA,
                DATE.atTime(6, 15), 900, 140));

        Clock clock = Clock.fixed(NOW.atZone(MALTA).toInstant(), MALTA);
        PriceCalculator prices = new PriceCalculator();
        FakePaymentGateway payments = new FakePaymentGateway();
        BookingReferences references = () -> String.format("FF-%04d", nextReference.incrementAndGet());

        app = new Routes(
                new BookingService(clock, sailings, bookings, payments, notifications, prices,
                        references),
                new SailingSearch(clock, sailings), sailings,
                new CancellationPolicy(new FixedWeatherFeed(3.0, 70), bookings, payments,
                        notifications, prices),
                prices).toJavalin();
        app.start(0);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = app.port();
    }

    @AfterEach
    void stopTheApplication() {
        app.stop();
        RestAssured.reset();
    }

    @Test
    void shouldListTheSailingsLeavingThatTerminalWhenTheDayIsSearched() {
        given().when().get("/api/sailings?date=2026-10-06&from=CIRKEWWA")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(SAILING_ID))
                .body("[0].passengerPlacesLeft", equalTo(900))
                .body("[0].full", equalTo(false));
    }

    @Test
    void shouldPrintTimesAsIso8601InMaltaTimeWhenASailingIsListed() {
        given().when().get("/api/sailings?date=2026-10-06&from=CIRKEWWA")
                .then().statusCode(200)
                .body("[0].departsAt", equalTo("2026-10-06T07:45:00+02:00"));
    }

    @Test
    void shouldRefuseWhenTheSearchHasNoDate() {
        given().when().get("/api/sailings?from=CIRKEWWA")
                .then().statusCode(400).body("errors", contains("date is needed"));
    }

    @Test
    void shouldRefuseWhenTheDateIsNotADate() {
        given().when().get("/api/sailings?date=the-sixth&from=CIRKEWWA")
                .then().statusCode(400).body("errors[0]", containsString("YYYY-MM-DD"));
    }

    @Test
    void shouldRefuseWhenTheTerminalIsNotOneOfTheTwo() {
        given().when().get("/api/sailings?date=2026-10-06&from=VALLETTA")
                .then().statusCode(400).body("errors[0]", containsString("CIRKEWWA or MGARR"));
    }

    @Test
    void shouldShowOneSailingWhenItsIdIsAskedFor() {
        given().when().get("/api/sailings/" + SAILING_ID)
                .then().statusCode(200).body("toName", equalTo("Mġarr"));
    }

    @Test
    void shouldAnswerNotFoundWhenTheSailingIsNotKnown() {
        given().when().get("/api/sailings/no-such-sailing")
                .then().statusCode(404).body("errors[0]", containsString("no sailing with id"));
    }

    @Test
    void shouldPriceATripWhenAQuoteIsAskedFor() {
        given().when().get("/api/quote?sailingId=" + SAILING_ID
                        + "&passenger=1990-04-05&passenger=2018-04-05&vehicle=true")
                .then().statusCode(200)
                .body("priceCents", equalTo(2150));
    }

    @Test
    void shouldChargeTheResidentFareWhenAQuoteSaysSo() {
        given().when().get("/api/quote?sailingId=" + SAILING_ID + "&passenger=1990-04-05:resident")
                .then().statusCode(200).body("priceCents", equalTo(115));
    }

    @Test
    void shouldRefuseWhenAQuoteNamesNobody() {
        given().when().get("/api/quote?sailingId=" + SAILING_ID)
                .then().statusCode(400).body("errors[0]", containsString("at least one passenger"));
    }

    @Test
    void shouldHoldPlacesWhenABookingIsPosted() {
        given().contentType("application/json").body(ONE_ADULT)
                .when().post("/api/bookings")
                .then().statusCode(201)
                .body("reference", equalTo("FF-0001"))
                .body("status", equalTo("RESERVED"))
                .body("priceCents", equalTo(465))
                .body("sailing.id", equalTo(SAILING_ID));
    }

    @Test
    void shouldTakeThePlacesOffTheSailingWhenABookingIsHeld() {
        reserve();

        given().when().get("/api/sailings?date=2026-10-06&from=CIRKEWWA")
                .then().body("[0].passengerPlacesLeft", equalTo(899));
    }

    @Test
    void shouldRefuseWhenTheBodyIsNotTheJsonItExpects() {
        given().contentType("application/json").body("{\"nope\": 1}")
                .when().post("/api/bookings")
                .then().statusCode(400).body("errors[0]", containsString("not the JSON"));
    }

    @Test
    void shouldRefuseWhenTheBookingNamesNoPassengers() {
        given().contentType("application/json")
                .body("{\"sailingId\": \"" + SAILING_ID
                        + "\", \"contactEmail\": \"a@b.com\", \"passengers\": []}")
                .when().post("/api/bookings")
                .then().statusCode(400).body("errors[0]", containsString("at least one passenger"));
    }

    @Test
    void shouldAnswerNotFoundWhenTheBookingIsForASailingThatIsNotThere() {
        given().contentType("application/json")
                .body(ONE_ADULT.replace(SAILING_ID, "no-such-sailing"))
                .when().post("/api/bookings")
                .then().statusCode(404);
    }

    @Test
    void shouldShowABookingWhenItsReferenceIsAskedFor() {
        String reference = reserve();

        given().when().get("/api/bookings/" + reference)
                .then().statusCode(200)
                .body("reference", equalTo(reference))
                .body("passengers[0].name", equalTo("Anna"))
                .body("price", equalTo("€4.65"));
    }

    @Test
    void shouldAnswerNotFoundWhenTheBookingIsNotKnown() {
        given().when().get("/api/bookings/FF-9999")
                .then().statusCode(404).body("errors[0]", containsString("no booking"));
    }

    @Test
    void shouldConfirmWhenTheCardIsGood() {
        String reference = reserve();

        given().contentType("application/json").body("{\"cardNumber\": \"4111111111111111\"}")
                .when().post("/api/bookings/" + reference + "/confirm")
                .then().statusCode(200).body("status", equalTo("CONFIRMED"));

        assertThat(notifications.messagesTo("traveller@example.com")).hasSize(1);
    }

    @Test
    void shouldRefuseWhenTheCardIsTurnedDown() {
        String reference = reserve();

        given().contentType("application/json").body("{\"cardNumber\": \"4111111111110000\"}")
                .when().post("/api/bookings/" + reference + "/confirm")
                .then().statusCode(400).body("errors[0]", containsString("turned down"));

        given().when().get("/api/bookings/" + reference)
                .then().body("status", equalTo("RESERVED"));
    }

    @Test
    void shouldCallTheBookingOffWhenItIsCancelled() {
        String reference = reserve();

        given().when().post("/api/bookings/" + reference + "/cancel")
                .then().statusCode(200).body("status", equalTo("CANCELLED"));
    }

    @Test
    void shouldGivePlacesBackWhenABookingIsCancelled() {
        String reference = reserve();

        given().when().post("/api/bookings/" + reference + "/cancel").then().statusCode(200);

        given().when().get("/api/sailings?date=2026-10-06&from=CIRKEWWA")
                .then().body("[0].passengerPlacesLeft", equalTo(900));
    }

    @Test
    void shouldRunTheCancellationPolicyWhenTheWeatherIsChecked() {
        String reference = reserve();

        given().when().post("/api/admin/sailings/" + SAILING_ID + "/weather-check")
                .then().statusCode(200)
                .body("sailingId", equalTo(SAILING_ID))
                .body("status", equalTo("CANCELLED"));

        given().when().get("/api/bookings/" + reference)
                .then().body("status", equalTo("CANCELLED"));
    }

    @Test
    void shouldAnswerNotFoundWhenTheWeatherIsCheckedForASailingThatIsNotThere() {
        given().when().post("/api/admin/sailings/no-such-sailing/weather-check")
                .then().statusCode(404);
    }

    @Test
    void shouldServeTheSearchPageWhenTheRootIsAskedFor() {
        given().when().get("/")
                .then().statusCode(200).body(containsString("Find a sailing"));
    }

    @Test
    void shouldServeTheScriptThePagesUseWhenItIsAskedFor() {
        given().when().get("/app.js")
                .then().statusCode(200).body(not(containsString("TODO")));
    }

    private String reserve() {
        return given().contentType("application/json").body(ONE_ADULT)
                .when().post("/api/bookings")
                .then().statusCode(201).extract().path("reference");
    }
}
