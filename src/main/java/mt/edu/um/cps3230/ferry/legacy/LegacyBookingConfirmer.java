package mt.edu.um.cps3230.ferry.legacy;

import java.util.Objects;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;

/**
 * Confirms a booking once it has been paid for, and tells the customer so.
 * How the message travels is the gateway's business; the socket code that
 * used to sit here is now {@link SmtpNotificationGateway}.
 *
 * <p>Sending is tried up to {@value #ATTEMPTS} times. If every attempt fails,
 * the failure from the last one reaches the caller instead of disappearing.
 */
public class LegacyBookingConfirmer {

    static final int ATTEMPTS = 3;

    private final NotificationGateway notifications;

    /** The confirmer the kiosks run, sending through the operator's mail server. */
    public LegacyBookingConfirmer() {
        this(new SmtpNotificationGateway());
    }

    public LegacyBookingConfirmer(NotificationGateway notifications) {
        this.notifications = Objects.requireNonNull(notifications, "notifications");
    }

    /**
     * @param booking a reserved booking
     * @param paymentReference the payment that was taken for it
     * @return the booking, confirmed
     */
    public Booking confirm(Booking booking, String paymentReference) {
        Booking confirmed = booking.confirmedWith(paymentReference);
        String message = "Booking " + confirmed.reference() + " is confirmed for the "
                + confirmed.sailing().departsAt() + " sailing from "
                + confirmed.sailing().from().displayName() + ".";
        sendTryingAgainOnFailure(confirmed.contactEmail(), message);
        return confirmed;
    }

    private void sendTryingAgainOnFailure(String emailAddress, String message) {
        for (int attempt = 1; ; attempt++) {
            try {
                notifications.send(emailAddress, message);
                return;
            } catch (RuntimeException failure) {
                if (attempt == ATTEMPTS) {
                    throw failure;
                }
            }
        }
    }
}
