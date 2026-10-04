package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adults;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.aged;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationWithVehicle;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void shouldChargeTheStandardFareWhenThePassengerIsAnAdult() {
        Money price = calculator.priceFor(reservationFor(adult()));

        assertThat(price).isEqualTo(Money.ofEuros(4, 65));
    }

    @Test
    void shouldChargeTheChildFareWhenThePassengerIsUnderTwelve() {
        Money price = calculator.priceFor(reservationFor(aged(7)));

        assertThat(price).isEqualTo(Money.ofEuros(1, 15));
    }

    @Test
    void shouldChargeNothingWhenThePassengerIsAnInfant() {
        Money price = calculator.priceFor(reservationFor(aged(1)));

        assertThat(price).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldTakeFifteenPercentOffWhenTenPassengersPayAFare() {
        Money price = calculator.priceFor(reservationFor(adults(10)));

        assertThat(price).isEqualTo(Money.ofEuros(39, 53));
    }

    @Test
    void shouldAddTheVehicleFareWhenTheBookingCarriesAVehicle() {
        Money price = calculator.priceFor(reservationWithVehicle(adult()));

        assertThat(price).isEqualTo(Money.ofEuros(20, 35));
    }
}
