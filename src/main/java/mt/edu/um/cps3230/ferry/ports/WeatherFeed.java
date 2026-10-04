package mt.edu.um.cps3230.ferry.ports;

import java.time.LocalDateTime;

/** Where the forecast for a departure comes from. */
public interface WeatherFeed {

    /**
     * The conditions expected at the hour of the given departure.
     *
     * @param departure the local departure time of a sailing
     * @return the forecast for that hour
     */
    Forecast forecastAt(LocalDateTime departure);
}
