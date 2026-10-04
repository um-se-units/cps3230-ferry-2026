package mt.edu.um.cps3230.ferry.domain;

/** Where a booking has reached. */
public enum BookingStatus {

    /** Held, with the places taken off the sailing, but not yet paid for. */
    RESERVED,

    /** Paid for. */
    CONFIRMED,

    /** Paid for, and the passengers have arrived at the terminal. */
    CHECKED_IN,

    /** Called off, by the customer or by the operator, with the places given back. */
    CANCELLED;

    /** Whether a booking in this status still holds places on its sailing. */
    public boolean isActive() {
        return this != CANCELLED;
    }
}
