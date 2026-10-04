package mt.edu.um.cps3230.ferry.web;

import java.util.List;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.domain.Money;
import mt.edu.um.cps3230.ferry.domain.Vehicle;

/** One booking as the API prints it. */
public record BookingView(String reference, SailingView sailing, List<PassengerView> passengers,
                          String vehicleRegistration, String contactEmail, String status,
                          String reservedAt, long priceCents, String price) {

    public static BookingView of(Booking booking, Money price) {
        return new BookingView(booking.reference(), SailingView.of(booking.sailing()),
                booking.passengers().stream().map(PassengerView::of).toList(),
                booking.vehicle().map(Vehicle::registration).orElse(null), booking.contactEmail(),
                booking.status().name(), MaltaTime.format(booking.reservedAt()), price.cents(),
                price.toString());
    }
}
