package mt.edu.um.cps3230.ferry.ports;

/** How the application reaches a customer. */
public interface NotificationGateway {

    /**
     * Sends one message to one contact.
     *
     * @param emailAddress the contact address held on the booking
     * @param message the text to send
     */
    void send(String emailAddress, String message);
}
