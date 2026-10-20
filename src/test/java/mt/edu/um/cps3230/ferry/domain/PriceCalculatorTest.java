package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adult;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.adults;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.aged;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationFor;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.reservationWithVehicle;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.resident;
import static mt.edu.um.cps3230.ferry.domain.Fixtures.residentAged;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();

    @ParameterizedTest(name = "a passenger aged {0} pays {1} cents")
    @CsvSource({
        "0, 0",
        "2, 0",
        "3, 115",
        "11, 115",
        "12, 465",
        "30, 465",
        "80, 465"
    })
    void shouldChargeByAgeOnTheSailingDateWhenOnePassengerTravels(int age, long cents) {
        assertThat(calculator.priceFor(reservationFor(aged(age)))).isEqualTo(Money.ofCents(cents));
    }

    @Test
    void shouldChargeTheResidentFareWhenThePassengerLivesInGozo() {
        assertThat(calculator.priceFor(reservationFor(resident()))).isEqualTo(Money.ofEuros(1, 15));
    }

    @Test
    void shouldStillChargeTheResidentFareWhenTheResidentIsAChild() {
        assertThat(calculator.priceFor(reservationFor(residentAged(7))))
                .isEqualTo(Money.ofEuros(1, 15));
    }

    @Test
    void shouldChargeNothingForTheInfantWhenAFamilyTravels() {
        Money price = calculator.priceFor(reservationFor(adult(), adult(), aged(7), aged(1)));

        assertThat(price).isEqualTo(Money.ofCents(465 + 465 + 115));
    }

    @Test
    void shouldAddTheVehicleFareWhenTheBookingCarriesAVehicle() {
        assertThat(calculator.priceFor(reservationWithVehicle(adult())))
                .isEqualTo(Money.ofEuros(20, 35));
    }

    @ParameterizedTest(name = "{0} fare paying passengers cost {1} cents")
    @CsvSource({
        "9, 4185",
        "10, 3953",
        "11, 4348"
    })
    void shouldTakeFifteenPercentOffWhenTenOrMorePassengersPayAFare(int howMany, long cents) {
        assertThat(calculator.priceFor(reservationFor(adults(howMany))))
                .isEqualTo(Money.ofCents(cents));
    }

    @Test
    void shouldNotCountInfantsTowardsTheGroupWhenTheGroupIsSized() {
        List<Passenger> nineAdultsAndTwoInfants = new ArrayList<>(adults(9));
        nineAdultsAndTwoInfants.add(aged(1));
        nineAdultsAndTwoInfants.add(aged(2));

        assertThat(calculator.priceFor(reservationFor(nineAdultsAndTwoInfants)))
                .isEqualTo(Money.ofCents(9 * 465));
    }

    @Test
    void shouldLeaveTheVehicleAtFullFareWhenTheGroupRateApplies() {
        Booking booking = Booking.reservation("FF-0003", Fixtures.sailing(), adults(10),
                java.util.Optional.of(Fixtures.CAR), Fixtures.CONTACT, Fixtures.DEPARTURE.minusDays(1));

        assertThat(calculator.priceFor(booking)).isEqualTo(Money.ofCents(3953 + 1570));
    }

    @Test
    void shouldPriceTheSameWhenNoBookingExistsYet() {
        Money quoted = calculator.priceFor(SAILING_DATE, List.of(adult(), aged(7)), true);

        assertThat(quoted).isEqualTo(calculator.priceFor(reservationWithVehicle(adult(), aged(7))));
    }
}
