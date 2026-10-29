package com.example.cli;

import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import com.example.cli.generated.ApiCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.NestedExceptionUtils;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.ParseResult;

@SpringBootApplication
public class CliApplication {

  // Commands come from Spring (constructor injection); anything else falls back to Picocli.
  @Bean
  IFactory picocliFactory(ApplicationContext ctx) {
    return new IFactory() {
      @Override
      public <K> K create(Class<K> cls) throws Exception {
        try {
          return ctx.getBean(cls);
        } catch (BeansException e) {
          return CommandLine.defaultFactory().create(cls);
        }
      }
    };
  }

  @Bean
  PicocliRunner picocliRunner(IFactory factory, RootCommand root,
                              @Value("${logging.file.name}") String logFile) {
    return new PicocliRunner(factory, root, logFile);
  }

  static class PicocliRunner implements CommandLineRunner, ExitCodeGenerator {
    private final IFactory factory;
    private static final Logger log = LoggerFactory.getLogger(PicocliRunner.class);
    private final RootCommand root;
    private final String logFile;
    private int exitCode;

    PicocliRunner(IFactory factory, RootCommand root, String logFile) {
      this.factory = factory;
      this.root = root;
      this.logFile = logFile;
    }

    @Override
    public void run(String... args) {
      CommandLine cmd = new CommandLine(root, factory);
      // Command groups generated from the OpenAPI spec (see src/main/openapi).
      ApiCommands.ALL.forEach(cmd::addSubcommand);
      // Stack traces go to the log file; the user gets a one-line message.
      cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
        log.error("Command failed", ex);
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
        String message = cause == ex ? ex.getMessage() : ex.getMessage() + " (" + cause + ")";
        commandLine.getErr().println(Ansi.AUTO.string("@|red Error: " + message + "|@"));
        return commandLine.getCommandSpec().exitCodeOnExecutionException();
      });
      exitCode = cmd.execute(args);
      if (!helpRequested(cmd.getParseResult())) {
        // Status goes to stderr so stdout stays clean for piping.
        cmd.getErr().println(exitCode == 0
            ? Ansi.AUTO.string("@|green Success|@")
            : Ansi.AUTO.string("@|red Failed|@ (exit code " + exitCode + "). Log: " + logFile));
      }
    }

    private static boolean helpRequested(ParseResult pr) {
      for (; pr != null; pr = pr.subcommand()) {
        if (pr.isUsageHelpRequested() || pr.isVersionHelpRequested()) return true;
      }
      return false;
    }

    @Override
    public int getExitCode() { return exitCode; }
  }

  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(CliApplication.class, args)));
  }
}
