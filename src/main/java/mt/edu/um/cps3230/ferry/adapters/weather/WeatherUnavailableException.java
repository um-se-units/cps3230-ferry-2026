package mt.edu.um.cps3230.ferry.adapters.weather;

/** Thrown when the forecast cannot be read, whatever the reason. */
public class WeatherUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WeatherUnavailableException(String message) {
        super(message);
    }

    public WeatherUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
