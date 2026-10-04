package mt.edu.um.cps3230.ferry.domain;

import java.util.Objects;
import mt.edu.um.cps3230.ferry.ports.BookingRepository;
import mt.edu.um.cps3230.ferry.ports.Forecast;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;
import mt.edu.um.cps3230.ferry.ports.PaymentGateway;
import mt.edu.um.cps3230.ferry.ports.WeatherFeed;

/**
 * Reads the forecast for a sailing and decides whether it goes ahead. The
 * thresholds are handed in, so a test can move them rather than move the
 * weather.
 */
public final class CancellationPolicy {

    public static final double CANCEL_WAVE_HEIGHT_METRES = 2.5;
    public static final double CANCEL_GUST_KPH = 60;
    public static final double AT_RISK_WAVE_HEIGHT_METRES = 1.8;
    public static final double AT_RISK_GUST_KPH = 45;

    private final WeatherFeed weather;
    private final BookingRepository bookings;
    private final PaymentGateway payments;
    private final NotificationGateway notifications;
    private final PriceCalculator prices;
    private final double cancelWaveHeightMetres;
    private final double cancelGustKph;
    private final double atRiskWaveHeightMetres;
    private final double atRiskGustKph;

    /** A policy using the operator's published thresholds. */
    public CancellationPolicy(WeatherFeed weather, BookingRepository bookings, PaymentGateway payments,
                              NotificationGateway notifications, PriceCalculator prices) {
        this(weather, bookings, payments, notifications, prices, CANCEL_WAVE_HEIGHT_METRES,
                CANCEL_GUST_KPH, AT_RISK_WAVE_HEIGHT_METRES, AT_RISK_GUST_KPH);
    }

    public CancellationPolicy(WeatherFeed weather, BookingRepository bookings, PaymentGateway payments,
                              NotificationGateway notifications, PriceCalculator prices,
                              double cancelWaveHeightMetres, double cancelGustKph,
                              double atRiskWaveHeightMetres, double atRiskGustKph) {
        this.weather = Objects.requireNonNull(weather, "weather");
        this.bookings = Objects.requireNonNull(bookings, "bookings");
        this.payments = Objects.requireNonNull(payments, "payments");
        this.notifications = Objects.requireNonNull(notifications, "notifications");
        this.prices = Objects.requireNonNull(prices, "prices");
        this.cancelWaveHeightMetres = cancelWaveHeightMetres;
        this.cancelGustKph = cancelGustKph;
        this.atRiskWaveHeightMetres = atRiskWaveHeightMetres;
        this.atRiskGustKph = atRiskGustKph;
    }

    /**
     * Reads the forecast and says what it means, changing nothing.
     *
     * @param sailing the sailing to judge
     * @return whether the sailing goes ahead
     */
    public SailingStatus decide(Sailing sailing) {
        Forecast forecast = weather.forecastAt(sailing.departsAt());
        if (forecast.waveHeightMetres() >= cancelWaveHeightMetres
                || forecast.windGustKph() >= cancelGustKph) {
            return SailingStatus.CANCELLED;
        }
        if (forecast.waveHeightMetres() >= atRiskWaveHeightMetres
                || forecast.windGustKph() >= atRiskGustKph) {
            return SailingStatus.AT_RISK;
        }
        return SailingStatus.SAILS;
    }

    /**
     * Decides, then acts: a cancelled sailing calls off every booking on it,
     * refunding the ones already paid for, and tells every contact. A sailing
     * at risk tells every contact and leaves the bookings alone.
     *
     * @param sailing the sailing to judge
     * @return what was decided
     */
    public SailingStatus apply(Sailing sailing) {
        SailingStatus status = decide(sailing);
        switch (status) {
            case CANCELLED -> callOffEveryBooking(sailing);
            case AT_RISK -> warnEveryContact(sailing);
            case SAILS -> { }
        }
        return status;
    }

    private void callOffEveryBooking(Sailing sailing) {
        for (Booking booking : bookings.findBySailingId(sailing.id())) {
            if (!booking.status().isActive()) {
                continue;
            }
            if (booking.status() == BookingStatus.CONFIRMED) {
                payments.refund(booking.paymentReference().orElseThrow(), prices.priceFor(booking));
            }
            bookings.save(booking.cancelled());
            notifications.send(booking.contactEmail(), "The " + sailing.departsAt() + " sailing from "
                    + sailing.from().displayName() + " is cancelled. Booking " + booking.reference()
                    + " has been called off.");
        }
    }

    private void warnEveryContact(Sailing sailing) {
        for (Booking booking : bookings.findBySailingId(sailing.id())) {
            if (!booking.status().isActive()) {
                continue;
            }
            notifications.send(booking.contactEmail(), "The " + sailing.departsAt() + " sailing from "
                    + sailing.from().displayName() + " may be called off. Booking "
                    + booking.reference() + " stands for now.");
        }
    }
}
