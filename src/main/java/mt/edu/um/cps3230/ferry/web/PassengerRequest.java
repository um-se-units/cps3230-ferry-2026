package mt.edu.um.cps3230.ferry.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import mt.edu.um.cps3230.ferry.domain.Passenger;

/** One passenger as a request sends them. */
public record PassengerRequest(String name, String dateOfBirth, boolean gozoResident) {

    public Passenger toPassenger() {
        try {
            return new Passenger(name, LocalDate.parse(dateOfBirth), gozoResident);
        } catch (NullPointerException | DateTimeParseException badDate) {
            throw new IllegalArgumentException(
                    "a passenger needs a date of birth as YYYY-MM-DD: " + dateOfBirth);
        }
    }
}
