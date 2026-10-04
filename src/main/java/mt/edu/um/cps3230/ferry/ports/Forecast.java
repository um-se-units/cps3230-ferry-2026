package mt.edu.um.cps3230.ferry.ports;

/**
 * The sea and wind conditions at one hour, as the cancellation policy needs them.
 *
 * @param waveHeightMetres significant wave height in metres
 * @param windGustKph highest wind gust in kilometres per hour
 */
public record Forecast(double waveHeightMetres, double windGustKph) {

    public Forecast {
        if (waveHeightMetres < 0) {
            throw new IllegalArgumentException("wave height cannot be negative: " + waveHeightMetres);
        }
        if (windGustKph < 0) {
            throw new IllegalArgumentException("wind gust cannot be negative: " + windGustKph);
        }
    }
}
