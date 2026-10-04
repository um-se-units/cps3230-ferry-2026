package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.sailing;
import static org.assertj.core.api.Assertions.assertThat;

import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryBookingRepository;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.adapters.payment.FakePaymentGateway;
import mt.edu.um.cps3230.ferry.adapters.weather.FixedWeatherFeed;
import org.junit.jupiter.api.Test;

class CancellationPolicyTest {

    @Test
    void shouldCancelTheSailingWhenTheWavesAreTooHigh() {
        CancellationPolicy policy = policyFacing(3.0, 20);

        assertThat(policy.decide(sailing())).isEqualTo(SailingStatus.CANCELLED);
    }

    @Test
    void shouldPutTheSailingAtRiskWhenTheGustsPickUp() {
        CancellationPolicy policy = policyFacing(0.5, 50);

        assertThat(policy.decide(sailing())).isEqualTo(SailingStatus.AT_RISK);
    }

    @Test
    void shouldLetTheSailingGoWhenTheSeaIsCalm() {
        CancellationPolicy policy = policyFacing(0.4, 12);

        assertThat(policy.decide(sailing())).isEqualTo(SailingStatus.SAILS);
    }

    private CancellationPolicy policyFacing(double waveHeightMetres, double windGustKph) {
        return new CancellationPolicy(new FixedWeatherFeed(waveHeightMetres, windGustKph),
                new InMemoryBookingRepository(), new FakePaymentGateway(),
                new InMemoryNotificationGateway(), new PriceCalculator());
    }
}
