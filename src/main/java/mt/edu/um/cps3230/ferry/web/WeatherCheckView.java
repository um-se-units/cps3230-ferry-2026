package mt.edu.um.cps3230.ferry.web;

import mt.edu.um.cps3230.ferry.domain.SailingStatus;

/** What running the cancellation policy over a sailing decided. */
public record WeatherCheckView(String sailingId, String status) {

    public static WeatherCheckView of(String sailingId, SailingStatus status) {
        return new WeatherCheckView(sailingId, status.name());
    }
}
