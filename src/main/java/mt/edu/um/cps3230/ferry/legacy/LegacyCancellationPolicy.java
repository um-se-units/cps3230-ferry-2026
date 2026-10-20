package mt.edu.um.cps3230.ferry.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingStatus;

/**
 * Decides whether a sailing goes ahead, as the terminal kiosks have always
 * done it: it asks Open-Meteo for the sea and the wind at the departure hour
 * and compares them with the operator's thresholds.
 *
 * <p>A sailing that has already left cannot be judged. A sailing more than
 * {@value #FORECAST_HORIZON_HOURS} hours away is not judged yet, because the
 * forecast is not trusted that far ahead: it sails until a later check says
 * otherwise.
 */
public class LegacyCancellationPolicy {

    static final int FORECAST_HORIZON_HOURS = 48;

    public SailingStatus decide(Sailing sailing) {
        LocalDateTime now = LocalDateTime.now();
        if (!sailing.departsAt().isAfter(now)) {
            throw new IllegalStateException("sailing " + sailing.id() + " has already left");
        }
        if (sailing.departsAt().isAfter(now.plusHours(FORECAST_HORIZON_HOURS))) {
            return SailingStatus.SAILS;
        }

        HttpClient client = HttpClient.newHttpClient();
        String day = sailing.date().toString();
        String hour = sailing.departsAt().truncatedTo(ChronoUnit.HOURS).toString();
        String where = "?latitude=36.02&longitude=14.30&timezone=Europe%2FMalta&start_date=" + day
                + "&end_date=" + day;
        double waveHeight = reading(client, "https://marine-api.open-meteo.com/v1/marine" + where
                + "&hourly=wave_height", "wave_height", hour);
        double gusts = reading(client, "https://api.open-meteo.com/v1/forecast" + where
                + "&hourly=wind_gusts_10m", "wind_gusts_10m", hour);

        if (waveHeight >= 2.5 || gusts >= 60) {
            return SailingStatus.CANCELLED;
        }
        if (waveHeight >= 1.8 || gusts >= 45) {
            return SailingStatus.AT_RISK;
        }
        return SailingStatus.SAILS;
    }

    private double reading(HttpClient client, String url, String measure, String hour) {
        try {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode hourly = new ObjectMapper().readTree(response.body()).path("hourly");
            JsonNode times = hourly.path("time");
            for (int index = 0; index < times.size(); index++) {
                if (times.get(index).asText().equals(hour)) {
                    return hourly.path(measure).get(index).asDouble();
                }
            }
            throw new IllegalStateException("Open-Meteo has no " + measure + " for " + hour);
        } catch (IOException | InterruptedException failure) {
            throw new IllegalStateException("could not reach Open-Meteo", failure);
        }
    }
}
