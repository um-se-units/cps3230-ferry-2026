package mt.edu.um.cps3230.ferry.legacy;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import mt.edu.um.cps3230.ferry.domain.BookingReferences;

/**
 * Hands out the references printed on booking tickets, such as
 * {@code FF-K7M2QX}. There is one for the whole application, reached with
 * {@link #getInstance()}, and it remembers every reference it has handed out
 * so that none is ever handed out twice. The terminal kiosks give it to
 * {@code BookingService} as that service's {@link BookingReferences}.
 */
public final class ReferenceNumbers implements BookingReferences {

    /** No I, O, 0 or 1: they are too easily mistaken for each other on a ticket. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    private static final ReferenceNumbers INSTANCE = new ReferenceNumbers();
    private static final Random RANDOM = new Random();
    private static final Set<String> ISSUED = new HashSet<>();

    private ReferenceNumbers() {
    }

    public static ReferenceNumbers getInstance() {
        return INSTANCE;
    }

    @Override
    public String next() {
        synchronized (ISSUED) {
            String reference;
            do {
                StringBuilder characters = new StringBuilder("FF-");
                for (int count = 0; count < LENGTH; count++) {
                    characters.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
                }
                reference = characters.toString();
            } while (!ISSUED.add(reference));
            return reference;
        }
    }

    /** How many references have been handed out since the application started. */
    public static int issuedCount() {
        synchronized (ISSUED) {
            return ISSUED.size();
        }
    }
}
