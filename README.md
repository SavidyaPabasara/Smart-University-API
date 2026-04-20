# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W – Client-Server Architectures  
**Stack:** JAX-RS (Jersey 2.41) + Grizzly HTTP Server  
**Base URL:** `http://localhost:8080/api/v1`

---

## API Overview

A RESTful API for managing campus rooms and IoT sensors, built entirely with JAX-RS and an embedded Grizzly server. All data is held in-memory using `ConcurrentHashMap`.

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/` | GET | Discovery – API metadata and links |
| `/api/v1/rooms` | GET | List all rooms |
| `/api/v1/rooms` | POST | Create a room |
| `/api/v1/rooms/{roomId}` | GET | Get one room |
| `/api/v1/rooms/{roomId}` | DELETE | Delete room (blocked if sensors present) |
| `/api/v1/sensors` | GET | List all sensors (supports `?type=` filter) |
| `/api/v1/sensors` | POST | Register a sensor |
| `/api/v1/sensors/{sensorId}` | GET | Get one sensor |
| `/api/v1/sensors/{sensorId}/readings` | GET | Get reading history |
| `/api/v1/sensors/{sensorId}/readings` | POST | Add a reading |

---

## Project Structure

```
uow-campus-api/
├── pom.xml
├── README.md
└── src/main/java/com/uow/
    ├── ServerLauncher.java          ← entry point (press ENTER to stop)
    ├── CampusApplication.java       ← @ApplicationPath, getClasses()
    ├── InMemoryStore.java           ← singleton ConcurrentHashMap store
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── ApiRootResource.java     ← GET /api/v1/
    │   ├── RoomController.java      ← /api/v1/rooms
    │   ├── SensorController.java    ← /api/v1/sensors
    │   └── ReadingController.java   ← /api/v1/sensors/{id}/readings (sub-resource)
    ├── exception/
    │   ├── RoomOccupiedException.java
    │   ├── InvalidReferenceException.java
    │   └── SensorOfflineException.java
    ├── mapper/
    │   ├── RoomOccupiedMapper.java  ← 409
    │   ├── InvalidReferenceMapper.java ← 422
    │   ├── SensorOfflineMapper.java ← 403
    │   └── CatchAllMapper.java      ← 500 global safety net
    └── filter/
        └── ApiLoggingFilter.java    ← logs every request and response
```

---

## Build & Run Instructions

### Prerequisites
- Java 11 or higher
- Apache Maven 3.8+

### Step 1 – Clone the repository
```bash
git clone https://github.com/<your-username>/uow-campus-api.git
cd uow-campus-api
```

### Step 2 – Build
```bash
mvn clean package
```

### Step 3 – Run
```bash
java -jar target/uow-campus-api-1.0.0.jar
```

Or via Maven:
```bash
mvn exec:java
```

### Step 4 – Stop
Press **ENTER** in the console window. The server will shut down gracefully.

### Step 5 – Verify
```bash
curl http://localhost:8080/api/v1/
```

### Running in NetBeans
1. Open the project (File → Open Project → select the folder)
2. Right-click project → **Build with Dependencies**
3. Right-click project → **Properties** → **Run** → set Main Class to `com.uow.ServerLauncher`
4. Press **F6** to run
5. Press **ENTER** in the Output tab to stop

---

## Sample curl Commands

### 1. Discovery
```bash
curl -s http://localhost:8080/api/v1/ | python3 -m json.tool
```

### 2. Create a Room
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}' \
  | python3 -m json.tool
```

### 3. List all Rooms
```bash
curl -s http://localhost:8080/api/v1/rooms | python3 -m json.tool
```

### 4. Register a Sensor
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"LIB-301"}' \
  | python3 -m json.tool
```

### 5. Filter Sensors by Type
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | python3 -m json.tool
```

### 6. Post a Sensor Reading
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.5}' | python3 -m json.tool
```

### 7. Get Reading History
```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings | python3 -m json.tool
```

### 8. Delete Room with Sensors → 409 Conflict
```bash
# First create a sensor in the room, then try to delete the room
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 9. Register Sensor with bad roomId → 422
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"CO2","roomId":"FAKE-999"}' \
  | python3 -m json.tool
```

### 10. Post Reading to MAINTENANCE Sensor → 403
```bash
# First set sensor status to MAINTENANCE, then try to post a reading
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":99.9}' | python3 -m json.tool
```

---

## Report – Answers to Coursework Questions

---

### Part 1.1 – JAX-RS Resource Class Lifecycle

By default JAX-RS follows a **per-request lifecycle**: the runtime creates a fresh instance of each resource class for every incoming HTTP request and discards it once the response is sent. This means instance-level fields on a resource class are request-scoped — any data stored there is invisible to other requests and lost immediately after the request completes.

This has a direct impact on in-memory data management. If room or sensor data were stored as fields on `RoomController`, each request would start with an empty map and all previously created data would be gone. To solve this, `InMemoryStore` is implemented as a **singleton** — one instance shared across the entire application lifetime. Every per-request `RoomController` or `SensorController` instance calls `InMemoryStore.getInstance()` and operates on the same underlying maps.

Thread safety is handled with `ConcurrentHashMap`. Because multiple HTTP requests can arrive simultaneously, concurrent reads and writes to the same map could corrupt it if using plain `HashMap`. `ConcurrentHashMap` partitions its buckets internally so different threads can write to different segments concurrently without blocking each other, preventing race conditions without requiring explicit `synchronized` blocks on every operation.

---

### Part 1.2 – HATEOAS and Hypermedia in RESTful Design

HATEOAS (Hypermedia as the Engine of Application State) is the principle that API responses should embed navigational links so clients can discover available actions dynamically, rather than relying on hardcoded URLs or static documentation.

The discovery endpoint (`GET /api/v1/`) demonstrates this by returning links to `/api/v1/rooms` and `/api/v1/sensors` within the response body. A client can start at this single known URL and navigate the entire API without prior knowledge of any other paths.

Benefits over static documentation:
- **Reduced coupling:** If the server renames `/api/v1/rooms` to `/api/v2/rooms`, clients following embedded links adapt automatically. Clients with hardcoded URLs would break.
- **Self-describing API:** A developer can explore the API by following links, similar to browsing a website, rather than consulting a separate document.
- **State-aware navigation:** Responses can include only the links that are valid in the current state. A room with active sensors would omit a delete link, preventing invalid calls before they are made.
- **Evolvability:** New resources can be added and surfaced through the discovery endpoint without breaking existing clients.

---

### Part 2.1 – Returning IDs vs Full Objects in Collections

**Returning only IDs** produces very small responses, which is useful for extremely large collections. However, it forces the client to make one additional HTTP request per ID to retrieve the actual room data — the N+1 request problem. Under real traffic this multiplies server load and increases total latency significantly.

**Returning full objects** increases response size per call, but delivers everything the client needs in a single round trip. For a facilities management dashboard that must display room names, capacities, and sensor lists, this is clearly superior. The response can also be cached on the client or at an intermediary proxy, avoiding repeated calls entirely.

In this implementation full room objects are returned. For very large deployments, pagination (`?page=1&size=20`) would control payload size without sacrificing data completeness.

---

### Part 2.2 – Is DELETE Idempotent?

**Yes**, DELETE is idempotent as defined by RFC 7231. Idempotency means that making the same request multiple times produces the same final **server state**, regardless of how many times it is called.

In this implementation:
- The **first DELETE** on an existing room with no sensors removes the room and returns `204 No Content`.
- The **second DELETE** on the same room ID finds nothing and returns `404 Not Found`.

The server state is identical after both calls — the room does not exist. The differing HTTP status codes (`204` vs `404`) do not violate idempotency, which concerns state rather than response codes. This is consistent with the HTTP specification, which explicitly notes that a 404 on a repeated DELETE is an acceptable outcome.

---

### Part 3.1 – Effect of @Consumes(APPLICATION_JSON) on Wrong Content-Type

`@Consumes(MediaType.APPLICATION_JSON)` declares that the method only accepts requests whose `Content-Type` header is `application/json`. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, the JAX-RS runtime performs content negotiation during request matching — before the method body is ever executed — and finds no matching method. It immediately returns **HTTP 415 Unsupported Media Type** without invoking any application code. This acts as an automatic input validation gate, protecting business logic from receiving unintended payload formats.

---

### Part 3.2 – @QueryParam vs Path Segment for Filtering

Using `GET /api/v1/sensors?type=CO2` (query parameter) is superior to `GET /api/v1/sensors/type/CO2` (path segment) for the following reasons:

1. **REST semantics:** A URI path should identify a resource. `sensors/CO2` implies `CO2` is itself a sub-resource, which it is not — it is a filter criterion.
2. **Optionality:** `GET /api/v1/sensors` (no filter) and `GET /api/v1/sensors?type=CO2` (filtered) both work with a single route definition. A path approach needs a second separate route to handle the unfiltered case.
3. **Composability:** Multiple query parameters combine naturally — `?type=CO2&status=ACTIVE`. Adding a second filter to a path approach creates combinatorial routing complexity.
4. **Cacheability:** The path `/api/v1/sensors` is a stable, cacheable resource identifier. Query parameters are understood by caches and proxies as modifiers, allowing the base URL to be cached independently of its filtered variants.

---

### Part 4.1 – Sub-Resource Locator Pattern Benefits

The sub-resource locator pattern delegates part of a URL hierarchy to a separate class. `SensorController` has no `@GET`/`@POST` on the `/readings` path — instead, it returns a `ReadingController` instance, and JAX-RS inspects that class to finish routing.

Benefits:

1. **Single Responsibility:** `SensorController` handles sensor CRUD. `ReadingController` handles reading history. Each class has one reason to change.
2. **Complexity management:** In a large API, placing all nested handlers in one class results in hundreds of lines of mixed concerns. Separate classes stay short and focused.
3. **Context injection:** The locator passes `sensorId` to `ReadingController`'s constructor, giving the sub-resource the context it needs without repeating path parameter extraction in every method.
4. **Testability:** `ReadingController` can be unit tested by instantiating it directly with a known sensor ID, without needing the full JAX-RS routing layer.
5. **Reusability:** Any other resource could reuse `ReadingController` via its own locator without duplicating code.

---

### Part 5.2 – Why 422 is More Accurate Than 404

When a client POSTs a new sensor with a `roomId` that does not exist, `404 Not Found` would be misleading because:

- **404 means the URL endpoint was not found.** But `POST /api/v1/sensors` exists and is working correctly.
- **422 Unprocessable Entity** (RFC 4918) means: "The server understands the content type, the syntax is correct, but the semantic instructions cannot be followed." This is exactly the situation — valid JSON, valid endpoint, but a field value inside the payload references a resource that does not exist.
- **Client guidance:** A 404 tells the client to check its URL. A 422 tells the client to fix the data it is sending — specifically the `roomId` field. This gives accurate, actionable feedback.
- **Analogy:** A form submission referencing a non-existent country code is not a missing page — it is a semantic validation failure on a field value. 422 models this precisely.

---

### Part 5.4 – Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to API consumers is a serious security vulnerability:

1. **Technology fingerprinting:** Stack traces reveal exact library names and versions (e.g., `org.glassfish.jersey 2.41`). Attackers cross-reference these with public CVE databases to find known, unpatched vulnerabilities targeting those exact versions.
2. **Internal architecture exposure:** Package names and class names (e.g., `com.uow.resource.ReadingController`) reveal the application's internal design, helping attackers map out the codebase before crafting targeted exploits.
3. **Code path disclosure:** Line numbers pinpoint exactly where an error occurred. Combined with source code obtained through other means, attackers can identify the precise lines to target with malformed input.
4. **Infrastructure leaks:** Database-related stack traces can reveal driver class names, connection strings, or schema names that expose backend infrastructure.
5. **Enumeration assistance:** Deliberately triggering errors and reading stack traces lets attackers enumerate all resource paths, parameter names, and validation boundaries — effectively reverse-engineering the API without documentation.

`CatchAllMapper` addresses this by logging the full stack trace server-side only (accessible to authorised engineers via log management tools) and returning a generic, opaque message to the client.

---

### Part 5.5 – Why Filters are Better than Manual Logging

Using `ApiLoggingFilter` to log all requests and responses is superior to placing `Logger.info()` calls inside every resource method for these reasons:

1. **DRY (Don't Repeat Yourself):** One filter covers all endpoints. Updating the log format requires changing one class, not dozens of resource methods.
2. **Guaranteed coverage:** A developer adding a new endpoint cannot accidentally omit logging — the filter intercepts all requests unconditionally.
3. **Separation of concerns:** Resource methods contain business logic only. Logging, authentication, CORS headers, and rate limiting are cross-cutting concerns that belong in the filter chain.
4. **Consistent format:** All log entries follow the same template, making log aggregation tools and alerting rules reliable.
5. **Lifecycle accuracy:** `ContainerRequestFilter` fires before the resource method; `ContainerResponseFilter` fires after the response is finalised. Both the request context and the final HTTP status code are available simultaneously only at this level — resource methods cannot see the final response code without try/finally boilerplate around every return statement.
