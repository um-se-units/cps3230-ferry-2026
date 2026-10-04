package mt.edu.um.cps3230.ferry.ports;

/**
 * The outcome of one attempt to charge a card.
 *
 * @param approved whether the payment went through
 * @param reference the payment reference, needed later to refund it, or null when declined
 */
public record PaymentResult(boolean approved, String reference) {

    public static PaymentResult approvedWith(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("an approved payment needs a reference");
        }
        return new PaymentResult(true, reference);
    }

    public static PaymentResult declined() {
        return new PaymentResult(false, null);
    }
}
