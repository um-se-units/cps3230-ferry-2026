package mt.edu.um.cps3230.ferry.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import mt.edu.um.cps3230.ferry.ports.BookingRepository;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;
import mt.edu.um.cps3230.ferry.ports.PaymentGateway;
import mt.edu.um.cps3230.ferry.ports.PaymentResult;
import mt.edu.um.cps3230.ferry.ports.SailingRepository;

/**
 * Makes, pays for, calls off and checks in bookings. Everything it needs to
 * reach the outside world, the clock included, is handed to it when it is
 * built.
 */
public final class BookingService {

    /** A customer may call a booking off until this long before the sailing leaves. */
    public static final Duration CANCELLATION_WINDOW = Duration.ofHours(2);

    private final Clock clock;
    private final SailingRepository sailings;
    private final BookingRepository bookings;
    private final PaymentGateway payments;
    private final NotificationGateway notifications;
    private final PriceCalculator prices;
    private final BookingReferences references;

    public BookingService(Clock clock, SailingRepository sailings, BookingRepository bookings,
                          PaymentGateway payments, NotificationGateway notifications,
                          PriceCalculator prices, BookingReferences references) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sailings = Objects.requireNonNull(sailings, "sailings");
        this.bookings = Objects.requireNonNull(bookings, "bookings");
        this.payments = Objects.requireNonNull(payments, "payments");
        this.notifications = Objects.requireNonNull(notifications, "notifications");
        this.prices = Objects.requireNonNull(prices, "prices");
        this.references = Objects.requireNonNull(references, "references");
    }

    /**
     * What a request would cost, without holding any places.
     *
     * @param request what the customer is asking for
     * @return the price
     */
    public Money quote(BookingRequest request) {
        Sailing sailing = sailingFor(request.sailingId());
        return prices.priceFor(sailing.date(), request.passengers(), request.vehicle().isPresent());
    }

    /**
     * Holds places on a sailing. The places come back if the booking is not
     * confirmed within {@link Booking#RESERVATION_WINDOW}.
     *
     * @param request what the customer is asking for
     * @return the reserved booking
     */
    public Booking reserve(BookingRequest request) {
        Sailing sailing = sailingFor(request.sailingId());
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isBefore(sailing.departsAt())) {
            throw new IllegalStateException("sailing " + sailing.id() + " has already left");
        }
        refusePlacesThatAreNotThere(sailing, request, now);
        Booking booking = Booking.reservation(references.next(), sailing, request.passengers(),
                request.vehicle(), request.contactEmail(), now);
        bookings.save(booking);
        return booking;
    }

    /**
     * Takes payment and confirms a reservation.
     *
     * @param reference the booking reference
     * @param cardNumber the card to charge
     * @return the confirmed booking
     */
    public Booking confirm(String reference, String cardNumber) {
        Booking booking = bookingFor(reference);
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking.status() != BookingStatus.RESERVED) {
            throw new IllegalStateException(
                    "booking " + reference + " cannot move from " + booking.status() + " to "
                            + BookingStatus.CONFIRMED);
        }
        if (booking.hasExpired(now)) {
            throw new IllegalStateException("reservation " + reference + " was held too long and has gone");
        }
        PaymentResult payment = payments.charge(cardNumber, prices.priceFor(booking));
        if (!payment.approved()) {
            throw new PaymentDeclinedException("the card on booking " + reference + " was turned down");
        }
        Booking confirmed = booking.confirmedWith(payment.reference());
        bookings.save(confirmed);
        notifications.send(confirmed.contactEmail(),
                "Booking " + reference + " is confirmed for the " + confirmed.sailing().departsAt()
                        + " sailing from " + confirmed.sailing().from().displayName() + ".");
        return confirmed;
    }

    /**
     * Calls a booking off at the customer's request, which is allowed until
     * {@link #CANCELLATION_WINDOW} before the sailing leaves.
     *
     * @param reference the booking reference
     * @return the cancelled booking
     */
    public Booking cancel(String reference) {
        Booking booking = bookingFor(reference);
        LocalDateTime deadline = booking.sailing().departsAt().minus(CANCELLATION_WINDOW);
        if (!LocalDateTime.now(clock).isBefore(deadline)) {
            throw new IllegalStateException(
                    "booking " + reference + " can no longer be cancelled, the sailing leaves at "
                            + booking.sailing().departsAt());
        }
        Booking cancelled = booking.cancelled();
        bookings.save(cancelled);
        return cancelled;
    }

    /**
     * Checks a confirmed booking in at the terminal.
     *
     * @param reference the booking reference
     * @return the checked in booking
     */
    public Booking checkIn(String reference) {
        Booking checkedIn = bookingFor(reference).checkedIn();
        bookings.save(checkedIn);
        return checkedIn;
    }

    public Booking find(String reference) {
        return bookingFor(reference);
    }

    private void refusePlacesThatAreNotThere(Sailing sailing, BookingRequest request, LocalDateTime now) {
        List<Booking> held = bookings.findBySailingId(sailing.id()).stream()
                .filter(booking -> booking.holdsPlacesAt(now))
                .toList();
        int passengersTaken = held.stream().mapToInt(booking -> booking.passengers().size()).sum();
        int passengersLeft = sailing.passengerCapacity() - passengersTaken;
        if (request.passengers().size() > passengersLeft) {
            throw new SailingFullException("sailing " + sailing.id() + " has " + passengersLeft
                    + " passenger places left, and the booking asks for " + request.passengers().size());
        }
        if (request.vehicle().isPresent()) {
            long vehiclesTaken = held.stream().filter(booking -> booking.vehicle().isPresent()).count();
            if (vehiclesTaken >= sailing.vehicleCapacity()) {
                throw new SailingFullException(
                        "sailing " + sailing.id() + " has no vehicle places left");
            }
        }
    }

    private Sailing sailingFor(String sailingId) {
        return sailings.findById(sailingId)
                .orElseThrow(() -> new NoSuchElementException("no sailing with id " + sailingId));
    }

    private Booking bookingFor(String reference) {
        return bookings.findByReference(reference)
                .orElseThrow(() -> new NoSuchElementException("no booking with reference " + reference));
    }
}
