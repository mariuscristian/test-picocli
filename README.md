# test-picocli

Spring Boot 4 + Picocli CLI demo (Java 25).

## Commands generated from OpenAPI

`src/main/openapi/greeting-api.yaml` is the source of truth. On every build,
`openapi-generator-maven-plugin` (stock `spring` generator,
`spring-http-interface` library) writes into `target/generated-sources/openapi`:

| Generated | From | Purpose |
|---|---|---|
| `model/*` | stock templates | JSON request/response models (Jackson 3) |
| `api/<Tag>Api` | stock templates | `@HttpExchange` interface, used as the HTTP client |
| `api/<Tag>ApiCommand` | `templates/picocliCommand.mustache` | picocli command group per tag, one subcommand per operation |
| `ApiCommands` | `templates/picocliApiCommands.mustache` | registers the HTTP clients (`@ImportHttpServices`) and lists the command groups |

Mapping: tag → command group, `operationId` → kebab-case subcommand, path/query/header
parameters → `--options`, request body → `--body <json>` (`-` reads stdin), response →
pretty JSON on stdout. Exit codes: 0 success, 1 HTTP or runtime error, 2 bad arguments.

To add an endpoint, edit the spec and rebuild; no Java changes are needed. Settings live in
`src/main/openapi/generator-config.yaml`; hand-written glue is `ApiCommandSupport`.

```sh
mvn package
export GREETING_API_URL=http://localhost:8080   # default
java -jar target/cli-demo-1.0.0.jar greetings create-greeting --body '{"name":"MM","times":2}'
java -jar target/cli-demo-1.0.0.jar greetings get-greeting --id 42 --lang ro
```

Logs go to `~/.cli-demo/cli-demo.log` (override with `LOGGING_FILE_NAME`).
