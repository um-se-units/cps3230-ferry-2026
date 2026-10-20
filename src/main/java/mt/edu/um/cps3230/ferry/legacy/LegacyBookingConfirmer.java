package mt.edu.um.cps3230.ferry.legacy;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import mt.edu.um.cps3230.ferry.domain.Booking;

/**
 * Confirms a booking once it has been paid for, and e-mails the customer to
 * say so, as the terminal kiosks have always done it: straight to the
 * operator's mail server.
 */
public class LegacyBookingConfirmer {

    private static final String MAIL_SERVER = "mail.fliegu.example";
    private static final int MAIL_PORT = 25;

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

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(MAIL_SERVER, MAIL_PORT), 2000);
            PrintWriter mail = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            mail.print("HELO fliegu.example\r\n");
            mail.print("MAIL FROM:<bookings@fliegu.example>\r\n");
            mail.print("RCPT TO:<" + confirmed.contactEmail() + ">\r\n");
            mail.print("DATA\r\n");
            mail.print("Subject: Your Fliegu Ferries booking\r\n\r\n");
            mail.print(message + "\r\n.\r\n");
            mail.print("QUIT\r\n");
            mail.flush();
        } catch (IOException e) {
            // The customer can always ring the office.
        }
        return confirmed;
    }
}
