package mt.edu.um.cps3230.ferry.web;

import mt.edu.um.cps3230.ferry.domain.Passenger;

/** One passenger as the API prints it. */
public record PassengerView(String name, String dateOfBirth, boolean gozoResident) {

    public static PassengerView of(Passenger passenger) {
        return new PassengerView(passenger.name(), passenger.dateOfBirth().toString(),
                passenger.gozoResident());
    }
}
