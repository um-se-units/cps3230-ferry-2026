# Recorded Open-Meteo answers

Recorded from the real service so that a test of `OpenMeteoWeatherFeed` never
depends on the network. The readings are hourly for 2026-10-06, in Malta local
time, at 36.02 N 14.30 E.

| File | What it stands for |
|---|---|
| `calm-marine.json` | wave height on a calm day, well under every threshold |
| `calm-forecast.json` | wind gusts on the same calm day |
| `storm-marine.json` | wave height in a storm, over the cancelling threshold |
| `storm-forecast.json` | wind gusts in the same storm |
| `server-error.json` | the body the service sends with an HTTP 500 |
| `malformed-body.json` | a truncated answer, to show what the adapter does with one |
