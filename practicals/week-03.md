# Week 3: choosing test cases for the fare rules

Last week the tests came one at a time, each driving the next piece of code.
This week the code already exists and the question is different: out of all
the bookings anyone could make, which few are worth a test? You will answer it
four ways, with equivalence partitions, boundary values, pairwise combinations
and properties, all aimed at `PriceCalculator`.

## What arrives this week

| File | What it is |
|---|---|
| `practicals/week-03.md` | This sheet, including the rules below, which are the specification you test against |
| `practicals/models/fares.pict` | A pairwise model of the fare rules, for PICT |

Bring your repository up to date first:

```bash
git pull
mvn verify
```

## The rules

`PriceCalculator.priceFor(Booking)` gives the price of a whole booking in euro
cents, as a `Money`. `priceFor(LocalDate, List<Passenger>, boolean)` gives the
same price for the date of a sailing, the people travelling and whether a
vehicle comes too, without a `Booking` having to exist. Both follow these rules.

**Age.** A passenger's age is the number of whole years between their date of
birth and the date of the sailing: not the date the booking was made, and not
today. A passenger whose birthday falls on the sailing date has already had it.

**Passengers.** Each passenger is exactly one of these, checked in this order:

| Passenger | Fare | Fare paying |
|---|---|---|
| Infant: under 3 on the sailing date, whether or not a Gozo resident | free | no |
| Gozo resident, 3 or over | &#x20AC;1.15 | yes |
| Child: 3 to 11 inclusive on the sailing date | &#x20AC;1.15 | yes |
| Anyone else | &#x20AC;4.65 | yes |

**Group rate.** When a booking has 10 or more fare paying passengers, 15% comes
off the passenger total. Infants do not count towards the ten. The discount is
rounded half up to the nearest cent: ten adults come to &#x20AC;46.50, less 15% is
&#x20AC;39.525, which is charged as &#x20AC;39.53.

**Vehicle.** A booking carries at most one vehicle, at &#x20AC;15.70. It is added
after any group rate, which never comes off the vehicle.

**Who can book.** A passenger needs a name that is not blank and a date of
birth. A booking request needs at least one passenger and a contact address
containing `@`. A vehicle needs a registration that is not blank. `Passenger`,
`Vehicle` and `BookingRequest` refuse anything else with an
`IllegalArgumentException` (or a `NullPointerException` for a missing value).

Work out every expected price in your tests from these rules, by hand. A test
whose expected value came from running the code checks only that the code does
what it does.

## Where your tests go

Put them in `src/test/java/mt/edu/um/cps3230/ferry/domain/`, in as many classes
as you like. The class `Fixtures` there already builds sailings, passengers and
bookings; add to it or write your own helpers. Name each test `should...When...`.

## Tasks

### 1. Partitions

For each input that changes the price (the age of each passenger, residency,
the number of fare paying passengers, the vehicle), write down its equivalence
partitions and the boundaries between them. A short table in a comment at the
top of your test class is enough. Then write one test per partition.

### 2. Boundaries

Test either side of every boundary you found. Use a parameterised test
(`@ParameterizedTest` with `@CsvSource`) so that each boundary is one row, not
one method. Two kinds of boundary are easy to miss:

- ages are whole years on a date, so the day before a birthday is a boundary
  as much as the birthday itself;
- the group rate counts fare paying passengers, not people.

### 3. Pairwise combinations

Faults often live where two rules meet: a rule that is right on its own and
wrong in combination with another. Testing every combination grows quickly,
so test every *pair* of values instead. PICT, a free tool from Microsoft,
generates such a set from `practicals/models/fares.pict`:

```bash
pict practicals/models/fares.pict
```

Each line of its output is one test case. Turn each into a row of a
parameterised test, with the expected price worked out from the rules. Then
compare the number of rows with the number of all possible combinations, and
look at the constraint at the bottom of the model: what would happen without
it?

Getting PICT: on Windows, `pict.exe` is on
<https://github.com/microsoft/pict/releases>; put it in a folder on your
`PATH`. On macOS and Linux, build it from the source with CMake (on macOS,
`brew install cmake` first):

```bash
git clone https://github.com/microsoft/pict.git
cd pict
cmake -DCMAKE_BUILD_TYPE=Release -S . -B build
cmake --build build
find build -name pict -type f
```

The last line shows where the program was built.

### 4. Properties

jqwik is already on the build. A property states something that must hold
for every input, and jqwik tries it on many generated ones:

```java
@Property
void shouldAddTheSameAmountWhenAVehicleIsAdded(@ForAll @IntRange(min = 0, max = 90) int age) {
    ...
}
```

Write at least two properties of the fare rules. Some that hold: a vehicle
always adds exactly &#x20AC;15.70; adding an infant never changes the price; the
order in which passengers are listed never changes the price. When a property
fails, jqwik prints the smallest input it found that breaks it.

### 5. Who can book

Apply partitions and boundaries to the rules under "Who can book": what makes
a `Passenger`, a `Vehicle` or a `BookingRequest` valid, and what is refused?

## Self check

```bash
mvn verify -Pcheck -Dweek=03
```

The self check does not test `PriceCalculator`. It tests your tests. It holds
six copies of `PriceCalculator`, each with one fault planted in it, and runs
every test class in your `domain` test package against each copy in turn. For
each copy, at least one of your tests has to fail; a copy that gets through is
reported with the rule it gets wrong, though not with the fault itself.

Before any of that, your tests have to pass against the real
`PriceCalculator`. A test that fails there fails against every copy too, and
catches nothing.
