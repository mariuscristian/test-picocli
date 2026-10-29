package com.example.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** JSON in/out for the commands generated from the OpenAPI spec. */
@Component
public class ApiCommandSupport {
  private final JsonMapper json;

  public ApiCommandSupport(JsonMapper json) { this.json = json; }

  /** Parses a JSON option value; "-" reads the JSON from stdin. */
  public <T> T readJson(String value, Class<T> type) {
    return json.readValue("-".equals(value) ? readStdin() : value, type);
  }

  /** Prints a response as pretty JSON on stdout. */
  public void printJson(Object value) {
    System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(value));
  }

  private static String readStdin() {
    try {
      return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
