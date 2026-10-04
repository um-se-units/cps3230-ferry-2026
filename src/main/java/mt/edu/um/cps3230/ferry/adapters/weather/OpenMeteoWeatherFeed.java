package mt.edu.um.cps3230.ferry.adapters.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import mt.edu.um.cps3230.ferry.ports.Forecast;
import mt.edu.um.cps3230.ferry.ports.WeatherFeed;

/**
 * Reads the conditions in the channel from Open-Meteo: the wave height from the
 * marine API and the wind gusts from the forecast API, both asked for in Malta
 * local time so that the hour lines up with the departure.
 */
public final class OpenMeteoWeatherFeed implements WeatherFeed {

    /** Halfway between the two terminals. */
    public static final double LATITUDE = 36.02;
    public static final double LONGITUDE = 14.30;

    public static final String MARINE_API = "https://marine-api.open-meteo.com/v1/marine";
    public static final String FORECAST_API = "https://api.open-meteo.com/v1/forecast";

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final String marineApi;
    private final String forecastApi;
    private final double latitude;
    private final double longitude;

    /** A feed reading the real service for the crossing. */
    public OpenMeteoWeatherFeed(HttpClient http) {
        this(http, MARINE_API, FORECAST_API, LATITUDE, LONGITUDE);
    }

    public OpenMeteoWeatherFeed(HttpClient http, String marineApi, String forecastApi,
                                double latitude, double longitude) {
        this.http = Objects.requireNonNull(http, "http");
        this.marineApi = Objects.requireNonNull(marineApi, "marineApi");
        this.forecastApi = Objects.requireNonNull(forecastApi, "forecastApi");
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public Forecast forecastAt(LocalDateTime departure) {
        String hour = departure.format(HOUR);
        double waveHeight = readingAt(url(marineApi, "wave_height", departure), "wave_height", hour);
        double windGust = readingAt(url(forecastApi, "wind_gusts_10m", departure), "wind_gusts_10m", hour);
        return new Forecast(waveHeight, windGust);
    }

    private String url(String api, String measure, LocalDateTime departure) {
        String day = departure.toLocalDate().toString();
        return api + "?latitude=" + latitude + "&longitude=" + longitude
                + "&hourly=" + measure + "&timezone=Europe%2FMalta"
                + "&start_date=" + day + "&end_date=" + day;
    }

    private double readingAt(String url, String measure, String hour) {
        JsonNode body = fetch(url);
        JsonNode hourly = body.path("hourly");
        JsonNode times = hourly.path("time");
        JsonNode readings = hourly.path(measure);
        if (!times.isArray() || !readings.isArray() || times.isEmpty()) {
            throw new WeatherUnavailableException(
                    "Open-Meteo returned no " + measure + " readings for " + hour);
        }
        for (int index = 0; index < times.size(); index++) {
            if (hour.equals(times.get(index).asText())) {
                JsonNode reading = readings.get(index);
                if (reading == null || reading.isNull()) {
                    throw new WeatherUnavailableException(
                            "Open-Meteo had no " + measure + " reading for " + hour);
                }
                return reading.asDouble();
            }
        }
        throw new WeatherUnavailableException(
                "Open-Meteo did not report " + measure + " for " + hour);
    }

    private JsonNode fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WeatherUnavailableException(
                        "Open-Meteo answered " + response.statusCode() + " for " + url);
            }
            return JSON.readTree(response.body());
        } catch (IOException failure) {
            throw new WeatherUnavailableException("could not read " + url, failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new WeatherUnavailableException("interrupted while reading " + url, interrupted);
        }
    }
}
