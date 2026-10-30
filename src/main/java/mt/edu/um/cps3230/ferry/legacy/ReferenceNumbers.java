package mt.edu.um.cps3230.ferry.legacy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import mt.edu.um.cps3230.ferry.domain.BookingReferences;

/**
 * Hands out the references printed on booking tickets, such as
 * {@code FF-K7M2QX}, and remembers every reference it has handed out so that
 * none is ever handed out twice.
 *
 * <p>The randomness is handed in, so a test can seed it or script it, and the
 * memory belongs to each instance, so two tests never share it. The
 * application makes one and gives it to {@code BookingService}; nothing needs
 * to be global for there to be only one.
 */
public final class ReferenceNumbers implements BookingReferences {

    /** No I, O, 0 or 1: they are too easily mistaken for each other on a ticket. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    private final Random random;
    private final Set<String> issued = new HashSet<>();

    /** References from an unseeded generator, as the kiosks need them. */
    public ReferenceNumbers() {
        this(new Random());
    }

    public ReferenceNumbers(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public synchronized String next() {
        String reference;
        do {
            StringBuilder characters = new StringBuilder("FF-");
            for (int count = 0; count < LENGTH; count++) {
                characters.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            reference = characters.toString();
        } while (!issued.add(reference));
        return reference;
    }

    /** How many references this generator has handed out. */
    public synchronized int issuedCount() {
        return issued.size();
    }
}
