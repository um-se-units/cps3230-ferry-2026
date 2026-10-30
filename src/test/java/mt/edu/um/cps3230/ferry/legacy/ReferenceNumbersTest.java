package mt.edu.um.cps3230.ferry.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * The generator with its randomness handed in: seeded where the test wants
 * the same references twice, scripted where it wants to force a repeat.
 */
class ReferenceNumbersTest {

    private static final String FORMAT = "FF-[A-HJ-NP-Z2-9]{6}";

    @Test
    void shouldHandOutAReferenceInTheTicketFormatWhenAskedForOne() {
        assertThat(new ReferenceNumbers(new Random(1)).next()).matches(FORMAT);
    }

    @Test
    void shouldHandOutTheSameReferencesWhenTwoGeneratorsShareASeed() {
        ReferenceNumbers first = new ReferenceNumbers(new Random(42));
        ReferenceNumbers second = new ReferenceNumbers(new Random(42));

        IntStream.range(0, 5).forEach(count -> assertThat(first.next()).isEqualTo(second.next()));
    }

    @Test
    void shouldNotRepeatAReferenceWhenTheRandomNumbersRepeat() {
        ReferenceNumbers references = new ReferenceNumbers(scriptedToRepeatOnce());

        String first = references.next();
        String second = references.next();

        assertThat(second).isNotEqualTo(first).matches(FORMAT);
    }

    @Test
    void shouldKeepWhatEachGeneratorHandedOutToItselfWhenTwoAreInUse() {
        ReferenceNumbers kiosk = new ReferenceNumbers(scriptedToRepeatOnce());
        ReferenceNumbers website = new ReferenceNumbers(scriptedToRepeatOnce());

        assertThat(website.next()).isEqualTo(kiosk.next());
    }

    @Test
    void shouldCountOnlyItsOwnReferencesWhenAskedHowManyItHandedOut() {
        ReferenceNumbers references = new ReferenceNumbers(new Random(3));
        new ReferenceNumbers(new Random(4)).next();

        references.next();
        references.next();

        assertThat(references.issuedCount()).isEqualTo(2);
    }

    /**
     * Draws the same six characters twice, then carries on like a seeded
     * generator, so that a generator which still remembers too much keeps
     * drawing until it finds something new instead of looping for ever.
     */
    private static Random scriptedToRepeatOnce() {
        Random afterwards = new Random(1);
        Random random = mock(Random.class);
        when(random.nextInt(anyInt()))
                .thenReturn(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                .thenAnswer(call -> afterwards.nextInt(call.<Integer>getArgument(0)));
        return random;
    }
}
