package mt.edu.um.cps3230.ferry.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingStatus;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.ports.Forecast;
import mt.edu.um.cps3230.ferry.ports.WeatherFeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The policy with its two seams in use: a fixed clock stands in for the time
 * and a Mockito stub for the forecast, so every test says exactly what the
 * sea is doing and when "now" is.
 */
@ExtendWith(MockitoExtension.class)
class LegacyCancellationPolicyTest {

    private static final ZoneId MALTA = ZoneId.of("Europe/Malta");
    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 10, 20, 7, 45);
    private static final Sailing SAILING = new Sailing("2026-10-20-0745-CIRKEWWA", Terminal.CIRKEWWA,
            Terminal.MGARR, DEPARTURE, 900, 140);

    @Mock
    private WeatherFeed weather;

    @ParameterizedTest(name = "waves {0} m and gusts {1} km/h: {2}")
    @CsvSource({
        "2.5, 10, CANCELLED",
        "0.5, 60, CANCELLED",
        "1.8, 10, AT_RISK",
        "0.5, 45, AT_RISK",
        "2.4, 59, AT_RISK",
        "1.7, 44, SAILS"
    })
    void shouldDecideFromTheForecastWhenTheSailingLeavesWithinTwoDays(double waves, double gusts,
                                                                     SailingStatus expected) {
        when(weather.forecastAt(DEPARTURE)).thenReturn(new Forecast(waves, gusts));

        assertThat(policyAt(DEPARTURE.minusDays(1)).decide(SAILING)).isEqualTo(expected);
    }

    @Test
    void shouldAskForTheForecastAtTheDepartureWhenJudging() {
        when(weather.forecastAt(DEPARTURE)).thenReturn(new Forecast(0.4, 12));

        policyAt(DEPARTURE.minusHours(3)).decide(SAILING);

        verify(weather).forecastAt(DEPARTURE);
    }

    @Test
    void shouldLetTheSailingGoWithoutAForecastWhenItLeavesMoreThanTwoDaysFromNow() {
        SailingStatus status = policyAt(DEPARTURE.minusHours(48).minusMinutes(1)).decide(SAILING);

        assertThat(status).isEqualTo(SailingStatus.SAILS);
        verifyNoInteractions(weather);
    }

    @Test
    void shouldJudgeTheSailingWhenItLeavesExactlyTwoDaysFromNow() {
        when(weather.forecastAt(DEPARTURE)).thenReturn(new Forecast(3.0, 70));

        assertThat(policyAt(DEPARTURE.minusHours(48)).decide(SAILING)).isEqualTo(SailingStatus.CANCELLED);
    }

    @ParameterizedTest(name = "{0} minutes after it left")
    @ValueSource(ints = {0, 1, 600})
    void shouldRefuseToJudgeTheSailingWhenItHasAlreadyLeft(int minutesAfter) {
        LegacyCancellationPolicy policy = policyAt(DEPARTURE.plusMinutes(minutesAfter));

        assertThatIllegalStateException().isThrownBy(() -> policy.decide(SAILING));
        verifyNoInteractions(weather);
    }

    private LegacyCancellationPolicy policyAt(LocalDateTime now) {
        return new LegacyCancellationPolicy(Clock.fixed(now.atZone(MALTA).toInstant(), MALTA), weather);
    }
}
