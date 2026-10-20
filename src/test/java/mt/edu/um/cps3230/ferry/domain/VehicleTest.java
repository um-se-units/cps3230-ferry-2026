package mt.edu.um.cps3230.ferry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class VehicleTest {

    @Test
    void shouldTidyTheRegistrationWhenTheVehicleIsBuilt() {
        assertThat(new Vehicle("  abc 123 ").registration()).isEqualTo("ABC 123");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRefuseWhenTheVehicleHasNoRegistration(String registration) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Vehicle(registration))
                .withMessageContaining("needs a registration");
    }
}
