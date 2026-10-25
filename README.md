# Flight Disruption Rebooking — Options Finder

Prototype for the Eurowings Digital Senior Full Stack coding challenge.

Flight **EW 4711 CGN → BER** is cancelled, which affects 48 bookings and 120 passengers.
The app searches three alternative sources **in parallel** (Lufthansa Group flights, other airlines, trains)
and converts their different formats into **one model**. It then picks **one recommended alternative per booking**
and shows the result in a simple web UI.

```
Vue UI  ──GET /api/disruption-options──►  Spring Boot
                                            ├─ derive routes from the bookings (CGN→BER, CGN→DXB, PMI→BER)
                                            ├─ search 3 sources × 3 routes in parallel (3 s timeout each)
                                            ├─ normalize into one Transport model
                                            ├─ rank per booking → one recommendation
                                            └─ JSON response
```

## Repository layout

| Folder | Contents |
|---|---|
| `backend/`  | Spring Boot 3.2 / Java 17 / Maven (wrapper included) |
| `frontend/` | Vue 3 + Vite + TypeScript |

## Setup and run

**Prerequisites:** Java 17+, Node.js 18+. Maven is not needed; the project includes `./mvnw`.

```bash
# 1. Backend → http://localhost:8080
cd backend
./mvnw spring-boot:run

# 2. Frontend → http://localhost:5173  (second terminal)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server forwards `/api` to the backend, so the browser only talks to one
address. To use a backend on another port, start the dev server with `API_URL=http://localhost:8081 npm run dev`.

The API can also be called directly:

```bash
curl http://localhost:8080/api/disruption-options
```

### With Docker

```bash
docker compose up --build
```

UI at http://localhost:3000, API at http://localhost:8080/api/disruption-options. Only Docker is needed. If a port is
taken, change it with `BACKEND_PORT=8082 FRONTEND_PORT=3001 docker compose up --build`.

- **`backend/Dockerfile`:** multi-stage build. Maven packages the jar, and it runs on a JRE-only Alpine image as a
  non-root user. Its health check uses `/actuator/health`, the only actuator endpoint exposed.
- **`frontend/Dockerfile`:** multi-stage build. Node builds the bundle, and nginx serves it and forwards `/api` to
  `BACKEND_URL` (default `http://backend:8080`). The browser sees one origin, as with the Vite dev proxy.
- **`docker-compose.yml`:** starts the frontend once the backend is healthy. Settings from `application.properties`
  can be overridden with environment variables, e.g. `MOCK_TRAINS_DELAYMS=4000` makes the train source miss the
  3 s budget.

### Tests

```bash
cd backend && ./mvnw test               # adapters, normalizers, sources, ranking, orchestration, endpoint (87 tests)
cd frontend && npm test                 # formatting, API client, every component, app states (45 tests)
cd frontend && npm run build            # type-check + production build
```

### Code coverage and Sonar

```bash
cd backend && ./mvnw verify             # JaCoCo → backend/target/site/jacoco/index.html (+ jacoco.xml)
cd frontend && npm run coverage         # V8 → frontend/coverage/index.html (+ lcov.info)
```

`sonar-project.properties` analyses both projects together and imports both coverage reports. The GitHub Actions
workflow (`.github/workflows/ci.yml`) runs the tests and coverage on every push and pull request, then the SonarCloud
scan. A second job builds both Docker images, starts them with `docker compose`, and smoke-tests the API through nginx. To enable the scan, import the repository on [sonarcloud.io](https://sonarcloud.io), check that
`sonar.organization` and `sonar.projectKey` match the values SonarCloud shows, turn off *Automatic Analysis* (it
cannot import coverage), and add the token as the repository secret `SONAR_TOKEN`. Until the secret exists, the workflow
still runs the tests and skips the scan.

### Configuration (`backend/src/main/resources/application.properties`)

| Property | Default | Meaning |
|---|---|---|
| `mock.internal.delay-ms` | 200 | Simulated latency of the internal flight search |
| `mock.external.delay-ms` | 500 | Simulated latency of the external airline search |
| `mock.trains.delay-ms` | 1500 | Simulated latency of the train search |
| `disruption.sources.timeout-ms` | 3000 | Maximum wait per source call; later answers are dropped |

To see the timeout in action, run `./mvnw spring-boot:run -Dspring-boot.run.arguments=--mock.trains.delay-ms=5000`.
The response then arrives after about 3 s, without train options, and a warning is logged.

## API

`GET /api/disruption-options`

```jsonc
{
  "disruption": { "flight": "EW 4711", "origin": "CGN", "destination": "BER", "status": "CANCELLED", "affectedBookings": 48, ... },

  // every option found, grouped by route and ranked best first
  "transportOptions": {
    "CGN#BER": [ { "offerId": "EXT-1", "source": "EXTERNAL_FLIGHT", "operator": "Nimbus Airways",
                   "departure": "2026-07-21T20:40:00+02:00", "arrival": "2026-07-21T21:50:00+02:00",
                   "totalSeats": 24, "suggestedPassengers": 60, "priceEur": 189.00,
                   "legs": [ { "operatorName": "Nimbus Airways", "operatorCode": "NB", "from": "CGN", "to": "BER", "number": "88" } ] }, ... ],
    "CGN#DXB": [ ... ],
    "PMI#BER": [ ... ]
  },

  // one recommendation per booking reference (PNR), in the order of disruption.json
  "recommendations": {
    "AJWFFO": {
      "passengers": 4,
      "journey": { "origin": "PMI", "destination": "BER", "segments": [   // original routing
        { "flight": "EW 9585", "from": "PMI", "to": "CGN", "departure": "...", "arrival": "...", "status": "NOT_DEPARTED" },
        { "flight": "EW 4711", "from": "CGN", "to": "BER", "departure": "...", "arrival": "...", "status": "CANCELLED" } ] },
      "transport": { /* same shape as above */ }
    },
    ...
  }
}
```

## Design decisions

### Parallel search that never blocks
- **Each source is a mock with its own latency**, reading the provided example files: internal 200 ms, external 500 ms, trains 1500 ms.
- **The service derives the affected routes from the bookings** and searches **each source once per route**, as real flight and rail
  search APIs work (one origin and destination per query). That makes 9 calls, all started at once with `CompletableFuture`
  on a dedicated thread pool.
- **Each call has a 3 s timeout and falls back to an empty list.** A slow or failing source only costs its own options, never the
  whole response. The total response time is about the slowest source (≈1.5 s), not the sum of all of them.

### One model for three formats
- `Transport` is the common interface. `InternalFlightTransport`, `ExternalFlightTransport` and `TrainTransport` each convert
  their source format **once, in the constructor**. One normalizer per source walks that source's response structure.
- **Prices:** EUR decimal, cents (`18900`) and a string (`"89.90 EUR"`) all become `BigDecimal` euros.
- **Flight and train numbers:** `"LH 1975"`, `"BX771"` and `"RX 1024"` are split into operator code + number.
- **Times:** all become `OffsetDateTime`, so arrivals in different time zones compare correctly. See the time zone assumption below.

### Ranking: one recommendation per booking
- **Bookings are matched on their final journey** (`journey.origin → journey.destination`), not on the cancelled segment.
  Passengers connecting to Dubai therefore get CGN→DXB options: their BER→DXB flight leaves before any CGN→BER alternative
  arrives. Passengers stranded in Palma get PMI→BER options.
- **Hard rule:** the option's reported seats must cover the whole party. Families are never split.
- **Order:** earliest arrival first. On a tie, Lufthansa Group comes before other airlines, then trains.

### Bookings competing for the same seats
The data is built so that no single option covers a whole route. For example, about 60 CGN→BER passengers would like EXT-1,
which has 24 seats.

**Decision:** each booking is recommended on its own merits, using the seats the source reports. The recommendation
does **not** reserve seats for other bookings, because this is a decision-support tool. Nobody is booked yet, and we
cannot know which passenger or agent will actually accept which option. Pre-allocating seats would hold them for people
who may never book, and turn away people who would.

The competition is made **visible** instead of hidden:
- **`suggestedPassengers`** counts how many passengers each option is recommended to. It is informational only and can
  exceed the seats.
- **The UI marks such options as *high demand*,** so agents can see which options will sell out and choose from the other
  ranked options on that route.

The competition is resolved at **booking time** by the inventory owner, for example with a conditional seat update or a
short seat hold. Whoever is too late receives a conflict and moves to the next option (see *Future improvements*).

**Alternative considered:** a greedy, seat-aware plan that counts each recommendation against the seats, so all 48
recommendations fit together. Done naively (first booking wins), it hands out early seats by an arbitrary rule. Done
properly, with an explicit priority policy and a reason per recommendation, it is the next step I would take (see
*Future improvements*, item 2).

### Error handling
A slow or failing **transport source is not an error**: it is left out and the response is built from the others.
Everything else goes through one `@RestControllerAdvice` (`GlobalExceptionHandler`), which answers with RFC 7807
`application/problem+json` and a stable `code` that clients can react to:

| Situation | Status | `code` |
|---|---|---|
| Disruption data cannot be loaded | 503 | `DISRUPTION_DATA_UNAVAILABLE` |
| Disruption data is invalid (missing journey, no passengers, duplicate `bookingRef`) | 500 | `INVALID_DISRUPTION_DATA` |
| Anything unexpected (logged with stack trace, no internals in the response) | 500 | `INTERNAL_ERROR` |
| Unknown path / wrong HTTP method | 404 / 405 | (Spring default problem) |

The disruption data is validated when loaded, so bad data fails with a clear message instead of a
`NullPointerException` deep in the ranking. The UI shows the problem's `detail` and offers *Try again*.

### UI
- **Kept simple, as the brief asks:**
  - **Disruption header.**
  - **One row per booking** with its recommendation and a route filter.
  - **All options per route** with seats and the recommended-to count.
- **Original journey next to the alternative:** each booking shows its original segments with their status
  (cancelled, will be missed, not departed) and how much later or earlier the alternative arrives than planned.
- **Multi-leg itineraries** show a stops badge (`1 stop · VIE`) and every leg with its number and from → to.
- **Times are shown in each airport's local time.** The UI reads them straight from the ISO string rather than through
  `Date`, which would convert them to the browser's time zone.

## Assumptions
- **External flight times have no time zone.** They are read as the local time of the respective airport
  (`AirportZones`). For example, EXT-3 arrives at 06:05 **Dubai** time, which makes it 5 minutes earlier than INT-5.
  Unknown airports fall back to Europe/Berlin.
- **Train times have no time zone either.** They are read as Europe/Berlin, since all connections are within Germany.
- **Options from nearby airports are not offered.** EXT-2 departs DUS, not CGN. Matching uses the airport codes exactly.
- **Seat numbers are the availability at search time,** not aircraft capacity.
- **A missed onward connection** (CGN→BER→DXB) is replaced by a complete CGN→DXB alternative, not by a new BER→DXB leg.
- **Only one disruption exists in the mock data,** so the endpoint takes no parameters.

## Known limitations
- **No times per leg.** The sources only provide the first departure and the final arrival, so layover durations cannot be shown.
- **Free-text notes are dropped during normalization:** TRN-2's change in Hannover and EXT-4's overnight layover without a
  hotel. TRN-2 therefore shows as direct.
- **The response does not say which sources failed or timed out.** That is only logged.

## Future improvements
1. **Booking / confirmation flow.** Let agents override the recommendation from the ranked list, then book with a
   seat check in a single step (`UPDATE … WHERE seats >= n`) or a short seat hold. Use idempotency keys, and on `409 Conflict`
   offer the next option.
2. **Capacity-aware re-accommodation.** Today 30 CGN→DXB passengers are all recommended EXT-3, which has 9 seats, so 21 of
   them only find out at booking time. Early seats are limited, so some passengers must arrive later. What the logic
   controls is that total delay stays as small as possible, and that the rule for who gets the early seats can be explained.
   This is how airline re-accommodation tools work:
   - **Priority order.** Sort bookings by a policy kept in one place (one `Comparator<Booking>`), e.g. reduced mobility
     and unaccompanied minors, then tight onward connections, then frequent-flyer status and fare class, then families
     and groups, then booking time. The challenge data has none of these fields, so the default would be the order in
     `disruption.json`, standing in for booking time.
   - **Allocate against remaining seats.** In that order, give each booking the earliest-arriving option that still has
     room for the whole party, and subtract its seats. Bookings are never split. Ties go by source priority, as today.
   - **Backfill.** If a family of 4 does not fit the last 3 seats on an early option, those seats go to the next smaller
     party instead of flying empty.
   - **Explain the result.** Add a `reason` when a booking did not get the earliest option, e.g.
     `"EXT-3 (arr 06:05) fully allocated"`. Then the agent can answer *"why not the earlier flight?"*: *"it is full; you are
     on our 06:10 flight, five minutes later, and I can waitlist you for the earlier one."*
   - **Expected result for CGN→DXB:** 9 passengers on EXT-3 (06:05), 20 on INT-5 (06:10), the rest on EXT-4 (12:40) or
     INT-6 (23:20). Every passenger is recommended a seat that exists, and `suggestedPassengers` never exceeds the seats.

   Seats are still not reserved: the plan only makes the recommendations fit the available capacity. Agents and passengers
   can still choose another ranked option, and conflicts are still resolved at booking time (item 1). Live seat updates in
   the UI (SSE/WebSocket) would keep the plan current as bookings come in.
3. **API.** `GET /api/disruptions/{flight}/{date}/options`, since flight numbers repeat daily. Search with a `notBefore` time,
   report per-source status (`sourcesFailed`) in the response, and publish OpenAPI docs.
4. **Extensibility.** A `TransportSource` interface (`search(Route)`), so Spring injects all sources and a new one, such as
   buses, needs no orchestration change.
5. **Resilience and operations.** Per-source circuit breakers, retries with backoff, metrics and tracing for latency and
   timeouts, and short-lived caching of search results.
6. **Model.** Carry leg times, `via` and notes through normalization. Convert the raw DTOs to Java records.
7. **Delivery.** Push versioned images to a registry from CI, deploy automatically to a staging environment, and promote
   to production with a manual approval. Add end-to-end tests (Playwright) against staging.
