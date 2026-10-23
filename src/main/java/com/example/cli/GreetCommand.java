package com.example.cli;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "greet", mixinStandardHelpOptions = true, description = "Print a greeting and some logs")
public class GreetCommand implements Callable<Integer> {
  private static final Logger log = LoggerFactory.getLogger(GreetCommand.class);
  private final GreetingService service;

  @Parameters(index = "0", description = "Who to greet")
  String name;

  @Option(names = {"-n", "--times"}, defaultValue = "1", description = "Repeat count")
  int times;

  public GreetCommand(GreetingService service) { this.service = service; }

  @Override
  public Integer call() {
    log.info("Greeting {} {} time(s)", name, times);
    for (int i = 0; i < times; i++) System.out.println(service.greet(name));
    log.info("Done");
    return 0;
  }
}
