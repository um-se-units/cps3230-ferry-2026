# Fliegu Ferries booking service

The running application for CPS3230, Test Engineering and Testable System
Design, University of Malta, 2026.

Fliegu Ferries carries passengers and vehicles between &#x010A;irkewwa and
M&#x0121;arr. The application holds the timetable, prices a crossing, takes
bookings, takes payment, and calls sailings off when the sea is rough. You
will test it at every level. You do not build it.

- Repository: https://github.com/um-se-units/cps3230-ferry-2026
- Practical sheets: [`practicals/`](practicals)
- Installing the tools: [`docs/installation.md`](docs/installation.md)

## What you need

Java 21, Maven 3.9 or later, Git, and Google Chrome. The step by step
instructions for macOS and Windows are in
[`docs/installation.md`](docs/installation.md).

## Get it, build it, run it

```bash
git clone https://github.com/um-se-units/cps3230-ferry-2026
cd cps3230-ferry-2026
mvn verify
```

A green build ends with `BUILD SUCCESS`, writes a coverage report to
`target/site/jacoco/index.html`, and leaves the application in
`target/ferry.jar`.

```bash
java -jar target/ferry.jar
```

Then open <http://localhost:8080>. The database lives in memory and is filled
with a fortnight of sailings when the application starts, so every restart
gives you the same clean timetable. Stop it with `Ctrl+C`.

## The pages

| Page | What it does |
|---|---|
| `index.html` | Search sailings by date and direction |
| `book.html?sailing=ID` | Build a booking, see the price, hold the places, pay |
| `booking.html?ref=REFERENCE` | Look at a booking and call it off |

Every element you need to reach from a test has an `id` and a `data-testid`.

Paying uses a made up card. Any number works except one ending in `0000`,
which is always turned down.

## The API

All of it answers JSON. Times are ISO 8601 in Malta local time. A request
that is refused answers `400` with `{"errors": [...]}`, and an unknown
reference answers `404`.

| Request | What it does |
|---|---|
| `GET /api/sailings?date=YYYY-MM-DD&from=CIRKEWWA` | The sailings that day, with the places left on each |
| `GET /api/sailings/{id}` | One sailing |
| `GET /api/quote?sailingId=ID&passenger=YYYY-MM-DD&vehicle=true` | A price, holding nothing. Repeat `passenger` once per traveller, and add `:resident` to a date for a Gozo resident |
| `POST /api/bookings` | Hold places, answering `201` with the booking and its price |
| `GET /api/bookings/{reference}` | One booking |
| `POST /api/bookings/{reference}/confirm` | Pay for it, with `{"cardNumber": "..."}` |
| `POST /api/bookings/{reference}/cancel` | Call it off |
| `POST /api/admin/sailings/{id}/weather-check` | Read the forecast for that sailing and act on it |

Holding places needs the `id` of a sailing. Ask for the sailings on any day
in the fortnight starting today, with that date in place of `YYYY-MM-DD`:

```bash
curl 'http://localhost:8080/api/sailings?date=YYYY-MM-DD&from=CIRKEWWA'
```

Then put the `id` of one that has not left yet in place of `SAILING_ID`:

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H 'Content-Type: application/json' \
  -d '{"sailingId": "SAILING_ID",
       "contactEmail": "traveller@example.com",
       "vehicleRegistration": "ABC 123",
       "passengers": [{"name": "Anna", "dateOfBirth": "1990-04-05", "gozoResident": false}]}'
```

## The rules it works to

| Fare | |
|---|---|
| Adult | &#x20AC;4.65 |
| Child, three to eleven on the date of the sailing | &#x20AC;1.15 |
| Infant, under three on the date of the sailing | free, and not counted as fare paying |
| Gozo resident, any age | &#x20AC;1.15 |
| Vehicle, one for each booking | &#x20AC;15.70 |
| Ten or more fare paying passengers | 15% off the passenger total, not off the vehicle |

A booking is `RESERVED` when the places are held, `CONFIRMED` once it is paid
for, and `CHECKED_IN` at the terminal. It can be `CANCELLED` by you until two
hours before the sailing leaves, or by the operator when the weather turns. A
reservation that is not paid for gives its places back.

## Running things

| Command | What it does |
|---|---|
| `mvn verify` | Compiles, runs the tests, writes the coverage report, builds the jar |
| `mvn verify -Pintegration` | Also runs the integration tests, the ones named `*IT` |
| `mvn verify -Pcheck -Dweek=NN` | Runs the self check for a week. Always two digits |
| `mvn pitest:mutationCoverage` | Mutation testing, report in `target/pit-reports` |
| `mvn dependency-check:check` | Scans the dependencies for known vulnerabilities |
| `java -jar target/ferry.jar` | Runs the application on port 8080 |

Settings for the application, all optional, given as system properties:
`ferry.port`, `ferry.db.file`, `ferry.notifications`, `ferry.timetable.days`.

```bash
java -Dferry.port=9000 -Dferry.db.file=./ferry-data -jar target/ferry.jar
```

## Each week

Week 1 starts on 2026-09-29. Each week adds to this repository, so bring
yours up to date before you start:

```bash
git pull
```

Your own tests are yours to keep. Put them under `src/test/java` and commit
them. Nothing that arrives later will overwrite them.

The self check for a week, where there is one, downloads a reference suite
version `2026` and runs it against your work:

```bash
mvn verify -Pcheck -Dweek=03
```
