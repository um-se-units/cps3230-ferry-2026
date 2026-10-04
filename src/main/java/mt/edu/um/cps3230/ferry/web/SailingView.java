package mt.edu.um.cps3230.ferry.web;

import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingAvailability;

/** One sailing as the API prints it, with what is left on it. */
public record SailingView(String id, String from, String fromName, String to, String toName,
                          String departsAt, int passengerCapacity, int vehicleCapacity,
                          int passengerPlacesLeft, int vehiclePlacesLeft, boolean full) {

    public static SailingView of(SailingAvailability availability) {
        Sailing sailing = availability.sailing();
        return new SailingView(sailing.id(), sailing.from().name(), sailing.from().displayName(),
                sailing.to().name(), sailing.to().displayName(),
                MaltaTime.format(sailing.departsAt()), sailing.passengerCapacity(),
                sailing.vehicleCapacity(), availability.passengerPlacesLeft(),
                availability.vehiclePlacesLeft(), availability.isFull());
    }

    /** A sailing with nothing known about what is booked on it. */
    public static SailingView of(Sailing sailing) {
        return of(new SailingAvailability(sailing, 0, 0));
    }
}
