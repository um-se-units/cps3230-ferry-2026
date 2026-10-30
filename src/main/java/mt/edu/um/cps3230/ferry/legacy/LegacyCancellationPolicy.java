package mt.edu.um.cps3230.ferry.legacy;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import mt.edu.um.cps3230.ferry.adapters.weather.OpenMeteoWeatherFeed;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingStatus;
import mt.edu.um.cps3230.ferry.ports.Forecast;
import mt.edu.um.cps3230.ferry.ports.WeatherFeed;

/**
 * Decides whether a sailing goes ahead, as the terminal kiosks have always
 * done it: it reads the sea and the wind at the departure hour and compares
 * them with the operator's thresholds.
 *
 * <p>A sailing that has already left cannot be judged. A sailing more than
 * {@value #FORECAST_HORIZON_HOURS} hours away is not judged yet, because the
 * forecast is not trusted that far ahead: it sails until a later check says
 * otherwise.
 *
 * <p>Both of the things it used to reach for, the time and the forecast, are
 * now handed to it, so a test can fix the one and stub the other.
 */
public class LegacyCancellationPolicy {

    static final int FORECAST_HORIZON_HOURS = 48;

    private final Clock clock;
    private final WeatherFeed weather;

    /**
     * The policy the kiosks run: the real forecast, on Malta time. The old
     * code read the time in whatever zone the machine was set to, which was
     * only right on a machine set to Malta.
     */
    public LegacyCancellationPolicy() {
        this(Clock.system(ZoneId.of("Europe/Malta")), new OpenMeteoWeatherFeed(HttpClient.newHttpClient()));
    }

    public LegacyCancellationPolicy(Clock clock, WeatherFeed weather) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.weather = Objects.requireNonNull(weather, "weather");
    }

    public SailingStatus decide(Sailing sailing) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!sailing.departsAt().isAfter(now)) {
            throw new IllegalStateException("sailing " + sailing.id() + " has already left");
        }
        if (sailing.departsAt().isAfter(now.plusHours(FORECAST_HORIZON_HOURS))) {
            return SailingStatus.SAILS;
        }

        Forecast forecast = weather.forecastAt(sailing.departsAt());
        if (forecast.waveHeightMetres() >= 2.5 || forecast.windGustKph() >= 60) {
            return SailingStatus.CANCELLED;
        }
        if (forecast.waveHeightMetres() >= 1.8 || forecast.windGustKph() >= 45) {
            return SailingStatus.AT_RISK;
        }
        return SailingStatus.SAILS;
    }
}
