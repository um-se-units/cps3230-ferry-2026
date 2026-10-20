# Week 4: seams and test doubles

Everything in `domain` was written to be tested: whatever a class needs, the
clock included, arrives through its constructor, so a test can hand it
something it controls. Not all code is like that. Three classes in `legacy/`
reach out for what they need themselves: the system clock, the network, a mail
server, one random number generator shared by the whole application. A test
cannot control any of those.

This week you introduce a seam into each of the three, a place where a test
can swap a real collaborator for a stand-in, and then test them with stubs,
mocks and spies.

## What arrives this week

| File | What it is |
|---|---|
| `src/main/java/mt/edu/um/cps3230/ferry/legacy/LegacyCancellationPolicy.java` | Decides whether a sailing goes ahead. Reads the time and fetches the forecast itself |
| `src/main/java/mt/edu/um/cps3230/ferry/legacy/LegacyBookingConfirmer.java` | Confirms a paid booking and e-mails the customer through a socket it opens itself |
| `src/main/java/mt/edu/um/cps3230/ferry/legacy/ReferenceNumbers.java` | Hands out booking references from one generator shared by everything |
| `practicals/week-04.md` | This sheet |

Nothing in the web application uses these classes; the terminal kiosks do.
They are the production code you change this week: introduce the seams, keep
what the code does.

```bash
git pull
mvn verify
```

## What you already have

| Stand-in | Kind | Where |
|---|---|---|
| `FixedWeatherFeed` | a stub: always reports the conditions it was given | `adapters/weather` |
| `InMemoryNotificationGateway` | a spy: keeps every message so a test can read them | `adapters/inmemory` |
| `InMemoryBookingRepository`, `FakePaymentGateway` | fakes: working, simplified versions | `adapters/inmemory`, `adapters/payment` |
| `Clock.fixed(...)` | a clock that never moves | `java.time` |
| Mockito | stubs, mocks and spies made in the test: `mock`, `when`, `doThrow`, `verify` | already on the build |

## Start by trying

Before changing anything, try to write one test for
`LegacyCancellationPolicy.decide` as it stands. Note down what stops you. What
should the test expect? What happens without a network? Would the same test
pass tomorrow? Keep the notes: you need them in task 4.

## The seams

Each class keeps its method exactly as it is and gains a constructor through
which its collaborators arrive. The self check builds each class through
these constructors, so use these parameter types, in this order:

| Class | Constructor to add | It takes the place of |
|---|---|---|
| `LegacyCancellationPolicy` | `(java.time.Clock clock, WeatherFeed weather)` | `LocalDateTime.now()` and the `HttpClient` code |
| `LegacyBookingConfirmer` | `(NotificationGateway notifications)` | the socket code |
| `ReferenceNumbers` | `(java.util.Random random)` | the static `Random` and the static set of references handed out |

`WeatherFeed` and `NotificationGateway` are in `mt.edu.um.cps3230.ferry.ports`:
the same ports the rest of the application uses. `OpenMeteoWeatherFeed` already
does what the `HttpClient` code in the policy does.

You may keep a constructor with no parameters that passes the real
collaborators on to the one above, so that code which builds these classes the
old way still works. The self check does not mind either way.

## Tasks

Put your tests in `src/test/java/mt/edu/um/cps3230/ferry/legacy/` and name
each one `should...When...`.

### 1. `LegacyCancellationPolicy`

What it does, which must not change:

- A sailing that has already left, or is leaving at this moment, is refused
  with an `IllegalStateException`.
- A sailing more than 48 hours away sails, and the forecast is not read.
- Otherwise the forecast for the departure decides: waves of at least 2.5 m, or
  gusts of at least 60 km/h, cancel the sailing; waves of at least 1.8 m, or
  gusts of at least 45 km/h, put it at risk; anything calmer sails.

Introduce the seam, then test every rule with a fixed clock and a stub for the
weather:

```java
ZoneId malta = ZoneId.of("Europe/Malta");
Clock clock = Clock.fixed(LocalDateTime.of(2026, 10, 19, 7, 45).atZone(malta).toInstant(), malta);
WeatherFeed weather = mock(WeatherFeed.class);
when(weather.forecastAt(departure)).thenReturn(new Forecast(2.6, 20));
```

Which of your tests has to use `verify` or `verifyNoInteractions` instead of
checking what `decide` returned?

### 2. `LegacyBookingConfirmer`

What it does, which must not change:

- Only a reserved booking can be confirmed. Anything else is refused with an
  `IllegalStateException`, and nothing is sent.
- The contact on the booking is told, in a message that includes the booking
  reference.

Introduce the seam. The socket code does not disappear: move it into a class
of its own that implements `NotificationGateway`, and hand the confirmer a
`NotificationGateway`. Then check with a spy that the right address is told the
right thing. `InMemoryNotificationGateway` is a spy ready made; Mockito's
`verify` is another way.

The operator also wants one thing the old code never did, and you add it test
first: **sending is tried up to three times in all.** When an attempt fails
(the gateway throws), try again. Once an attempt gets through, stop. If the
third attempt fails as well, its exception reaches the caller instead of being
swallowed. Mockito can make a mock fail on the calls you choose:

```java
doThrow(new UncheckedIOException(new IOException("mail server down")))
        .doNothing()
        .when(gateway).send(anyString(), anyString());
```

### 3. `ReferenceNumbers`

What it does, which must not change:

- A reference is `FF-` followed by six characters drawn from the alphabet in
  the class.
- A generator never hands out the same reference twice.

Introduce the seam: the `Random` arrives through the constructor, and the set
of references handed out belongs to each instance instead of to the class.
Keep the way a reference is made from the random numbers as it is.

Then show that:

- two generators given the same seed (`new Random(42)`) hand out the same
  references;
- a `Random` that repeats itself, made with Mockito or as a small subclass of
  `Random`, still cannot make a generator hand out a reference twice;
- two generators do not share what they have handed out;
- a `BookingService` given `new ReferenceNumbers(new Random(7))` gives a
  reservation a reference your test can know in advance.

Careful with a stand-in `Random` that returns the same number for ever: a
generator that keeps drawing until it finds an unused reference will keep
drawing for ever too.

### 4. Compare

Put `LegacyCancellationPolicy` next to `domain/CancellationPolicy`. They make
the same decision. Why does one need a clock and the other not? Which is easier
to test now, and what would you still change in either? Look back at your notes
from the start: which of the problems did each seam solve?

## Self check

```bash
mvn verify -Pcheck -Dweek=04
```

The reference suite builds each class through the constructor in "The seams"
and checks the rules above with Mockito, the three attempts included. Until a
class has its constructor, its part of the suite stops straight away and names
the constructor it is looking for. The three classes are checked separately, so
you can take them one at a time.
