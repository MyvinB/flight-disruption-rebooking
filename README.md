# Flight Disruption Rebooking — Options Finder

Prototype for the Eurowings Digital Senior Full Stack coding challenge.

Flight **EW 4711 CGN → BER** is cancelled, which affects 48 bookings and 120 passengers.
The app searches three alternative sources **in parallel** (Lufthansa Group flights, other airlines, trains)
and converts their different formats into **one model**. It then picks **one recommended alternative per booking**
and shows the result in a simple web UI.

**Live demo:** https://disruption-frontend.onrender.com
(API: https://disruption-backend.onrender.com/api/disruption-options). It runs on Render's free plan, so the first
visit after a quiet period takes about a minute while the services wake up.

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

### Deployment (Render, free plan)

Both services run on [Render](https://render.com) from their Dockerfiles, defined in `render.yaml`:

| Service | URL |
|---|---|
| Frontend (UI) | https://disruption-frontend.onrender.com |
| Backend (API) | https://disruption-backend.onrender.com/api/disruption-options |
| Backend health | https://disruption-backend.onrender.com/actuator/health |

```
Browser → disruption-frontend.onrender.com  (nginx: UI)
            └── /api/* → disruption-backend.onrender.com  (Spring Boot)
```

The frontend's nginx forwards `/api` to the backend's public URL (`BACKEND_URL`), so the browser talks to one origin.
Both containers listen on the `PORT` that Render sets.

**Deploys come from CI, and only when asked for.** The `deploy` job in `.github/workflows/ci.yml` runs when a push to
`main` has `DEPLOY` in its latest commit message, and only after the tests and the Docker smoke test pass. It calls
each service's Render deploy hook. Every other push is tested but not deployed.

```bash
git commit -m "Show original journey DEPLOY"         # tested and deployed
git commit --allow-empty -m "DEPLOY"                 # redeploy without changes
```

One-time setup:
1. In Render: **New → Blueprint**, select this repository. Render reads `render.yaml` and creates both services.
2. When asked for `BACKEND_URL`, enter the **backend's** URL, here `https://disruption-backend.onrender.com`. Render
   shows the exact URL on the backend's page. If `BACKEND_URL` points to the frontend instead, `/api` calls loop back to
   nginx and fail with `508 Loop Detected`. A missing `https://` or a trailing slash is corrected at startup, and the
   frontend logs `Forwarding /api to …` so the value in use is easy to check.
3. On each service, copy **Settings → Deploy Hook**. In GitHub, add the two hooks under **Settings → Secrets and
   variables → Actions** as `RENDER_DEPLOY_HOOK_BACKEND` and `RENDER_DEPLOY_HOOK_FRONTEND`.

Until both secrets exist, the deploy job is skipped and CI still passes.

**Free plan:** services sleep after about 15 minutes without traffic. The first request afterwards takes about a minute
while they wake up. If the UI shows an error then, *Try again* works once the backend is up. Open the site a minute
before a demo.

### Tests

```bash
cd backend && ./mvnw test               # adapters, normalizers, sources, ranking, orchestration, endpoint (93 tests)
cd frontend && npm test                 # formatting, API client, every component, app states (45 tests)
cd frontend && npm run build            # type-check + production build
```

### Code coverage

```bash
cd backend && ./mvnw verify             # JaCoCo → backend/target/site/jacoco/index.html (+ jacoco.xml)
cd frontend && npm run coverage         # V8 → frontend/coverage/index.html (+ lcov.info)
```

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs both test suites with coverage on every push and pull
request, and attaches the HTML reports to the run as the `coverage-reports` artifact. A second job builds both Docker
images, starts them with `docker compose`, and smoke-tests the API through nginx.

Sonar analysis is not part of the pipeline yet. `sonar-project.properties` is already set up to analyse both projects
and import both coverage reports, so adding a SonarCloud or SonarQube scan step to the workflow is all that is missing.

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
- **Order** among the options that fit:
  1. **Earliest arrival.** The brief asks for arrival time, and for a stranded passenger reaching the destination is
     what matters most. Arrivals are compared as instants, so 06:05 in Dubai (+04:00) correctly comes before 05:00 in
     Berlin (+02:00).
  2. **Lower price**, when two options arrive at the same time. The airline pays for the rebooking, so of two equally
     fast options the cheaper one is better.
  3. **Source priority**, when arrival and price are both equal: Lufthansa Group first, then other airlines, then
     trains. Own-group flights are easier to rebook onto and keep the passenger with the airline.
- **One option per booking.** Price never outweighs time in the recommendation: an earlier option wins, however much
  more it costs. Weighing the two is listed under *Future improvements*.

Example, CGN → DXB (the order of `transportOptions['CGN#DXB']`):

| # | Option | Arrives (Dubai time) | Price | Seats |
|---|---|---|---|---|
| 1 | EXT-3 Meridian Air via VIE | 22 Jul 06:05 | €424 | 9 |
| 2 | INT-5 Lufthansa via FRA | 22 Jul 06:10 | €486 | 20 |
| 3 | EXT-4 Zephyr Jet via ATH | 22 Jul 12:40 | €288 | 26 |
| 4 | INT-7 Lufthansa | 22 Jul 22:05 | €512 | 9 |
| 5 | INT-6 Lufthansa | 22 Jul 23:20 | €421 | 33 |

A party of up to 9 gets EXT-3. A party of 10 or more does not fit EXT-3 and gets INT-5.

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
- **Arrival time comes before price.** The brief names arrival time and seats; price is my addition and only decides
  between options arriving at the same time. A cheaper but later option is never recommended over an earlier one.
- **Prices are per passenger, all in EUR, and comparable as given.** Taxes, fees, baggage rules and fare conditions are
  not modelled, and the total for a booking is price × passengers.
- **Transport priority is my assumption, not from the brief.** The brief does not rank the sources. I assumed this
  order (`TransportMode`):
  1. Lufthansa Group flights (`INTERNAL_FLIGHT`)
  2. Other airlines (`EXTERNAL_FLIGHT`)
  3. Trains (`TRAIN`)

  The reasoning is that rebooking onto the group's own flights is usually cheaper and easier for the airline, and
  keeps the passenger with it. A train is a different mode of travel, so it comes last. The priority is only the
  **last** criterion: it decides only when two options arrive at the same time for the same price, so a cheaper train
  arriving at the same time still beats a Lufthansa flight. The real order would be a business decision.
- **`suggestedPassengers` only counts this disruption, for this request.** It is computed fresh each time and not
  stored. In reality other cancelled flights, other agents or self-service rebooking may point passengers to the same
  alternative, so the real demand can be higher than shown. In production the recommendations would be kept in a
  shared store (database or Redis):
  - one row per booking, updated rather than added on every new search, so repeated searches never double-count
  - a stable flight key (flight number + date + cabin) instead of the per-search offer id
  - an expiry, and removal once the booking is made or declined
  - a count of the active rows per flight across all disruptions

  It would still be a hint; the seats themselves are checked at booking time.

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
     room for the whole party, and subtract its seats. Bookings are never split. Ties go by price, as today.
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
3. **Price in the decision, not just as a tie-break.** Today an earlier option always wins, even when it costs far more.
   Two ways to let price count while still showing one option per booking:
   - **Arrival window:** options arriving within N minutes (e.g. 60) of the earliest one count as equally good, and the
     cheapest of them is recommended. Easy for an agent to explain: *"it lands within an hour of the earliest and is the
     cheapest of those."* With this data, 60 minutes changes nothing; 3 hours would move the 24 Cologne → Berlin
     bookings from EXT-1 (21:50, €189) to the train TRN-1 (00:18, €89.90).
   - **Cheaper option as a note:** keep the earliest option as the recommendation and add one line of text, e.g.
     *"TRN-1 arrives 2h 28m later and saves €99.10 per passenger"*, so the agent can offer it if the passenger does not
     mind waiting. For Dubai that would be EXT-4 (6h 35m later, €136 less); for Palma there is none, since INT-8 is
     already the cheapest.

   The acceptable extra wait is a business decision. It could depend on route length, or on the EU261 delay thresholds
   (e.g. 3 hours), above which compensation is due anyway.
4. **API.** `GET /api/disruptions/{flight}/{date}/options`, since flight numbers repeat daily. Search with a `notBefore` time,
   report per-source status (`sourcesFailed`) in the response, and publish OpenAPI docs.
5. **Extensibility.** A `TransportSource` interface (`search(Route)`), so Spring injects all sources and a new one, such as
   buses, needs no orchestration change.
6. **Resilience and operations.** Per-source circuit breakers, retries with backoff, metrics and tracing for latency and
   timeouts, and short-lived caching of search results.
7. **Model.** Carry leg times, `via` and notes through normalization. Convert the raw DTOs to Java records.
8. **Delivery.** Push versioned images to a registry from CI, deploy automatically to a staging environment, and promote
   to production with a manual approval. Add end-to-end tests (Playwright) against staging.
