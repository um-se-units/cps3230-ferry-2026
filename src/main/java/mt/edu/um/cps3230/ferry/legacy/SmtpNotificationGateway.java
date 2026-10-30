package mt.edu.um.cps3230.ferry.legacy;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;

/**
 * Sends a message straight to the operator's mail server. This is the code
 * that used to sit inside {@link LegacyBookingConfirmer}, moved behind the
 * port unchanged except in one way: a failure is thrown, not swallowed, so
 * that the confirmer can try again. It stays untested here; it is the thin
 * edge a fake gateway stands in for.
 */
public final class SmtpNotificationGateway implements NotificationGateway {

    private static final String MAIL_SERVER = "mail.fliegu.example";
    private static final int MAIL_PORT = 25;

    @Override
    public void send(String emailAddress, String message) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(MAIL_SERVER, MAIL_PORT), 2000);
            PrintWriter mail = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            mail.print("HELO fliegu.example\r\n");
            mail.print("MAIL FROM:<bookings@fliegu.example>\r\n");
            mail.print("RCPT TO:<" + emailAddress + ">\r\n");
            mail.print("DATA\r\n");
            mail.print("Subject: Your Fliegu Ferries booking\r\n\r\n");
            mail.print(message + "\r\n.\r\n");
            mail.print("QUIT\r\n");
            mail.flush();
        } catch (IOException failure) {
            throw new UncheckedIOException("could not send the message to " + emailAddress, failure);
        }
    }
}
