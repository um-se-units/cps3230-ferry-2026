package mt.edu.um.cps3230.ferry.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One booking on one sailing. A booking never changes: every move through the
 * lifecycle returns a new booking, and a move that the lifecycle does not allow
 * throws.
 *
 * @param reference the reference the customer quotes
 * @param sailing the sailing being travelled on
 * @param passengers the people travelling, at least one
 * @param vehicle the vehicle being carried, if any
 * @param contactEmail where the operator writes to
 * @param status where the booking has reached
 * @param reservedAt when the places were first held
 * @param paymentReference the payment to refund against, once confirmed
 */
public record Booking(String reference, Sailing sailing, List<Passenger> passengers,
                      Optional<Vehicle> vehicle, String contactEmail, BookingStatus status,
                      LocalDateTime reservedAt, Optional<String> paymentReference) {

    /** A reservation that is not confirmed within this long gives its places back. */
    public static final Duration RESERVATION_WINDOW = Duration.ofMinutes(15);

    public Booking {
        Objects.requireNonNull(reference, "a booking needs a reference");
        Objects.requireNonNull(sailing, "a booking needs a sailing");
        Objects.requireNonNull(vehicle, "vehicle must be present or empty, never null");
        Objects.requireNonNull(status, "a booking needs a status");
        Objects.requireNonNull(reservedAt, "a booking needs the time it was reserved");
        Objects.requireNonNull(paymentReference, "paymentReference must be present or empty, never null");
        passengers = List.copyOf(passengers);
        if (passengers.isEmpty()) {
            throw new IllegalArgumentException("a booking needs at least one passenger");
        }
        if (contactEmail == null || !contactEmail.contains("@")) {
            throw new IllegalArgumentException("a booking needs a contact e-mail address: " + contactEmail);
        }
    }

    /** A new booking, holding its places and waiting to be paid for. */
    public static Booking reservation(String reference, Sailing sailing, List<Passenger> passengers,
                                      Optional<Vehicle> vehicle, String contactEmail,
                                      LocalDateTime reservedAt) {
        return new Booking(reference, sailing, passengers, vehicle, contactEmail,
                BookingStatus.RESERVED, reservedAt, Optional.empty());
    }

    public Booking confirmedWith(String paymentReference) {
        requireStatusIn(BookingStatus.CONFIRMED, BookingStatus.RESERVED);
        return new Booking(reference, sailing, passengers, vehicle, contactEmail,
                BookingStatus.CONFIRMED, reservedAt, Optional.of(paymentReference));
    }

    public Booking cancelled() {
        requireStatusIn(BookingStatus.CANCELLED, BookingStatus.RESERVED, BookingStatus.CONFIRMED);
        return new Booking(reference, sailing, passengers, vehicle, contactEmail,
                BookingStatus.CANCELLED, reservedAt, paymentReference);
    }

    public Booking checkedIn() {
        requireStatusIn(BookingStatus.CHECKED_IN, BookingStatus.CONFIRMED);
        return new Booking(reference, sailing, passengers, vehicle, contactEmail,
                BookingStatus.CHECKED_IN, reservedAt, paymentReference);
    }

    /**
     * Whether the reservation has been held too long without being paid for.
     *
     * @param now the time to judge against
     * @return true when the places should go back to the sailing
     */
    public boolean hasExpired(LocalDateTime now) {
        return status == BookingStatus.RESERVED && now.isAfter(reservedAt.plus(RESERVATION_WINDOW));
    }

    /** Whether the booking still holds places on its sailing at the given time. */
    public boolean holdsPlacesAt(LocalDateTime now) {
        return status.isActive() && !hasExpired(now);
    }

    private void requireStatusIn(BookingStatus target, BookingStatus... allowed) {
        for (BookingStatus candidate : allowed) {
            if (status == candidate) {
                return;
            }
        }
        throw new IllegalStateException(
                "booking " + reference + " cannot move from " + status + " to " + target);
    }
}
