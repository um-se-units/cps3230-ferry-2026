package mt.edu.um.cps3230.ferry.web;

import mt.edu.um.cps3230.ferry.domain.Money;

/** A price with no booking behind it. */
public record QuoteView(long priceCents, String price) {

    public static QuoteView of(Money money) {
        return new QuoteView(money.cents(), money.toString());
    }
}
