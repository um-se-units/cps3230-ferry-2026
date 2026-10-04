package mt.edu.um.cps3230.ferry.adapters.weather;

import java.time.LocalDateTime;
import java.util.Objects;
import mt.edu.um.cps3230.ferry.ports.Forecast;
import mt.edu.um.cps3230.ferry.ports.WeatherFeed;

/** Reports the same conditions whatever the hour, so that a test can set them. */
public final class FixedWeatherFeed implements WeatherFeed {

    private final Forecast forecast;

    public FixedWeatherFeed(Forecast forecast) {
        this.forecast = Objects.requireNonNull(forecast, "forecast");
    }

    public FixedWeatherFeed(double waveHeightMetres, double windGustKph) {
        this(new Forecast(waveHeightMetres, windGustKph));
    }

    /** A calm day: the sailing goes ahead. */
    public static FixedWeatherFeed calm() {
        return new FixedWeatherFeed(0.4, 12);
    }

    @Override
    public Forecast forecastAt(LocalDateTime departure) {
        return forecast;
    }
}
