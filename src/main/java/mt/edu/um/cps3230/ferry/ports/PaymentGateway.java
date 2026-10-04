package mt.edu.um.cps3230.ferry.ports;

import mt.edu.um.cps3230.ferry.domain.Money;

/** How the application takes and returns money. */
public interface PaymentGateway {

    /**
     * Charges a card.
     *
     * @param cardNumber the card to charge
     * @param amount the amount to take
     * @return an approved result carrying a payment reference, or a declined one
     */
    PaymentResult charge(String cardNumber, Money amount);

    /**
     * Returns money taken by an earlier charge.
     *
     * @param paymentReference the reference from the approved {@link PaymentResult}
     * @param amount the amount to return
     */
    void refund(String paymentReference, Money amount);
}
