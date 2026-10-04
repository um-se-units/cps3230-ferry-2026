package mt.edu.um.cps3230.ferry.domain;

import java.util.Objects;

/**
 * A sailing together with what is already booked on it, which is what a search
 * result shows.
 *
 * @param sailing the sailing
 * @param passengersBooked how many passengers already hold places
 * @param vehiclesBooked how many vehicles already hold places
 */
public record SailingAvailability(Sailing sailing, int passengersBooked, int vehiclesBooked) {

    public SailingAvailability {
        Objects.requireNonNull(sailing, "availability needs a sailing");
        if (passengersBooked < 0 || vehiclesBooked < 0) {
            throw new IllegalArgumentException("a booked count cannot be negative");
        }
    }

    public int passengerPlacesLeft() {
        return Math.max(0, sailing.passengerCapacity() - passengersBooked);
    }

    public int vehiclePlacesLeft() {
        return Math.max(0, sailing.vehicleCapacity() - vehiclesBooked);
    }

    public boolean isFull() {
        return passengerPlacesLeft() == 0;
    }
}
