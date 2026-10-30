package mt.edu.um.cps3230.ferry.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import mt.edu.um.cps3230.ferry.adapters.inmemory.InMemoryNotificationGateway;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.domain.BookingStatus;
import mt.edu.um.cps3230.ferry.domain.Passenger;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.ports.NotificationGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The confirmer with its gateway handed in. The first two tests use the
 * hand written spy that ships with the application; the rest use a Mockito
 * mock, which can also be told to fail.
 */
@ExtendWith(MockitoExtension.class)
class LegacyBookingConfirmerTest {

    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 10, 20, 7, 45);
    private static final Sailing SAILING = new Sailing("2026-10-20-0745-CIRKEWWA", Terminal.CIRKEWWA,
            Terminal.MGARR, DEPARTURE, 900, 140);
    private static final String CONTACT = "traveller@example.com";
    private static final Booking RESERVED = Booking.reservation("FF-ABC234", SAILING,
            List.of(new Passenger("Anna", LocalDate.of(1990, 4, 5), false)),
            Optional.empty(), CONTACT, DEPARTURE.minusDays(1));

    private static final RuntimeException MAIL_DOWN =
            new UncheckedIOException(new IOException("the mail server did not answer"));

    @Mock
    private NotificationGateway gateway;

    @Test
    void shouldConfirmTheBookingWhenTheMessageGoesThrough() {
        Booking confirmed = new LegacyBookingConfirmer(new InMemoryNotificationGateway())
                .confirm(RESERVED, "PAY-1");

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.paymentReference()).contains("PAY-1");
    }

    @Test
    void shouldTellTheContactTheReferenceWhenTheBookingIsConfirmed() {
        InMemoryNotificationGateway spy = new InMemoryNotificationGateway();

        new LegacyBookingConfirmer(spy).confirm(RESERVED, "PAY-1");

        assertThat(spy.messagesTo(CONTACT)).singleElement().asString().contains("FF-ABC234");
    }

    @Test
    void shouldSendOnceWhenTheFirstAttemptGetsThrough() {
        new LegacyBookingConfirmer(gateway).confirm(RESERVED, "PAY-1");

        verify(gateway, times(1)).send(anyString(), anyString());
    }

    @Test
    void shouldTryAgainWhenTheFirstAttemptFails() {
        doThrow(MAIL_DOWN).doNothing().when(gateway).send(anyString(), anyString());

        Booking confirmed = new LegacyBookingConfirmer(gateway).confirm(RESERVED, "PAY-1");

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(gateway, times(2)).send(anyString(), anyString());
    }

    @Test
    void shouldConfirmTheBookingWhenOnlyTheThirdAttemptGetsThrough() {
        doThrow(MAIL_DOWN).doThrow(MAIL_DOWN).doNothing().when(gateway).send(anyString(), anyString());

        Booking confirmed = new LegacyBookingConfirmer(gateway).confirm(RESERVED, "PAY-1");

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(gateway, times(3)).send(anyString(), anyString());
    }

    @Test
    void shouldPassTheFailureOnWhenAllThreeAttemptsFail() {
        doThrow(MAIL_DOWN).when(gateway).send(anyString(), anyString());
        LegacyBookingConfirmer confirmer = new LegacyBookingConfirmer(gateway);

        assertThatThrownBy(() -> confirmer.confirm(RESERVED, "PAY-1")).isSameAs(MAIL_DOWN);
        verify(gateway, times(3)).send(anyString(), anyString());
    }

    @Test
    void shouldSendNothingWhenTheBookingCannotBeConfirmed() {
        Booking cancelled = RESERVED.cancelled();
        LegacyBookingConfirmer confirmer = new LegacyBookingConfirmer(gateway);

        assertThatIllegalStateException().isThrownBy(() -> confirmer.confirm(cancelled, "PAY-1"));
        verifyNoInteractions(gateway);
    }
}
