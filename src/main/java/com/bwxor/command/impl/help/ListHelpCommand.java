package com.bwxor.command.impl.help;

import com.bwxor.command.Command;

import java.util.List;
import java.util.function.Consumer;

public class ListHelpCommand implements Command {
    private static final String HELP_LIST_MESSAGE = """
        Usage: keystore list [options]

        Lists all keys stored in the keystore, together with their values.

        Options:
          -s, --silent   List only the keys, without their values

        Examples:
          keystore list
          keystore list --silent""";

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        displayFunction.accept(HELP_LIST_MESSAGE);
    }
}
