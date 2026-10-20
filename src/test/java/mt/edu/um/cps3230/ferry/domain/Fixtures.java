package mt.edu.um.cps3230.ferry.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/** Ready made values, so that each test says only what matters to it. */
final class Fixtures {

    static final ZoneId MALTA = ZoneId.of("Europe/Malta");
    static final LocalDate SAILING_DATE = LocalDate.of(2026, 10, 6);
    static final LocalDateTime DEPARTURE = SAILING_DATE.atTime(7, 45);
    static final String SAILING_ID = "2026-10-06-0745-CIRKEWWA";
    static final String CONTACT = "traveller@example.com";
    static final Vehicle CAR = new Vehicle("ABC 123");
    static final String GOOD_CARD = "4111111111111111";
    static final String DECLINED_CARD = "4111111111110000";

    private Fixtures() {
    }

    static Sailing sailing() {
        return sailing(900, 140);
    }

    static Sailing sailing(int passengerCapacity, int vehicleCapacity) {
        return new Sailing(SAILING_ID, Terminal.CIRKEWWA, Terminal.MGARR, DEPARTURE,
                passengerCapacity, vehicleCapacity);
    }

    static Sailing sailingFrom(Terminal from, LocalDateTime departsAt) {
        return new Sailing(idFor(from, departsAt), from, from.opposite(), departsAt, 900, 140);
    }

    static String idFor(Terminal from, LocalDateTime departsAt) {
        return departsAt.toLocalDate() + "-"
                + departsAt.toLocalTime().format(DateTimeFormatter.ofPattern("HHmm")) + "-" + from;
    }

    static Passenger adult() {
        return aged(30);
    }

    static Passenger aged(int years) {
        return new Passenger("Passenger aged " + years, SAILING_DATE.minusYears(years), false);
    }

    static Passenger resident() {
        return residentAged(30);
    }

    static Passenger residentAged(int years) {
        return new Passenger("Gozo resident aged " + years, SAILING_DATE.minusYears(years), true);
    }

    static List<Passenger> adults(int howMany) {
        return IntStream.range(0, howMany).mapToObj(index -> adult()).toList();
    }

    static Booking reservationFor(Passenger... passengers) {
        return reservationFor(List.of(passengers));
    }

    static Booking reservationFor(List<Passenger> passengers) {
        return Booking.reservation("FF-0001", sailing(), passengers, Optional.empty(), CONTACT,
                DEPARTURE.minusDays(1));
    }

    static Booking reservationWithVehicle(Passenger... passengers) {
        return Booking.reservation("FF-0002", sailing(), List.of(passengers), Optional.of(CAR),
                CONTACT, DEPARTURE.minusDays(1));
    }

    static Booking reservationOn(Sailing sailing, String reference, String contactEmail,
                                 LocalDateTime reservedAt, Passenger... passengers) {
        return Booking.reservation(reference, sailing, List.of(passengers), Optional.empty(),
                contactEmail, reservedAt);
    }

    static Clock clockAt(LocalDateTime moment) {
        return Clock.fixed(moment.atZone(MALTA).toInstant(), MALTA);
    }
}
