package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Properties of the fare rules: things that hold for every party, checked on
 * a thousand generated ones each. Parties run up to fifteen people, so that
 * many of them are big enough for the group rate.
 */
class FarePropertiesTest {

    private static final LocalDate SAILING = LocalDate.of(2026, 10, 13);
    private static final Money VEHICLE_FARE = Money.ofEuros(15, 70);

    private final PriceCalculator calculator = new PriceCalculator();

    @Property
    void shouldAddExactlyTheVehicleFareWhenAVehicleIsAdded(@ForAll("parties") List<Passenger> party) {
        Money without = calculator.priceFor(SAILING, party, false);
        Money with = calculator.priceFor(SAILING, party, true);

        assertThat(with).isEqualTo(without.plus(VEHICLE_FARE));
    }

    @Property
    void shouldKeepThePriceWhenAnInfantJoinsTheParty(@ForAll("parties") List<Passenger> party,
                                                      @ForAll("infants") Passenger infant) {
        List<Passenger> withInfant = new ArrayList<>(party);
        withInfant.add(infant);

        assertThat(calculator.priceFor(SAILING, withInfant, false))
                .isEqualTo(calculator.priceFor(SAILING, party, false));
    }

    @Property
    void shouldKeepThePriceWhenThePassengersAreListedInAnotherOrder(@ForAll("parties") List<Passenger> party,
                                                                    @ForAll Random random) {
        List<Passenger> shuffled = new ArrayList<>(party);
        Collections.shuffle(shuffled, random);

        assertThat(calculator.priceFor(SAILING, shuffled, false))
                .isEqualTo(calculator.priceFor(SAILING, party, false));
    }

    @Provide
    Arbitrary<List<Passenger>> parties() {
        return passengersAged(0, 90).list().ofMaxSize(15);
    }

    @Provide
    Arbitrary<Passenger> infants() {
        return passengersAged(0, 2);
    }

    /** Anyone whose age on the sailing date is in the range, born on any day of that year of age. */
    private static Arbitrary<Passenger> passengersAged(int youngest, int oldest) {
        return Combinators.combine(
                        Arbitraries.integers().between(youngest, oldest),
                        Arbitraries.integers().between(0, 364),
                        Arbitraries.of(true, false))
                .as((age, daysSinceBirthday, resident) -> new Passenger("Passenger",
                        SAILING.minusYears(age).minusDays(daysSinceBirthday), resident));
    }
}
