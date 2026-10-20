package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * One row for each case of a pairwise set for {@code practicals/models/fares.pict}:
 * every pair of values from any two parameters appears in at least one row,
 * in eleven rows instead of the 44 that every combination allowed by the
 * model's constraint would need. The
 * rows PICT prints may differ; any set that covers every pair does the job.
 *
 * <p>Each class of age is stood for by one age well inside it: an infant is
 * 1, a child 7, an adult 40. The boundaries are {@link FareBoundaryTest}'s
 * business. The prices are worked out from the rules, for example the fourth
 * row: a child (115) and ten adults (4650) are eleven fare paying passengers,
 * so 4765 less 15% is 4050.25, charged as 4050, plus the vehicle at 1570.
 */
class FarePairwiseTest {

    private static final LocalDate SAILING = LocalDate.of(2026, 10, 13);

    private final PriceCalculator calculator = new PriceCalculator();

    @ParameterizedTest(name = "lead {0}, resident {1}, {2} other adults, vehicle {3}: {4} cents")
    @CsvSource(delimiter = '|', value = {
        "infant | yes |  8 | yes | 5290",
        "infant | no  |  9 | no  | 4185",
        "child  | yes |  0 | no  |  115",
        "child  | no  | 10 | yes | 5620",
        "adult  | yes |  9 | yes | 5225",
        "adult  | no  |  8 | no  | 4185",
        "infant | yes | 10 | no  | 3953",
        "adult  | no  |  0 | yes | 2035",
        "child  | yes |  8 | yes | 5405",
        "child  | yes |  9 | yes | 5225",
        "adult  | yes | 10 | yes | 5620"
    })
    void shouldChargeWhatTheRulesSayWhenTheRulesMeetInPairs(String leadAge, String leadResident,
                                                             int otherAdults, String vehicle, long cents) {
        List<Passenger> party = new ArrayList<>();
        party.add(new Passenger("Lead", SAILING.minusYears(yearsFor(leadAge)), "yes".equals(leadResident)));
        IntStream.range(0, otherAdults)
                .forEach(index -> party.add(new Passenger("Adult " + index, SAILING.minusYears(40), false)));

        Money price = calculator.priceFor(SAILING, party, "yes".equals(vehicle));

        assertThat(price).isEqualTo(Money.ofCents(cents));
    }

    private static int yearsFor(String ageClass) {
        return switch (ageClass) {
            case "infant" -> 1;
            case "child" -> 7;
            case "adult" -> 40;
            default -> throw new IllegalArgumentException("no such class of age: " + ageClass);
        };
    }
}
