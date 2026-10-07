package com.bwxor.command.impl;

import com.bwxor.command.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CommandDispatcher implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: 'keystore <command>'
            Type 'keystore help' for more information
            """;

    private final Map<String, Command> subcommands = new HashMap<>();

    public CommandDispatcher register(String name, Command command) {
        subcommands.put(name, command);
        return this;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.isEmpty()) {
            System.out.println(USAGE_MESSAGE);
            return;
        }

        Command command = subcommands.get(args.get(0));
        if (command == null) {
            System.out.println("Unknown command: " + args.get(0));
            return;
        }

        command.execute(args.subList(1, args.size()), displayFunction);   // pass the rest down
    }
}