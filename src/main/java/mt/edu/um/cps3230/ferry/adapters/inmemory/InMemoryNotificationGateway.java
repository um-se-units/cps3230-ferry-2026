package mt.edu.um.cps3230.ferry.adapters.inmemory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;

/** Keeps every message it is given, so that a test can read them back. */
public final class InMemoryNotificationGateway implements NotificationGateway {

    private final List<String> recipients = new CopyOnWriteArrayList<>();
    private final List<String> messages = new CopyOnWriteArrayList<>();

    @Override
    public void send(String emailAddress, String message) {
        recipients.add(emailAddress);
        messages.add(message);
    }

    /** The addresses written to, in the order they were written to. */
    public List<String> recipients() {
        return List.copyOf(recipients);
    }

    /** The messages sent to one address, in the order they were sent. */
    public List<String> messagesTo(String emailAddress) {
        return IntStream.range(0, recipients.size())
                .filter(index -> recipients.get(index).equals(emailAddress))
                .mapToObj(messages::get)
                .toList();
    }

    public int sentCount() {
        return messages.size();
    }
}
