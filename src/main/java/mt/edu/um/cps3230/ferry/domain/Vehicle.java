package mt.edu.um.cps3230.ferry.domain;

/**
 * The one vehicle a booking may carry.
 *
 * @param registration the registration plate
 */
public record Vehicle(String registration) {

    public Vehicle {
        if (registration == null || registration.isBlank()) {
            throw new IllegalArgumentException("a vehicle needs a registration");
        }
        registration = registration.trim().toUpperCase();
    }
}
