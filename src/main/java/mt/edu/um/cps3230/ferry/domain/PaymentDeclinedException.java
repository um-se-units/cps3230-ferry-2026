package mt.edu.um.cps3230.ferry.domain;

/** Thrown when the payment gateway turns a card down. */
public class PaymentDeclinedException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
