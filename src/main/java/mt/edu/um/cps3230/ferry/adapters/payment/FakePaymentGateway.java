package mt.edu.um.cps3230.ferry.adapters.payment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import mt.edu.um.cps3230.ferry.domain.Money;
import mt.edu.um.cps3230.ferry.ports.PaymentGateway;
import mt.edu.um.cps3230.ferry.ports.PaymentResult;

/**
 * Stands in for a card processor. Every card is approved except one ending in
 * 0000, which is always turned down, so that a declined payment can be tried
 * without any real service being up.
 */
public final class FakePaymentGateway implements PaymentGateway {

    /** A card ending in this is always turned down. */
    public static final String DECLINED_CARD_ENDING = "0000";

    private final AtomicLong nextPayment = new AtomicLong();
    private final Map<String, Money> refunds = new ConcurrentHashMap<>();

    @Override
    public PaymentResult charge(String cardNumber, Money amount) {
        if (cardNumber == null || cardNumber.isBlank() || cardNumber.endsWith(DECLINED_CARD_ENDING)) {
            return PaymentResult.declined();
        }
        return PaymentResult.approvedWith("PAY-" + nextPayment.incrementAndGet());
    }

    @Override
    public void refund(String paymentReference, Money amount) {
        refunds.put(paymentReference, amount);
    }

    /** What has been refunded, by payment reference. */
    public Map<String, Money> refunds() {
        return Map.copyOf(refunds);
    }
}
