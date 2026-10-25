# Flight Disruption Rebooking — Options Finder

Eurowings Digital coding challenge. Flight **EW 4711 CGN → BER** is cancelled (48 bookings, 120 passengers).
The backend searches three sources **in parallel** (Lufthansa Group flights, other airlines, trains), normalizes them
into **one model** and recommends **one alternative per booking**. A simple Vue UI shows the result.

**Live demo:** https://disruption-frontend.onrender.com (Render free plan: the first visit may take ~1 min to wake up)

```
Vue UI ──GET /api/disruption-options──► Spring Boot
                                          ├─ routes from the bookings (CGN→BER, CGN→DXB, PMI→BER)
                                          ├─ 3 sources × 3 routes in parallel, 3 s timeout each
                                          ├─ normalize → one Transport model
                                          └─ rank per booking → one recommendation
```

## Run

**Local** (Java 17+, Node 18+):
```bash
cd backend && ./mvnw spring-boot:run          # http://localhost:8080
cd frontend && npm install && npm run dev     # http://localhost:5173 (proxies /api to the backend)
```

**Docker:**
```bash
docker compose up --build                     # UI http://localhost:3000, API http://localhost:8080
```

**Tests:**
```bash
cd backend && ./mvnw verify                   # 93 tests + JaCoCo coverage report
cd frontend && npm run coverage               # 45 tests + coverage report
```

CI (GitHub Actions) runs both test suites, builds the Docker images and smoke-tests them. A push to `main` with
`DEPLOY` in the commit message also deploys to Render (`render.yaml`, deploy hooks as repository secrets).

## API

`GET /api/disruption-options` returns:
- `disruption`: the cancelled flight
- `transportOptions`: all options per route (`"CGN#BER": [...]`), ranked
- `recommendations`: one entry per booking reference, with `passengers`, the original `journey` and the recommended
  `transport` (`null` if nothing fits)

Errors are RFC 7807 `application/problem+json` with a stable `code` (503 data unavailable, 500 invalid data).

## Design decisions

- **Parallel, fail-soft search.** 9 `CompletableFuture` calls on a dedicated pool; each has a 3 s timeout and falls
  back to an empty list. A slow or failing source only loses its own options. Response time ≈ the slowest source.
- **One model.** `Transport` interface with one adapter per source (`InternalFlightTransport`, `ExternalFlightTransport`,
  `TrainTransport`). Prices become `BigDecimal` EUR, times `OffsetDateTime`, flight numbers code + number.
- **Matching on the final journey,** not the cancelled segment: Dubai connections get CGN→DXB options, Palma passengers
  PMI→BER options.
- **Ranking.** Only options with enough seats for the whole party (bookings are never split), then:
  1. earliest arrival
  2. lower price, if arrival is equal
  3. source priority, if both are equal: Lufthansa Group → other airlines → trains
- **Competing for seats.** Each booking is recommended independently; nothing is reserved, because nobody has booked
  yet. `suggestedPassengers` and a *high demand* badge show where recommendations exceed seats. Conflicts are resolved
  at booking time.
- **UI.** Disruption header, one row per booking (original journey with segment status, recommendation, change in
  arrival time, route filter) and all options per route. Multi-leg itineraries list every leg. Times are airport-local.

## Assumptions

- External flight times are local to their airport; train times are Europe/Berlin.
- Airports must match exactly: EXT-2 (from DUS) is not offered for CGN.
- Seats are the availability at search time. Prices are per passenger, in EUR, without fees.
- A missed connection (CGN→BER→DXB) is replaced by a complete CGN→DXB alternative.
- Price and source priority are my additions; the brief only names seats and arrival time. Both only break ties,
  so an earlier option always wins.
- The source order (Lufthansa Group → other airlines → trains) is my assumption; the real order is a business decision.
- `suggestedPassengers` only counts this disruption and this request. Other disruptions or channels may target the
  same flights.
- One disruption in the mock data, so the endpoint takes no parameters.

## Deliberately left out (time-box)

No booking flow, no persistence, no authentication, no seat allocation across bookings, minimal UI styling.

## Future improvements

1. **Booking flow.** Confirm with an atomic seat check (`UPDATE … WHERE seats >= n`), idempotency keys, and `409` →
   offer the next option.
2. **Capacity-aware allocation.** Allocate bookings against remaining seats in an explicit priority order (assistance
   needs, tight connections, status, families), with a `reason` when a booking does not get the earliest option.
3. **Price in the decision.** For example, recommend the cheapest option arriving within N minutes of the earliest, or
   add a note such as "TRN-1 arrives 2h 28m later and saves €99.10 per passenger".
4. **Shared demand.** Store recommendations (one row per booking, stable flight key, expiry) so demand is counted
   across disruptions and agents.
5. **API.** `/api/disruptions/{flight}/{date}/options`, per-source status in the response, OpenAPI docs.
6. **Extensibility and resilience.** A `TransportSource` interface so new sources need no orchestration change;
   circuit breakers, retries, metrics and tracing per source.
7. **Delivery.** Images pushed to a registry, a staging environment with manual promotion to production, a Sonar
   quality gate (`sonar-project.properties` is ready), and end-to-end tests.
