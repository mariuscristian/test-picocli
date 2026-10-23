package com.example.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
public class CliApplication {

  @Bean
  PicocliRunner picocliRunner(IFactory factory, RootCommand root) {
    return new PicocliRunner(factory, root);
  }

  static class PicocliRunner implements CommandLineRunner, ExitCodeGenerator {
    private final IFactory factory;
    private final RootCommand root;
    private int exitCode;

    PicocliRunner(IFactory factory, RootCommand root) {
      this.factory = factory;
      this.root = root;
    }

    @Override
    public void run(String... args) {
      exitCode = new CommandLine(root, factory).execute(args);
    }

    @Override
    public int getExitCode() { return exitCode; }
  }

  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(CliApplication.class, args)));
  }
}
