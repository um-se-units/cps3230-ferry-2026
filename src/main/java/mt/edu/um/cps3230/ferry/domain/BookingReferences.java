package mt.edu.um.cps3230.ferry.domain;

/**
 * Where the reference printed on a booking comes from. It is handed to
 * {@link BookingService} so that a test can hand out references it knows.
 */
@FunctionalInterface
public interface BookingReferences {

    /** The next reference to print on a booking. */
    String next();
}
