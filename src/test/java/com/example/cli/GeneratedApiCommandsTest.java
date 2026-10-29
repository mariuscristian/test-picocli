package com.example.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Runs the commands generated from greeting-api.yaml against a stub HTTP server. */
@ExtendWith(OutputCaptureExtension.class)
class GeneratedApiCommandsTest {
  private static HttpServer server;
  private static final List<String> requests = new ArrayList<>();

  @BeforeAll
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/greetings", GeneratedApiCommandsTest::handle);
    server.start();
    System.setProperty("spring.http.serviceclient.greeting-api.base-url",
        "http://127.0.0.1:" + server.getAddress().getPort());
    System.setProperty("logging.file.name", "target/test-cli-demo.log");
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
    System.clearProperty("spring.http.serviceclient.greeting-api.base-url");
    System.clearProperty("logging.file.name");
  }

  @BeforeEach
  void reset() { requests.clear(); }

  private static void handle(HttpExchange exchange) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + body);
    String uri = exchange.getRequestURI().toString();
    int status = 200;
    String response;
    if (exchange.getRequestMethod().equals("POST")) {
      response = "{\"id\":\"1\",\"messages\":[\"Hello, MM!\",\"Hello, MM!\"]}";
    } else if (uri.equals("/greetings/42?lang=ro")) {
      response = "{\"id\":\"42\",\"messages\":[\"Salut, MM!\"]}";
    } else {
      status = 404;
      response = "{\"error\":\"not found\"}";
    }
    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private static int run(String... args) {
    return SpringApplication.exit(SpringApplication.run(CliApplication.class, args));
  }

  @Test
  void postsJsonBodyAndPrintsJsonResponse(CapturedOutput output) {
    assertThat(run("greetings", "create-greeting", "--body", "{\"name\":\"MM\",\"times\":2}")).isZero();
    assertThat(requests).containsExactly("POST /greetings {\"name\":\"MM\",\"times\":2}");
    assertThat(output.getOut()).contains("\"messages\" : [ \"Hello, MM!\", \"Hello, MM!\" ]");
    assertThat(output.getErr()).contains("Success");
  }

  @Test
  void mapsPathAndQueryOptions(CapturedOutput output) {
    assertThat(run("greetings", "get-greeting", "--id", "42", "--lang", "ro")).isZero();
    assertThat(requests).containsExactly("GET /greetings/42?lang=ro ");
    assertThat(output.getOut()).contains("Salut, MM!");
  }

  @Test
  void httpErrorFailsWithExitCode1(CapturedOutput output) {
    assertThat(run("greetings", "get-greeting", "--id", "7")).isEqualTo(1);
    assertThat(output.getOut()).isEmpty();
    assertThat(output.getErr()).contains("404").contains("Failed (exit code 1)");
  }

  @Test
  void missingRequiredOptionFailsWithExitCode2(CapturedOutput output) {
    assertThat(run("greetings", "create-greeting")).isEqualTo(2);
    assertThat(requests).isEmpty();
    assertThat(output.getErr()).contains("Missing required option: '--body=JSON'");
  }
}
