package mt.edu.um.cps3230.ferry.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Works out what a booking costs. Every age is taken against the date of the
 * sailing, so the price of a booking never moves on its own.
 */
public final class PriceCalculator {

    public static final Money ADULT_FARE = Money.ofEuros(4, 65);
    public static final Money CHILD_FARE = Money.ofEuros(1, 15);
    public static final Money RESIDENT_FARE = Money.ofEuros(1, 15);
    public static final Money VEHICLE_FARE = Money.ofEuros(15, 70);

    /** From this many fare paying passengers, the group rate applies. */
    public static final int GROUP_SIZE = 10;

    /** How much comes off the passenger total at the group rate. The vehicle pays full. */
    public static final int GROUP_DISCOUNT_PERCENT = 15;

    /**
     * @param booking the booking to price
     * @return what the booking costs
     */
    public Money priceFor(Booking booking) {
        return priceFor(booking.sailing().date(), booking.passengers(), booking.vehicle().isPresent());
    }

    /**
     * The same price, for a booking that does not exist yet.
     *
     * @param sailingDate the date every age is taken against
     * @param passengers the people travelling
     * @param withVehicle whether a vehicle is carried
     * @return what the booking would cost
     */
    public Money priceFor(LocalDate sailingDate, List<Passenger> passengers, boolean withVehicle) {
        Money total = Money.ZERO;
        int farePaying = 0;
        for (Passenger passenger : passengers) {
            if (passenger.isInfantOn(sailingDate)) {
                continue;
            }
            farePaying++;
            total = total.plus(fareFor(passenger, sailingDate));
        }
        if (farePaying >= GROUP_SIZE) {
            total = total.lessPercent(GROUP_DISCOUNT_PERCENT);
        }
        return withVehicle ? total.plus(VEHICLE_FARE) : total;
    }

    private Money fareFor(Passenger passenger, LocalDate sailingDate) {
        if (passenger.gozoResident()) {
            return RESIDENT_FARE;
        }
        return passenger.isChildOn(sailingDate) ? CHILD_FARE : ADULT_FARE;
    }
}
