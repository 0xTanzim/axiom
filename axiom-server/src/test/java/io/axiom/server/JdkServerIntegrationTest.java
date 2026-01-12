package io.axiom.server;

import io.axiom.core.app.App;
import io.axiom.core.app.Axiom;
import io.axiom.core.routing.Router;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JdkServer with virtual threads.
 */
class JdkServerIntegrationTest {

    private App app;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        this.app = Axiom.create();
        this.client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (this.app.isRunning()) {
            this.app.stop();
        }
    }

    @Test
    void serverStartsAndRespondsToRequests() throws IOException, InterruptedException {
        final Router router = new Router();
        router.get("/hello", ctx -> ctx.text("Hello, World!"));
        this.app.route(router);
        this.app.listen(0);

        final int port = this.app.port();
        assertThat(port).isGreaterThan(0);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/hello"))
                .GET()
                .build();

        final HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Hello, World!");
    }

    @Test
    void serverHandlesJsonResponse() throws IOException, InterruptedException {
        record Message(String text) {}

        final Router router = new Router();
        router.get("/api/message", ctx -> ctx.json(new Message("test")));
        this.app.route(router);
        this.app.listen(0);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + this.app.port() + "/api/message"))
                .GET()
                .build();

        final HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(ct ->
                assertThat(ct).contains("application/json"));
        assertThat(response.body()).contains("\"text\":\"test\"");
    }

    @Test
    void serverHandlesPathParameters() throws IOException, InterruptedException {
        final Router router = new Router();
        router.get("/users/:id", ctx -> ctx.text("User: " + ctx.param("id")));
        this.app.route(router);
        this.app.listen(0);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + this.app.port() + "/users/42"))
                .GET()
                .build();

        final HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("User: 42");
    }

    @Test
    void serverHandlesQueryParameters() throws IOException, InterruptedException {
        final Router router = new Router();
        router.get("/search", ctx -> ctx.text("Query: " + ctx.query("q")));
        this.app.route(router);
        this.app.listen(0);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + this.app.port() + "/search?q=java"))
                .GET()
                .build();

        final HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Query: java");
    }

    @Test
    void serverHandlesPostWithBody() throws IOException, InterruptedException {
        record Input(String name) {}

        final Router router = new Router();
        router.post("/api/greet", ctx -> {
            final Input input = ctx.body(Input.class);
            ctx.text("Hello, " + input.name() + "!");
        });
        this.app.route(router);
        this.app.listen(0);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + this.app.port() + "/api/greet"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"World\"}"))
                .build();

        final HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Hello, World!");
    }

    @Test
    void serverHandlesMiddleware() throws IOException, InterruptedException {
        final Router router = new Router();
        router.get("/test", ctx -> ctx.text("OK"));

        this.app.use(ctx -> {
            ctx.header("X-Custom", "middleware");
            ctx.next();
        });
        this.app.route(router);
        this.app.listen(0);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + this.app.port() + "/test"))
                .GET()
                .build();

        final HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");
        assertThat(response.headers().firstValue("X-Custom")).hasValue("middleware");
    }

    @Test
    void serverReportsCorrectRuntimeName() {
        final String name = Axiom.serverName();
        assertThat(name).isEqualTo("jdk-httpserver");
    }

    @Test
    void serverIsRunningAfterListen() {
        final Router router = new Router();
        router.get("/", ctx -> ctx.text("root"));
        this.app.route(router);
        this.app.listen(0);

        assertThat(this.app.isRunning()).isTrue();
        assertThat(this.app.port()).isGreaterThan(0);
    }

    @Test
    void serverStopsCorrectly() {
        final Router router = new Router();
        router.get("/", ctx -> ctx.text("root"));
        this.app.route(router);
        this.app.listen(0);
        this.app.stop();

        assertThat(this.app.isRunning()).isFalse();
    }
}
