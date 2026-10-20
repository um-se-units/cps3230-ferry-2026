package mt.edu.um.cps3230.ferry.adapters.weather;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import mt.edu.um.cps3230.ferry.ports.Forecast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the adapter against the answers recorded in src/test/resources/wiremock. */
class OpenMeteoWeatherFeedTest {

    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 10, 6, 7, 45);
    private static final String MARINE_PATH = "/v1/marine";
    private static final String FORECAST_PATH = "/v1/forecast";

    private WireMockServer openMeteo;
    private OpenMeteoWeatherFeed feed;

    @BeforeEach
    void startTheStub() {
        openMeteo = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        openMeteo.start();
        feed = new OpenMeteoWeatherFeed(HttpClient.newHttpClient(),
                openMeteo.baseUrl() + MARINE_PATH, openMeteo.baseUrl() + FORECAST_PATH,
                OpenMeteoWeatherFeed.LATITUDE, OpenMeteoWeatherFeed.LONGITUDE);
    }

    @AfterEach
    void stopTheStub() {
        openMeteo.stop();
    }

    @Test
    void shouldReadTheHourOfTheDepartureWhenTheDayIsCalm() {
        given(MARINE_PATH, 200, recorded("calm-marine.json"));
        given(FORECAST_PATH, 200, recorded("calm-forecast.json"));

        Forecast forecast = feed.forecastAt(DEPARTURE);

        assertThat(forecast.waveHeightMetres()).isEqualTo(0.41);
        assertThat(forecast.windGustKph()).isEqualTo(11.2);
    }

    @Test
    void shouldReadTheHourOfTheDepartureWhenThereIsAStorm() {
        given(MARINE_PATH, 200, recorded("storm-marine.json"));
        given(FORECAST_PATH, 200, recorded("storm-forecast.json"));

        Forecast forecast = feed.forecastAt(DEPARTURE);

        assertThat(forecast.waveHeightMetres()).isEqualTo(3.24);
        assertThat(forecast.windGustKph()).isEqualTo(76.2);
    }

    @Test
    void shouldTakeTheReadingForTheRightHourWhenTheWholeDayIsReturned() {
        given(MARINE_PATH, 200, recorded("calm-marine.json"));
        given(FORECAST_PATH, 200, recorded("calm-forecast.json"));

        assertThat(feed.forecastAt(DEPARTURE.withHour(0)).waveHeightMetres()).isEqualTo(0.34);
        assertThat(feed.forecastAt(DEPARTURE.withHour(12)).waveHeightMetres()).isEqualTo(0.48);
    }

    @Test
    void shouldRefuseWhenTheServiceIsBroken() {
        given(MARINE_PATH, 500, recorded("server-error.json"));
        given(FORECAST_PATH, 200, recorded("calm-forecast.json"));

        assertThatExceptionOfType(WeatherUnavailableException.class)
                .isThrownBy(() -> feed.forecastAt(DEPARTURE))
                .withMessageContaining("answered 500");
    }

    @Test
    void shouldRefuseWhenTheAnswerIsNotTheJsonItExpects() {
        given(MARINE_PATH, 200, recorded("malformed-body.json"));
        given(FORECAST_PATH, 200, recorded("calm-forecast.json"));

        assertThatExceptionOfType(WeatherUnavailableException.class)
                .isThrownBy(() -> feed.forecastAt(DEPARTURE));
    }

    @Test
    void shouldRefuseWhenNoReadingCoversTheDeparture() {
        given(MARINE_PATH, 200, recorded("calm-marine.json"));
        given(FORECAST_PATH, 200, recorded("calm-forecast.json"));

        assertThatExceptionOfType(WeatherUnavailableException.class)
                .isThrownBy(() -> feed.forecastAt(DEPARTURE.plusDays(3)))
                .withMessageContaining("did not report");
    }

    @Test
    void shouldAskForBothTheWavesAndTheGustsWhenAForecastIsNeeded() {
        given(MARINE_PATH, 200, recorded("calm-marine.json"));
        given(FORECAST_PATH, 200, recorded("calm-forecast.json"));

        feed.forecastAt(DEPARTURE);

        openMeteo.verify(com.github.tomakehurst.wiremock.client.WireMock
                .getRequestedFor(urlPathEqualTo(MARINE_PATH)));
        openMeteo.verify(com.github.tomakehurst.wiremock.client.WireMock
                .getRequestedFor(urlPathEqualTo(FORECAST_PATH)));
    }

    private void given(String path, int status, String body) {
        openMeteo.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    private static String recorded(String name) {
        try (InputStream source = OpenMeteoWeatherFeedTest.class
                .getResourceAsStream("/wiremock/" + name)) {
            if (source == null) {
                throw new IllegalStateException("no recording called " + name);
            }
            return new String(source.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("could not read the recording " + name, failure);
        }
    }
}
