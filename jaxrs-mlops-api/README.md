# MLOps Pipeline Management API

This is my project submission for the 5COSC022W Client-Server Architectures coursework. The task was to build a REST API to manage Machine Learning Workspaces and Models, plus a history of their evaluation metrics. 

Since the rules said no Spring Boot or database engines (like SQL Server or Postgres), I wrote the backend using Java 21, core JAX-RS (Jersey 2.x), and Grizzly to run the server. Everything is stored in memory.

---

## Technical Architecture & Design Overview

I went with a simple layered structure. The main challenge during development was keeping data safe because we aren't using a real database. I decided to store everything in memory using thread-safe collection classes. This way, if concurrent API requests hit the server, the data doesn't get messed up.

```
                  +----------------------------------------------+
                  |                 HTTP Client                  |
                  +----------------------+-----------------------+
                                         |
                                         | HTTP Requests / Responses
                                         v
                  +----------------------+-----------------------+
                  |            Jersey Container (Grizzly)         |
                  |  +----------------------------------------+  |
                  |  |             LoggingFilter              |  |
                  |  +-------------------+--------------------+  |
                  |                      |                       |
                  |                      v                       |
                  |  +-------------------+--------------------+  |
                  |  |           ExceptionMappers             |  |
                  |  +-------------------+--------------------+  |
                  |                      |                       |
                  |                      v                       |
                  |  +-------------------+--------------------+  |
                  |  |             JAX-RS Resources           |  |
                  |  +-------------------+--------------------+  |
                  +----------------------|-----------------------+
                                         |
                                         v
                  +----------------------+-----------------------+
                  |         InMemoryDatabase (Thread-Safe)       |
                  |  - ConcurrentHashMap (Workspaces & Models)   |
                  |  - CopyOnWriteArrayList (Metrics history)    |
                  +----------------------------------------------+
```

### Key parts of the code:
- **API Entry & Versioning**: Inside `MLOpsApplication.java`, I used the `@ApplicationPath("/api/v1")` annotation. This sets up the base path for all the endpoints to be versioned.
- **In-Memory Store**: I wrote an `InMemoryDatabase.java` file. It uses `ConcurrentHashMap` for workspaces and models, and `CopyOnWriteArrayList` for the evaluation metrics history. I chose these thread-safe collections because regular Maps and Lists can throw errors if multiple requests edit them at once.
- **Logging Filter**: I made a custom `LoggingFilter.java` class implementing `ContainerRequestFilter` and `ContainerResponseFilter`. It prints out basic things like HTTP Method, URI, and Response Code to the console. This was super helpful for debugging when I was testing endpoints.
- **Exception Mappers**: To make the API robust (so clients don't see ugly Java stack traces when they make a bad request), I mapped exceptions to custom JSON error bodies:
  - **409 Conflict**: Thrown by `WorkspaceNotEmptyException` if you try to delete a workspace that still has models in it.
  - **422 Unprocessable Entity**: Thrown by `LinkedWorkspaceNotFoundException` if you POST a model with a `workspaceId` that does not exist.
  - **403 Forbidden**: Thrown by `ModelDeprecatedException` if you try to add evaluation metrics to a model that is marked as DEPRECATED.
  - **500 Internal Server Error**: I wrote a fallback `GlobalExceptionMapper` for generic exceptions (like NullPointerException) so the API doesn't leak internal code details if something crashes.

---

## Setup, Build, and Execution Instructions

### Prerequisites
- **Java JDK 21** or later
- **Apache Maven**

### Building the Project
Open a terminal in the project root folder. To compile everything, run:

```bash
mvn clean compile
```

To build the executable JAR:

```bash
mvn package
```

### Running the Server
You can start up Grizzly by running:

```bash
mvn exec:java
```

The server runs on port `8080`.
- **Discovery Endpoint**: `http://localhost:8080/api/v1`
- **WADL Schema**: `http://localhost:8080/api/v1/application.wadl`

To stop the server, just press **Enter** in the terminal window.

---

## Sample HTTP Interactions (curl commands)

I tested all these endpoints using Postman (which you can see in the demo video), but you can also run them using these curl commands:

### 1. Discovery Endpoint (GET)

```bash
curl -X GET http://localhost:8080/api/v1 -H "Accept: application/json"
```

### 2. Create a New Workspace (POST)

```bash
curl -X POST http://localhost:8080/api/v1/workspaces \
  -H "Content-Type: application/json" \
  -d "{\"id\": \"WS-VISION-01\", \"teamName\": \"Computer Vision Lab\", \"storageQuotaGb\": 150}"
```

### 3. Get Workspace Metadata (GET)

```bash
curl -X GET http://localhost:8080/api/v1/workspaces/WS-VISION-01 -H "Accept: application/json"
```

### 4. Create a Model (POST)
Note: The server auto-generates the model ID, so you don't pass one in the JSON.

```bash
curl -X POST http://localhost:8080/api/v1/models \
  -H "Content-Type: application/json" \
  -d "{\"framework\": \"PyTorch\", \"status\": \"TRAINING\", \"latestAccuracy\": 0.0, \"workspaceId\": \"WS-VISION-01\"}"
```

### 5. Get Models with status filter (GET)

```bash
curl -X GET http://localhost:8080/api/v1/models?status=TRAINING -H "Accept: application/json"
```

### 6. Append Metric (Deep Nesting POST)
Make sure to replace `MOD-XXXX` with the model ID the server generated in step 4.

```bash
curl -X POST http://localhost:8080/api/v1/models/MOD-XXXX/metrics \
  -H "Content-Type: application/json" \
  -d "{\"accuracyScore\": 0.948}"
```

### 7. Trigger Conflict Error (DELETE)
If you try to delete the workspace while the model is still in it, it will fail and return a 409 error:

```bash
curl -X DELETE http://localhost:8080/api/v1/workspaces/WS-VISION-01
```

---

## Conceptual Report & Coursework Question Answers

### Part 1: Service Architecture & Setup

#### Q1.1: When returning a Java object from a method, it is automatically serialised into JSON. Explain the role of a `MessageBodyWriter` or a JSON provider (like Jackson) in this conversion process.

At first, I found it a bit confusing how a Java class (like `MLWorkspace`) turns into a JSON string when returned by a JAX-RS method. After some reading, I found out JAX-RS delegates this to a provider implementing the `MessageBodyWriter` interface. 

In my case, I registered the Jackson provider in the `pom.xml` file. When an API call returns a Java object:
- The framework looks at the `@Produces` annotation (which tells it to output JSON) and checks the object's class type.
- It finds a writer that supports this class. Jackson's `MessageBodyWriter` says it can write it, so it takes over.
- Jackson's `ObjectMapper` looks at the class properties (using fields, getters, and setters) and converts them into a formatted JSON string.
- Finally, it writes this string as bytes directly to the HTTP response stream.

If we don't have a JSON provider registered, JAX-RS throws a `MessageBodyWriterNotFoundException` because it doesn't know how to convert custom Java objects.

#### Q1.2: REST architecture dictates that APIs should be strictly 'stateless'. Define what statelessness means in this context and explain why it makes cloud APIs easier to scale horizontally across multiple servers.

Statelessness basically means the server doesn't save any info about client sessions or history. Every single request from the client has to be completely self-contained. The client has to send all the metadata, request data, and authentication details every time.

This makes horizontal scaling (running the app on multiple servers behind a load balancer) much easier:
- **No sticky sessions**: The load balancer can route any request to any server that is free. We don't have to keep sending the same user to the same server they logged into.
- **Easy replication**: We can spin up new server instances or shut down old ones based on CPU load. Since new servers don't need to sync session data, they can start handling requests immediately.
- **No session sync complexity**: Stateful systems need clustered databases or caches like Redis to share session state across all servers. Stateless APIs completely avoid this database overhead.

---

### Part 2: Workspace Management

#### Q2.1: Discuss how implementing HTTP `Cache-Control` headers on the `GET /workspaces` endpoint could improve performance for the client and reduce unnecessary processing load on the server.

The `Cache-Control` header is basically a way for the server to tell the browser (or CDN) that it can keep a copy of the data and reuse it.

In my project, I added `Cache-Control: public, max-age=60` to the workspaces list.
For the client, this is great because if they request the list again within 60 seconds, it loads instantly from their local cache. They don't have to wait for a network request to complete.
For the server, it is a huge relief. If clients are loading cached data, they aren't hit-testing our backend. This keeps the server's CPU and memory free to handle other requests, like creating new workspaces or editing models.

#### Q2.2: If a client needs to verify whether a specific workspace exists but wants to save bandwidth by not downloading the entire JSON body, which HTTP method should they use instead of GET? Explain your reasoning.

They should use the **`HEAD`** method.

This method works exactly like a `GET` request. The server goes through the same resource lookup and checks if the workspace exists, but it only returns the HTTP headers and status code (like `200 OK` or `404 Not Found`). It is forbidden from sending the JSON body.

So if a client calls `HEAD /api/v1/workspaces/{workspaceId}`, they can tell if it's there based on the response status. This saves a lot of bandwidth since they don't download the workspace metadata, and it also saves the server from converting Java objects to JSON.

---

### Part 3: Model Operations & Linking

#### Q3.1: When creating a new Model via a POST request, it is considered best practice for the server to generate the unique id (e.g., using `UUID.randomUUID()`) rather than allowing the client to pass an id in their JSON payload. Discuss the security and data integrity reasons behind this architectural choice.

I decided to generate model IDs on the server side (using UUIDs) instead of letting the client pass their own ID in the JSON body. Here is why:
- **Data Integrity**: If clients could choose IDs, two clients might try to use the same ID due to bad sync or bugs. This would lead to collisions, overwriting other models, or database crashes. Having the server generate them guarantees they are unique.
- **Security (ID Guessing / IDOR)**: If clients choose IDs, they usually go with simple sequential formats like `MOD-001`. Attackers can guess these easily and try to access or delete models belonging to other teams (which is called an IDOR attack). Random UUIDs are impossible to guess.
- **Control**: The server should be the single source of truth for resource lifecycles. Forcing the client to accept server-generated IDs ensures clean lifecycle management.

#### Q3.2: If a user attempts to search for a framework containing spaces or special characters (e.g., `?framework=Scikit Learn & Tools`), how must the client modify the URL, and why is this encoding necessary?

The client has to percent-encode (URL-encode) the search query parameter. The URL would end up looking like this: `?framework=Scikit%20Learn%20%26%20Tools` (where spaces become `%20` and the ampersand becomes `%26`).

This is necessary because:
- **Reserved characters get in the way**: The `&` character is used in URLs to separate different parameters (like `?a=1&b=2`). If we send `Scikit Learn & Tools` raw, the server will think ` Tools` is a separate parameter name. This cuts off the framework search query to just `Scikit Learn `.
- **Spaces are illegal**: Raw spaces are not allowed in HTTP URLs and can cause web servers or proxy servers to reject the request with a 400 Bad Request error.

Encoding converts these characters into safe hexadecimal formats so the backend can parse the search query correctly.

---

### Part 4: Deep Nesting with Sub-Resources

#### Q4.1: You can place annotations like `@Produces(MediaType.APPLICATION_JSON)` at either the class level or the individual method level. What is the benefit of class-level placement, and how does method-level overriding work?

Setting `@Produces` at the class level acts as a default for all the methods inside that class. It is really useful because we don't have to keep repeating the annotation, which makes the code cleaner.

If a specific method needs to return something else (like plain text or HTML), we can just put a method-level `@Produces` on it. JAX-RS always prioritizes the method-level annotation over the class-level one. For example, if a method is annotated with `@Produces(MediaType.TEXT_PLAIN)`, JAX-RS will override the default class-level JSON formatting and return plain text instead.

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging

#### Q5.1: HTTP status codes are categorised into classes (e.g., 2xx, 4xx, 5xx). Explain fundamentally why a validation failure caused by the user providing a non-existent workspaceId must return a 4xx code rather than a 5xx code.

The main reason is that the class of status code tells us who made the mistake. 4xx codes are for client-side errors (bad data, typos, missing parameters) while 5xx codes mean the server crashed or has a bug.

If a user tries to POST a model with a `workspaceId` that doesn't exist, they are passing invalid data. That's a client error.
If we returned a 5xx error, we would be telling the client that the server broke. This is a problem because:
- Automated monitoring tools might trigger alerts for the server team when nothing is actually broken on the server.
- The client might keep retrying the exact same request without changes, thinking it will eventually succeed when the server recovers.

Returning a 4xx code (like 422 Unprocessable Entity) tells the client they made a mistake and need to fix their payload before trying again.

#### Q5.2: If an operation throws a specific custom exception (e.g., `LinkedWorkspaceNotFoundException`) and you also have a global `ExceptionMapper<Throwable>`, how does the JAX-RS runtime determine which mapper to execute?

During implementation, I had some issues with custom exceptions getting caught by the global mapper, but then I realized JAX-RS uses a proximity matching algorithm to select the right `ExceptionMapper`.

When an exception is thrown, the JAX-RS runtime looks at all registered mappers and calculates the inheritance 'distance' between the thrown exception and the exception class declared in each mapper's signature. It then picks the closest one (the shortest distance).

For example, if `LinkedWorkspaceNotFoundException` is thrown:
- `LinkedWorkspaceNotFoundExceptionMapper` is an exact match, so the inheritance distance is 0.
- `GlobalExceptionMapper<Throwable>` matches since it handles `Throwable` (which is a parent of all exceptions), but the distance is much further up the inheritance tree.

Because of this proximity matching, JAX-RS will choose the specific exception mapper (distance 0) over the global fallback mapper. The global mapper only acts as a safety net for unexpected crashes.

#### Q5.3: In your filter, you interact with `ContainerRequestContext` and `ContainerResponseContext`. List two pieces of crucial HTTP metadata (e.g., headers, URIs) you can extract from these contexts that are highly valuable for debugging server issues.

During testing, I noticed that logging specific metadata helped me find bugs much faster. In my custom filter, I extracted these three pieces of info:
- **Request URI**: Using `requestContext.getUriInfo().getRequestUri()`. This prints the exact path and query parameters the client requested, so I could check if they made a typo in the URL.
- **Request Headers**: Retrieved via `requestContext.getHeaders()`. This is useful to verify if they sent the right `Content-Type` or authorization headers.
- **HTTP Status Code**: Retrieved using `responseContext.getStatus()`. This tells us if the request succeeded or what error code was sent back to the client.
