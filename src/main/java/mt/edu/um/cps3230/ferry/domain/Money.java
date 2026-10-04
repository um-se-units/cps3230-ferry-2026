package mt.edu.um.cps3230.ferry.domain;

/**
 * An amount in euro, held as a whole number of cents so that no fare is ever
 * rounded away. Amounts are never negative: a refund is a positive amount
 * returned, not a negative one taken.
 *
 * @param cents the amount in euro cents
 */
public record Money(long cents) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("an amount cannot be negative: " + cents);
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    /**
     * @param euros whole euro
     * @param cents cents, 0 to 99
     * @return the amount
     */
    public static Money ofEuros(long euros, int cents) {
        if (cents < 0 || cents > 99) {
            throw new IllegalArgumentException("cents must be between 0 and 99: " + cents);
        }
        return new Money(euros * 100 + cents);
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }

    /**
     * Takes a percentage off, rounding half up to the nearest cent.
     *
     * @param percent the percentage to take off, 0 to 100
     * @return what is left
     */
    public Money lessPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("a percentage must be between 0 and 100: " + percent);
        }
        return new Money((cents * (100 - percent) + 50) / 100);
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }

    @Override
    public String toString() {
        return String.format("%s%d.%02d", "€", cents / 100, cents % 100);
    }
}
