package mt.edu.um.cps3230.ferry.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What a customer asks for, before a booking exists.
 *
 * @param sailingId the sailing to travel on
 * @param passengers the people travelling, at least one
 * @param vehicle the vehicle to carry, if any
 * @param contactEmail where the operator should write
 */
public record BookingRequest(String sailingId, List<Passenger> passengers,
                             Optional<Vehicle> vehicle, String contactEmail) {

    public BookingRequest {
        Objects.requireNonNull(sailingId, "a request needs a sailing");
        Objects.requireNonNull(vehicle, "vehicle must be present or empty, never null");
        passengers = List.copyOf(passengers);
        if (passengers.isEmpty()) {
            throw new IllegalArgumentException("a booking needs at least one passenger");
        }
        if (contactEmail == null || !contactEmail.contains("@")) {
            throw new IllegalArgumentException("a booking needs a contact e-mail address: " + contactEmail);
        }
    }

    public static BookingRequest forPassengers(String sailingId, List<Passenger> passengers,
                                               String contactEmail) {
        return new BookingRequest(sailingId, passengers, Optional.empty(), contactEmail);
    }

    public static BookingRequest withVehicle(String sailingId, List<Passenger> passengers,
                                             Vehicle vehicle, String contactEmail) {
        return new BookingRequest(sailingId, passengers, Optional.of(vehicle), contactEmail);
    }
}
