package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;

import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.adapters.weather.FixedWeatherFeed;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * MC/DC for {@link CancellationPolicy#decide}, with the operator's thresholds.
 *
 * <pre>
 * A  waves at least 2.5 m     first decision, A || B: CANCELLED
 * B  gusts at least 60 km/h
 * C  waves at least 1.8 m     second decision, C || D: AT_RISK, reached only when A and B are false
 * D  gusts at least 45 km/h
 *
 * Row  Waves  Gusts  A B C D  Status
 *  1    1.0    20    F F F F  SAILS
 *  2    2.5    20    T - - -  CANCELLED
 *  3    1.0    60    F T - -  CANCELLED
 *  4    1.8    20    F F T F  AT_RISK
 *  5    1.0    45    F F F T  AT_RISK      (- is not evaluated)
 *
 * Condition  Pair    The one input that changes
 * A          1, 2    waves 1.0 to 2.5
 * B          1, 3    gusts 20 to 60
 * C          1, 4    waves 1.0 to 1.8
 * D          1, 5    gusts 20 to 45
 * </pre>
 *
 * <p>Row 1, with every condition false, is in all four pairs, so four
 * conditions need five rows. Every row that makes a condition true sits on its
 * threshold, so the same rows also catch a {@code >=} written as {@code >}.
 *
 * <p>The sheet's questions. A implies C and B implies D, so the rows with A
 * true and C false cannot happen; no pair needs one, because the second
 * decision is only ever reached with A and B false. B is not evaluated when A
 * is true, so row 2 would take the same path whatever its gusts; keeping them
 * at 20 makes rows 1 and 2 differ in one input, which is what the pair needs.
 *
 * <p>The five rows on the sheet in task 3 take all eight branches too, but hold
 * no pair for A or for C: every row with high waves also has gusts high enough
 * to reach the same status on their own. A {@code decide} that ignored the
 * waves altogether would pass all five. Here it fails rows 2 and 4.
 */
class CancellationPolicyMcdcTest {

    @ParameterizedTest(name = "row {0}: waves {1} m and gusts {2} km/h mean {3}")
    @CsvSource({
        "1, 1.0, 20, SAILS",
        "2, 2.5, 20, CANCELLED",
        "3, 1.0, 60, CANCELLED",
        "4, 1.8, 20, AT_RISK",
        "5, 1.0, 45, AT_RISK"
    })
    void shouldLetEachConditionDecideOnItsOwnWhenTheOthersAreFalse(
            int row, double waveHeightMetres, double windGustKph, SailingStatus expected) {
        CancellationPolicy policy = new CancellationPolicy(
                new FixedWeatherFeed(waveHeightMetres, windGustKph),
                new InMemoryBookingRepository(), new FakePaymentGateway(),
                new InMemoryNotificationGateway(), new PriceCalculator());

        assertThat(policy.decide(sailing())).isEqualTo(expected);
    }
}
