package mt.edu.um.cps3230.ferry.web;

import java.util.List;
import java.util.Optional;
import mt.edu.um.cps3230.ferry.domain.BookingRequest;
import mt.edu.um.cps3230.ferry.domain.Vehicle;

/** A request to hold places on a sailing. */
public record NewBookingRequest(String sailingId, List<PassengerRequest> passengers,
                                String vehicleRegistration, String contactEmail) {

    public BookingRequest toBookingRequest() {
        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException("a booking needs at least one passenger");
        }
        Optional<Vehicle> vehicle = Optional.ofNullable(vehicleRegistration)
                .filter(registration -> !registration.isBlank())
                .map(Vehicle::new);
        return new BookingRequest(sailingId, passengers.stream()
                .map(PassengerRequest::toPassenger).toList(), vehicle, contactEmail);
    }
}
