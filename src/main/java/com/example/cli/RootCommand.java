package com.example.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "cli-demo", mixinStandardHelpOptions = true, version = "cli-demo 1.0.0",
         description = "Demo CLI built with Spring Boot and Picocli",
         subcommands = {GreetCommand.class})
public class RootCommand {}
