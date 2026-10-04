package mt.edu.um.cps3230.ferry.domain;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * One person on a booking. Age is always asked for against the date of the
 * sailing, never against today, so that a fare never changes while a booking
 * is being made.
 *
 * @param name the passenger's name
 * @param dateOfBirth the date the passenger was born
 * @param gozoResident whether the passenger holds Gozo residency
 */
public record Passenger(String name, LocalDate dateOfBirth, boolean gozoResident) {

    /** A passenger below this age travels free and does not pay a fare. */
    public static final int INFANT_AGE = 3;

    /** A passenger below this age, and at or above {@link #INFANT_AGE}, pays the child fare. */
    public static final int ADULT_AGE = 12;

    public Passenger {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a passenger needs a name");
        }
        Objects.requireNonNull(dateOfBirth, "a passenger needs a date of birth");
        name = name.trim();
    }

    /**
     * @param sailingDate the date of the sailing
     * @return the passenger's age in whole years on that date
     */
    public int ageOn(LocalDate sailingDate) {
        return Period.between(dateOfBirth, sailingDate).getYears();
    }

    public boolean isInfantOn(LocalDate sailingDate) {
        return ageOn(sailingDate) < INFANT_AGE;
    }

    public boolean isChildOn(LocalDate sailingDate) {
        int age = ageOn(sailingDate);
        return age >= INFANT_AGE && age < ADULT_AGE;
    }
}
