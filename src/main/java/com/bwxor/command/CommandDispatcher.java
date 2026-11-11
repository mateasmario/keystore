package com.bwxor.command;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public abstract class CommandDispatcher implements Command {
    protected final Map<String, Command> subcommands = new HashMap<>();

    public CommandDispatcher register(String name, Command command) {
        subcommands.put(name, command);
        return this;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.isEmpty()) {
            processDefaultMessage(displayFunction);
            return;
        }

        Command command = subcommands.get(args.getFirst().toLowerCase(Locale.ROOT));
        if (command == null) {
            displayFunction.accept("Unknown command: " + args.getFirst().toLowerCase(Locale.ROOT));
            return;
        }

        command.execute(args.subList(1, args.size()), displayFunction);   // pass the rest down
    }

    public abstract void processDefaultMessage(Consumer<String> displayFunction);
}
