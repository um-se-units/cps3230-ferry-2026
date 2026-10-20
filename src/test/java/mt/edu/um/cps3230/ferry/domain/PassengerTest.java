package mt.edu.um.cps3230.ferry.domain;

import static mt.edu.um.cps3230.ferry.domain.Fixtures.SAILING_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PassengerTest {

    @Test
    void shouldCountTheYearsWhenTheBirthdayFallsOnTheSailingDate() {
        Passenger passenger = born(SAILING_DATE.minusYears(12));

        assertThat(passenger.ageOn(SAILING_DATE)).isEqualTo(12);
    }

    @Test
    void shouldStillBeAYearYoungerWhenTheBirthdayIsTheDayAfterTheSailing() {
        Passenger passenger = born(SAILING_DATE.minusYears(12).plusDays(1));

        assertThat(passenger.ageOn(SAILING_DATE)).isEqualTo(11);
    }

    @ParameterizedTest(name = "a passenger aged {0} is an infant: {1}")
    @CsvSource({"0, true", "2, true", "3, false", "11, false", "30, false"})
    void shouldBeAnInfantWhenUnderThree(int age, boolean infant) {
        assertThat(born(SAILING_DATE.minusYears(age)).isInfantOn(SAILING_DATE)).isEqualTo(infant);
    }

    @ParameterizedTest(name = "a passenger aged {0} is a child: {1}")
    @CsvSource({"2, false", "3, true", "11, true", "12, false", "30, false"})
    void shouldBeAChildWhenBetweenThreeAndElevenInclusive(int age, boolean child) {
        assertThat(born(SAILING_DATE.minusYears(age)).isChildOn(SAILING_DATE)).isEqualTo(child);
    }

    @Test
    void shouldTakeTheAgeFromTheSailingDateWhenTheBirthdayFallsBetweenBookingAndSailing() {
        Passenger passenger = born(SAILING_DATE.minusYears(12));

        assertThat(passenger.isChildOn(SAILING_DATE.minusDays(1))).isTrue();
        assertThat(passenger.isChildOn(SAILING_DATE)).isFalse();
    }

    @Test
    void shouldRefuseWhenThePassengerHasNoName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Passenger("  ", SAILING_DATE.minusYears(30), false))
                .withMessageContaining("needs a name");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Passenger(null, SAILING_DATE.minusYears(30), false))
                .withMessageContaining("needs a name");
    }

    @Test
    void shouldRefuseWhenThePassengerHasNoDateOfBirth() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Passenger("Passenger", null, false))
                .withMessageContaining("date of birth");
    }

    private static Passenger born(LocalDate dateOfBirth) {
        return new Passenger("Passenger", dateOfBirth, false);
    }
}
